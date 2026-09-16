package br.gov.interpretaai.server.api;

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

public final class PilotLearningModels {
    private PilotLearningModels() {}

    public enum EventType {
        SESSION_STARTED, PROMPT_HEARD, OBSERVATION_RECORDED,
        RESPONSE_SUBMITTED, STAGE_COMPLETED, HELP_REQUESTED, SESSION_COMPLETED
    }

    public enum Modality { VOICE, TOUCH, CAMERA, DRAWING, NONE }

    public enum ObservationCategory {
        ORAL_EXPRESSION, CONTEXT_REASONING, PARTICIPATION, NONE
    }

    public record LearningEventRequest(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{8,64}") String eventId,
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{3,64}") String activityId,
            @NotNull EventType type,
            @NotNull Modality modality,
            @Min(0) @Max(600_000) Long durationMs,
            @NotNull ObservationCategory observationCategory,
            @NotNull Instant occurredAt) {}

    public record LearningEventBatchRequest(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{6,64}") String deviceId,
            @NotEmpty @Size(max = 50) List<@Valid LearningEventRequest> events) {}

    public record LearningEventBatchResponse(int accepted, int duplicates) {}

    public record ClassroomSummary(
            String classroomId,
            String classroomLabel,
            int participants,
            long sessions,
            long participations,
            long completedStages,
            long helpRequests,
            long voiceResponses,
            long averageResponseMs,
            Instant updatedAt) {}

    public record SecretariatSummary(
            int classrooms,
            int participants,
            long sessions,
            long participations,
            long completedStages,
            long helpRequests,
            List<ClassroomSummary> classroomSummaries,
            Instant generatedAt) {}
}
