package br.gov.interpretaai.server.delivery;

import br.gov.interpretaai.server.api.DeliveryModels.Assignment;
import br.gov.interpretaai.server.api.DeliveryModels.AssignmentTarget;
import br.gov.interpretaai.server.api.DeliveryModels.AssignmentTargetType;
import br.gov.interpretaai.server.api.DeliveryModels.CreateAssignmentRequest;
import br.gov.interpretaai.server.api.DeliveryModels.PreparedReceipt;
import br.gov.interpretaai.server.api.DeliveryModels.PreparationSummary;
import br.gov.interpretaai.server.device.DevicePairingService.DevicePrincipal;
import br.gov.interpretaai.server.identity.InstitutionAction;
import br.gov.interpretaai.server.identity.InstitutionAuditStore;
import br.gov.interpretaai.server.identity.InstitutionalAccessService;
import br.gov.interpretaai.server.identity.InstitutionalAccessStore;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryService {
    public record ManifestPage(List<DeliveryStore.ManifestRecord> items, String nextCursor, Instant serverTime) {}

    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("[A-Za-z0-9._:-]{16,128}");
    private static final Pattern CURSOR = Pattern.compile("d1\\.([0-9]{1,18})");
    private static final int PAGE_SIZE = 50;
    private static final int PREPARATION_FRESHNESS_HOURS = 24;

    private final DeliveryStore assignments;
    private final InstitutionalAccessService access;
    private final InstitutionAuditStore audit;
    private final PreparationReceiptStore receipts;
    private final Clock clock;

    public DeliveryService(
            DeliveryStore assignments,
            InstitutionalAccessService access,
            InstitutionAuditStore audit,
            PreparationReceiptStore receipts,
            Clock clock) {
        this.assignments = assignments;
        this.access = access;
        this.audit = audit;
        this.receipts = receipts;
        this.clock = clock;
    }

    @Transactional
    public Assignment create(
            String oidcSubject,
            String schoolId,
            String idempotencyKey,
            CreateAssignmentRequest request) {
        validateKey(idempotencyKey);
        if (request.target().type() != AssignmentTargetType.CLASSROOM) {
            throw new DeliveryException(
                    422,
                    "assignment_target_unavailable",
                    "Nesta versão, publique a história para uma turma inteira.");
        }
        if (request.expiresAt() != null && !request.expiresAt().isAfter(request.availableFrom())) {
            throw new DeliveryException(
                    400,
                    "assignment_window_invalid",
                    "O término da atividade precisa ocorrer depois da disponibilidade.");
        }
        var grant = access.requireClassroomAction(
                oidcSubject, request.target().id(), InstitutionAction.PUBLISH_TO_CLASSROOM);
        if (!grant.schoolId().equals(schoolId)) throw new InstitutionalAccessService.AccessDeniedException();
        if (!assignments.publishedVersionExists(request.storyId(), request.storyVersion(), schoolId)) {
            throw new DeliveryException(
                    409,
                    "story_not_published",
                    "A história precisa estar aprovada e publicada antes do envio à turma.");
        }
        int priority = request.priority() == null ? 50 : request.priority();
        String fingerprint = fingerprint(request, priority);
        var existing = assignments.findByIdempotency(grant.userId(), schoolId, idempotencyKey);
        if (existing.isPresent()) return matching(existing.orElseThrow(), fingerprint);

        Instant now = clock.instant();
        var assignment = new DeliveryStore.AssignmentRecord(
                "assignment_" + UUID.randomUUID().toString().replace("-", ""), schoolId,
                request.storyId(), request.storyVersion(), request.target().id(), grant.userId(),
                fingerprint, priority, request.availableFrom(), request.expiresAt(), now);
        try {
            assignments.insert(assignment, idempotencyKey, now);
        } catch (DataIntegrityViolationException race) {
            var repeated = assignments.findByIdempotency(grant.userId(), schoolId, idempotencyKey)
                    .orElseThrow(() -> race);
            return matching(repeated, fingerprint);
        }
        audit.append(grant.userId(), schoolId, "STORY_ASSIGNED_TO_CLASSROOM", "ASSIGNMENT",
                assignment.assignmentId(), now);
        return view(assignment);
    }

    public ManifestPage manifest(DevicePrincipal device, String after) {
        long cursor = decodeCursor(after);
        Instant now = clock.instant();
        List<DeliveryStore.ManifestRecord> retrieved = assignments.findManifest(
                device.schoolId(), device.classroomId(), device.appVersion(), cursor, now, PAGE_SIZE + 1);
        retrieved.forEach(DeliveryService::requirePackIntegrity);
        List<DeliveryStore.ManifestRecord> page = retrieved.size() > PAGE_SIZE
                ? retrieved.subList(0, PAGE_SIZE) : retrieved;
        long next = page.isEmpty() ? cursor : page.get(page.size() - 1).deliverySequence();
        return new ManifestPage(List.copyOf(page), cursor(next), now);
    }

    /** Assignments visible to this adult, not evidence that a tablet has prepared the pack. */
    public List<Assignment> activeForStory(
            String oidcSubject, String schoolId, String storyId, int version) {
        access.requireSchoolAction(oidcSubject, schoolId, InstitutionAction.CREATE_DRAFT);
        Set<String> allowedClassrooms = access.activeClassrooms(oidcSubject, schoolId).stream()
                .map(InstitutionalAccessStore.ClassroomDisplay::classroomId)
                .collect(java.util.stream.Collectors.toSet());
        return assignments.activeForStory(schoolId, storyId, version, clock.instant()).stream()
                .filter(item -> allowedClassrooms.contains(item.classroomId()))
                .map(DeliveryService::view).toList();
    }

    /** A device report follows local hash/file verification; it is not a child-learning metric. */
    @Transactional
    public PreparedReceipt confirmPrepared(
            DevicePrincipal device, String assignmentId, String packSha256) {
        if (!assignments.lockAssignment(assignmentId, device.schoolId(), device.classroomId())) {
            throw new DeliveryException(404, "assignment_not_available",
                    "Esta atividade não está disponível neste aparelho.");
        }
        var pack = pack(device, assignmentId);
        if (!pack.packSha256().equals(packSha256)) {
            throw new DeliveryException(409, "preparation_hash_mismatch",
                    "O pacote confirmado não corresponde à versão publicada.");
        }
        Instant now = clock.instant();
        receipts.confirm(assignmentId, device.deviceId(), packSha256, now);
        return new PreparedReceipt(assignmentId, device.deviceId(), packSha256, now);
    }

    public PreparationSummary preparationSummary(
            String oidcSubject, String schoolId, String assignmentId) {
        access.requireSchoolAction(oidcSubject, schoolId, InstitutionAction.CREATE_DRAFT);
        var assignment = assignments.findById(assignmentId, schoolId)
                .orElseThrow(() -> new DeliveryException(404, "assignment_not_found",
                        "A atribuição não está disponível nesta escola."));
        var grant = access.requireClassroomAction(
                oidcSubject, assignment.classroomId(), InstitutionAction.PUBLISH_TO_CLASSROOM);
        if (!grant.schoolId().equals(schoolId)) {
            throw new InstitutionalAccessService.AccessDeniedException();
        }
        var pack = assignments.publishedPackMetadata(assignmentId, schoolId)
                .orElseThrow(() -> new DeliveryException(404, "assignment_not_found",
                        "A atribuição não está disponível nesta escola."));
        Instant now = clock.instant();
        var counts = receipts.counts(assignmentId, schoolId, assignment.classroomId(),
                pack.sha256(), pack.minAppVersion(),
                now.minus(Duration.ofHours(PREPARATION_FRESHNESS_HOURS)));
        return new PreparationSummary(assignmentId, counts.pairedCompatibleDevices(),
                counts.recentlyConfirmedDevices(), counts.lastConfirmationAt(), now,
                PREPARATION_FRESHNESS_HOURS);
    }

    public DeliveryStore.ManifestRecord pack(DevicePrincipal device, String assignmentId) {
        var pack = assignments.findEligiblePack(
                assignmentId, device.schoolId(), device.classroomId(), device.appVersion(), clock.instant())
                .orElseThrow(() -> new DeliveryException(
                        404, "assignment_not_available", "Esta atividade não está disponível neste aparelho."));
        requirePackIntegrity(pack);
        return pack;
    }

    /** Resolves a declared variant through the published version, never through a pack path. */
    public DeliveryStore.AssetRecord asset(
            DevicePrincipal device, String assignmentId, String assetId, String role) {
        return assignments.findEligibleAsset(
                assignmentId, assetId, role, device.schoolId(), device.classroomId(),
                device.appVersion(), clock.instant()).orElseThrow(() -> new DeliveryException(
                404, "asset_not_available", "Este recurso não está disponível neste aparelho."));
    }

    private static Assignment matching(DeliveryStore.AssignmentRecord existing, String fingerprint) {
        if (!existing.requestFingerprint().equals(fingerprint)) {
            throw new DeliveryException(
                    409, "idempotency_conflict", "A chave já pertence a outra solicitação.");
        }
        return view(existing);
    }

    private static Assignment view(DeliveryStore.AssignmentRecord item) {
        return new Assignment(
                item.assignmentId(), item.storyId(), item.storyVersion(),
                new AssignmentTarget(AssignmentTargetType.CLASSROOM, item.classroomId()),
                item.availableFrom(), item.expiresAt(), item.priority(), item.createdAt());
    }

    private static void validateKey(String key) {
        if (key == null || !IDEMPOTENCY_KEY.matcher(key).matches()) {
            throw new DeliveryException(
                    400, "invalid_idempotency_key", "Use uma chave idempotente de 16 a 128 caracteres.");
        }
    }

    private static String fingerprint(CreateAssignmentRequest request, int priority) {
        String canonical = request.storyId() + "|" + request.storyVersion() + "|"
                + request.target().type() + "|" + request.target().id() + "|"
                + request.availableFrom() + "|" + request.expiresAt() + "|" + priority;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void requirePackIntegrity(DeliveryStore.ManifestRecord pack) {
        if (!sha256(pack.packJson()).equals(pack.packSha256())) {
            throw new DeliveryException(
                    503,
                    "delivery_pack_integrity_invalid",
                    "A atividade está sendo verificada antes do envio ao aparelho.");
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static long decodeCursor(String value) {
        if (value == null || value.isBlank()) return 0;
        Matcher match = CURSOR.matcher(value);
        if (!match.matches()) {
            throw new DeliveryException(400, "manifest_cursor_invalid", "O cursor de atualização é inválido.");
        }
        try {
            return Long.parseLong(match.group(1));
        } catch (NumberFormatException invalid) {
            throw new DeliveryException(400, "manifest_cursor_invalid", "O cursor de atualização é inválido.");
        }
    }

    private static String cursor(long sequence) {
        return "d1." + sequence;
    }
}
