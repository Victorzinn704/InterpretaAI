package br.gov.interpretaai.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class AuthoringJobModels {
    private AuthoringJobModels() {}

    private static final String ID = "[a-z0-9][a-z0-9_-]{2,63}";

    public record CreateAuthoringJobRequest(
            @NotEmpty @Size(max = 8)
                    List<@Pattern(regexp = ID) String> objectiveIds,
            @NotNull YearRange yearRange,
            @Min(3) @Max(40) int durationMinutes,
            @NotNull ParticipationMode participationMode,
            @NotNull @Valid Source source,
            @Size(min = 2, max = 40) String confirmedWord,
            @Size(max = 1000) String teacherNotes,
            @NotEmpty @Size(max = 5) List<@NotNull Component> requestedComponents,
            boolean reducedStimuliDefault,
            @Pattern(regexp = ID) String externalProcessingConsentId) {}

    public record Source(
            @NotNull SourceType type,
            @Pattern(regexp = ID) String mediaId,
            @Pattern(regexp = ID) String assetId,
            @Size(min = 2, max = 160) String theme) {}

    public record Progress(
            String step,
            int completedSteps,
            int totalSteps) {}

    public record Failure(
            String code,
            boolean retryable,
            String safeMessage) {}

    public record AuthoringJob(
            String jobId,
            String status,
            Progress progress,
            Failure failure,
            Instant createdAt,
            Instant updatedAt) {}

    public enum YearRange {
        @JsonProperty("1_YEAR") YEAR_1,
        @JsonProperty("2_YEAR") YEAR_2,
        @JsonProperty("3_YEAR") YEAR_3,
        @JsonProperty("4_YEAR") YEAR_4,
        @JsonProperty("5_YEAR") YEAR_5,
        MIXED
    }
    public enum ParticipationMode { INDIVIDUAL, PAIR, GROUP }
    public enum SourceType { TEACHER_UPLOAD, APPROVED_LIBRARY, THEME }
    public enum Component { COMIC, PUZZLE, WORD_BUILDER, GROUP_HANDOFF }
}
