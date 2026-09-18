package br.gov.interpretaai.server.device;

import br.gov.interpretaai.server.api.DevicePairingModels.DeviceCredential;
import br.gov.interpretaai.server.api.DevicePairingModels.PairingCode;
import br.gov.interpretaai.server.api.DevicePairingModels.RedeemPairingRequest;
import br.gov.interpretaai.server.identity.InstitutionAction;
import br.gov.interpretaai.server.identity.InstitutionAuditStore;
import br.gov.interpretaai.server.identity.InstitutionalAccessService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DevicePairingService {
    public record DevicePrincipal(String deviceId, String schoolId, String classroomId, int appVersion) {}

    private static final Pattern NORMALIZED_CODE =
            Pattern.compile("[23456789A-HJ-NP-Z]{8}");
    private static final Pattern IDEMPOTENCY_KEY =
            Pattern.compile("[A-Za-z0-9._:-]{16,128}");

    private final boolean enabled;
    private final DeviceCredentialCodec credentials;
    private final DevicePairingStore store;
    private final InstitutionalAccessService access;
    private final InstitutionAuditStore audit;
    private final Clock clock;
    private final Duration codeTtl;

    public DevicePairingService(
            DevicePairingStore store,
            InstitutionalAccessService access,
            InstitutionAuditStore audit,
            Clock clock,
            @Value("${interpretaai.device-pairing.enabled:false}") boolean enabled,
            @Value("${interpretaai.device-pairing.secret:}") String secret,
            @Value("${interpretaai.device-pairing.code-ttl-minutes:10}") long ttlMinutes) {
        if (enabled && secret.length() < 32) {
            throw new IllegalStateException(
                    "DEVICE_PAIRING_SECRET must contain at least 32 characters when pairing is enabled");
        }
        this.enabled = enabled;
        this.credentials = new DeviceCredentialCodec(secret.isEmpty() ? "disabled" : secret);
        this.store = store;
        this.access = access;
        this.audit = audit;
        this.clock = clock;
        this.codeTtl = Duration.ofMinutes(ttlMinutes);
    }

    @Transactional
    public PairingCode createCode(
            String oidcSubject, String schoolId, String classroomId) {
        requireEnabled();
        var grant = access.requireClassroomAction(
                oidcSubject, classroomId, InstitutionAction.PAIR_DEVICE);
        if (!grant.schoolId().equals(schoolId)) {
            throw new InstitutionalAccessService.AccessDeniedException();
        }
        Instant now = clock.instant();
        String rawCode = credentials.pairingCode();
        String pairingId = "pair_" + compactUuid();
        Instant expiresAt = now.plus(codeTtl);
        store.insertPairing(
                pairingId,
                credentials.hash(rawCode),
                schoolId,
                classroomId,
                grant.userId(),
                expiresAt,
                now);
        audit.append(
                grant.userId(), schoolId, "DEVICE_PAIRING_CREATED", "PAIRING", pairingId, now);
        return new PairingCode(pairingId, format(rawCode), expiresAt);
    }

    @Transactional
    public DeviceCredential redeem(RedeemPairingRequest request) {
        requireEnabled();
        String code = normalize(request.code());
        if (!NORMALIZED_CODE.matcher(code).matches()) throw invalidCode();
        Instant now = clock.instant();
        var pairing = store.findActivePairing(credentials.hash(code), now)
                .orElseThrow(DevicePairingService::invalidCode);
        if (!store.consume(pairing.pairingId(), now)) throw invalidCode();

        String installationHash = credentials.hash(request.installationId());
        var previousInstallation = store.findDeviceByInstallationForUpdate(installationHash);
        if (previousInstallation.filter(device -> "ACTIVE".equals(device.status())).isPresent()) {
            throw installationAlreadyPaired();
        }
        previousInstallation.ifPresent(device -> store.retireInstallation(
                device.deviceId(), installationHash,
                credentials.hash("retired." + device.deviceId() + "." + compactUuid())));

        String deviceId = "device_" + compactUuid();
        String token = "dvc." + deviceId + "." + credentials.deviceSecret();
        try {
            store.insertDevice(
                    deviceId,
                    pairing,
                    installationHash,
                    "Tablet " + deviceId.substring(deviceId.length() - 4).toUpperCase(Locale.ROOT),
                    credentials.hash(token),
                    request.appVersion(),
                    request.architecture(),
                    request.viewportWidthDp(),
                    request.viewportHeightDp(),
                    now);
        } catch (DataIntegrityViolationException duplicateInstallation) {
            throw installationAlreadyPaired();
        }
        audit.append(
                pairing.createdByUserId(), pairing.schoolId(),
                "DEVICE_PAIRED", "DEVICE", deviceId, now);
        return new DeviceCredential(
                deviceId, token, pairing.schoolId(), pairing.classroomId(), now);
    }

    @Transactional
    public String revoke(
            String oidcSubject,
            String schoolId,
            String deviceId,
            String idempotencyKey) {
        requireEnabled();
        if (idempotencyKey == null || !IDEMPOTENCY_KEY.matcher(idempotencyKey).matches()) {
            throw new DevicePairingException(
                    400, "invalid_idempotency_key", "Use uma chave idempotente válida.");
        }
        var device = store.findDevice(deviceId)
                .orElseThrow(InstitutionalAccessService.AccessDeniedException::new);
        if (!device.schoolId().equals(schoolId)) {
            throw new InstitutionalAccessService.AccessDeniedException();
        }
        var grant = access.requireClassroomAction(
                oidcSubject, device.classroomId(), InstitutionAction.MANAGE_DEVICE);
        Instant now = clock.instant();
        if (store.revoke(deviceId, now)) {
            audit.append(
                    grant.userId(), schoolId, "DEVICE_REVOKED", "DEVICE", deviceId, now);
        }
        return "REVOKED";
    }

    public DevicePrincipal authenticate(String expectedDeviceId, String token) {
        requireEnabled();
        if (token == null || !token.startsWith("dvc." + expectedDeviceId + ".")) {
            throw invalidCredential();
        }
        var device = store.findActiveDevice(expectedDeviceId)
                .orElseThrow(DevicePairingService::invalidCredential);
        if (!credentials.matches(token, device.credentialHash())) throw invalidCredential();
        return new DevicePrincipal(
                device.deviceId(), device.schoolId(), device.classroomId(), device.appVersion());
    }

    private void requireEnabled() {
        if (!enabled) {
            throw new DevicePairingException(
                    503, "device_pairing_disabled", "O pareamento institucional não está disponível.");
        }
    }

    private static String normalize(String code) {
        if (code == null) return "";
        return code.replace("-", "").replace(" ", "").toUpperCase(Locale.ROOT);
    }

    private static String format(String code) {
        return code.substring(0, 4) + "-" + code.substring(4);
    }

    private static String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static DevicePairingException invalidCode() {
        return new DevicePairingException(
                400, "pairing_code_invalid", "O código é inválido, expirou ou já foi utilizado.");
    }

    private static DevicePairingException invalidCredential() {
        return new DevicePairingException(
                401, "device_credential_invalid", "A credencial do aparelho não é válida.");
    }

    private static DevicePairingException installationAlreadyPaired() {
        return new DevicePairingException(
                409,
                "installation_already_paired",
                "Este aplicativo já possui um pareamento. Revogue-o antes de repetir.");
    }
}
