package br.gov.interpretaai.server.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;

public final class StoryVersionModels {
    private StoryVersionModels() {}

    public record ApprovalRequest(
            @NotNull @Min(1) Long expectedRevision,
            @NotNull @Pattern(regexp = "[a-f0-9]{64}") String expectedPackSha256,
            @NotNull @Size(max = 400) List<@Valid AssetConfirmation> confirmedAssets,
            @NotNull @Size(max = 64) List<String> confirmedWarningIds) {}

    public record AssetConfirmation(
            @NotNull @Pattern(regexp = "[a-z0-9][a-z0-9_-]{2,63}") String assetId,
            @NotNull @Pattern(regexp = "PHONE|TABLET|THUMBNAIL|AUDIO") String role,
            @NotNull @Pattern(regexp = "[a-f0-9]{64}") String sha256) {}

    public record ReviewAsset(String assetId, String role, String sha256, String previewUrl) {}

    public record ReviewBundle(
            String storyId, int version, long revision, String state,
            String packSha256, String packJson, List<ReviewAsset> assets) {}

    public record ReviewListItem(
            String storyId, int version, String title, String state,
            long revision, String packSha256) {}

    public record StoryVersionState(
            String storyId,
            int version,
            String state,
            String packSha256,
            Instant updatedAt) {}
}
