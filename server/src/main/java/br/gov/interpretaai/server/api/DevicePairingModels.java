package br.gov.interpretaai.server.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class DevicePairingModels {
    private DevicePairingModels() {}

    public record CreatePairingCodeRequest(
            @NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9_-]{2,63}") String classroomId) {}

    public record PairingCode(String pairingId, String code, Instant expiresAt) {}

    public record RedeemPairingRequest(
            @NotBlank @Size(min = 8, max = 9) String code,
            @NotBlank @Size(min = 16, max = 128)
                    @Pattern(regexp = "[A-Za-z0-9._:-]+") String installationId,
            @Min(1) int appVersion,
            @Pattern(regexp = "ARM64|UNIVERSAL|UNKNOWN") String architecture,
            @Min(240) @Max(2000) int viewportWidthDp,
            @Min(320) @Max(3000) int viewportHeightDp) {}

    public record DeviceCredential(
            String deviceId,
            String deviceToken,
            String schoolId,
            String classroomId,
            Instant issuedAt) {}

    public record DeviceState(String deviceId, String status) {}

    public record DeviceContext(
            String deviceId,
            String schoolId,
            String classroomId,
            String status) {}
}
