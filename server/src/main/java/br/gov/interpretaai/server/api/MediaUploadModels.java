package br.gov.interpretaai.server.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class MediaUploadModels {
    private MediaUploadModels() {}

    public record CreateUploadRequest(
            @NotBlank @Size(max = 160) String fileName,
            @Pattern(regexp = "image/(jpeg|png|webp)") String mediaType,
            @Min(1) @Max(10_485_760) long bytes,
            @Pattern(regexp = "STORY_SOURCE") String purpose) {}

    public record UploadSession(
            String mediaId,
            String uploadUrl,
            Instant expiresAt,
            long maxBytes) {}

    public record UploadReceipt(
            String mediaId,
            String status,
            long bytes,
            String sha256) {}
}
