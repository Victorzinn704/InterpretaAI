package br.gov.interpretaai.server.api;

import br.gov.interpretaai.server.api.DevicePairingModels.CreatePairingCodeRequest;
import br.gov.interpretaai.server.api.DevicePairingModels.DeviceCredential;
import br.gov.interpretaai.server.api.DevicePairingModels.DeviceState;
import br.gov.interpretaai.server.api.DevicePairingModels.PairingCode;
import br.gov.interpretaai.server.api.DevicePairingModels.RedeemPairingRequest;
import br.gov.interpretaai.server.device.DevicePairingRateLimiter;
import br.gov.interpretaai.server.device.DevicePairingService;
import br.gov.interpretaai.server.identity.AdultIdentity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v2")
public class DevicePairingController {
    private static final String ID_PATTERN = "[a-z0-9][a-z0-9_-]{2,63}";

    private final AdultIdentity identity;
    private final DevicePairingService pairing;
    private final DevicePairingRateLimiter limiter;

    public DevicePairingController(
            AdultIdentity identity,
            DevicePairingService pairing,
            DevicePairingRateLimiter limiter) {
        this.identity = identity;
        this.pairing = pairing;
        this.limiter = limiter;
    }

    @PostMapping("/device-management/pairing-codes")
    @ResponseStatus(HttpStatus.CREATED)
    public PairingCode createCode(
            Authentication authentication,
            @RequestHeader("X-School-Id") @Pattern(regexp = ID_PATTERN) String schoolId,
            @Valid @RequestBody CreatePairingCodeRequest request) {
        return pairing.createCode(
                identity.subject(authentication), schoolId, request.classroomId());
    }

    @PostMapping("/device-pairings/redeem")
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceCredential redeem(
            HttpServletRequest servletRequest,
            @Valid @RequestBody RedeemPairingRequest request) {
        limiter.check(servletRequest.getRemoteAddr());
        return pairing.redeem(request);
    }

    @PostMapping("/device-management/devices/{deviceId}/revoke")
    public DeviceState revoke(
            Authentication authentication,
            @RequestHeader("X-School-Id") @Pattern(regexp = ID_PATTERN) String schoolId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable @Pattern(regexp = ID_PATTERN) String deviceId) {
        return new DeviceState(
                deviceId,
                pairing.revoke(
                        identity.subject(authentication), schoolId, deviceId, idempotencyKey));
    }
}
