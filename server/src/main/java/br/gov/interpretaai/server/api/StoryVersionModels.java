package br.gov.interpretaai.server.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class StoryVersionModels {
    private StoryVersionModels() {}

    public record ApprovalRequest(
            @NotNull @Min(1) Long expectedRevision,
            @NotNull @Size(max = 64) List<String> confirmedWarningIds) {}

    public record StoryVersionState(
            String storyId,
            int version,
            String state,
            String packSha256,
            Instant updatedAt) {}
}
