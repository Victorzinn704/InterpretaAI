package br.gov.interpretaai.server.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class DeliveryModels {
    private DeliveryModels() {}

    private static final String ID = "[a-z0-9][a-z0-9_-]{2,63}";

    public enum AssignmentTargetType { CLASSROOM, GROUP, DEVICE }

    public record AssignmentTarget(
            @NotNull AssignmentTargetType type,
            @NotBlank @Pattern(regexp = ID) String id) {}

    public record CreateAssignmentRequest(
            @NotBlank @Pattern(regexp = ID) String storyId,
            @Min(1) int storyVersion,
            @NotNull @Valid AssignmentTarget target,
            @NotNull Instant availableFrom,
            Instant expiresAt,
            @Min(0) @Max(100) Integer priority) {}

    public record Assignment(
            String assignmentId,
            String storyId,
            int storyVersion,
            AssignmentTarget target,
            Instant availableFrom,
            Instant expiresAt,
            int priority,
            Instant createdAt) {}

    public record ManifestItem(
            String assignmentId,
            String storyId,
            int storyVersion,
            int minAppVersion,
            String packSha256,
            long bytes,
            int priority,
            String downloadUrl,
            Instant expiresAt) {}

    public record DeviceManifest(List<ManifestItem> items, String nextCursor, Instant serverTime) {}
}
