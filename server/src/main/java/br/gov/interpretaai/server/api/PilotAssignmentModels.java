package br.gov.interpretaai.server.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class PilotAssignmentModels {
    private PilotAssignmentModels() {}

    public enum Activity { COMIC, PUZZLE, DRAWING, SOUND_M }
    public enum DrawingPrompt { BALL, APPLE, HOUSE, TREE }

    public record PublishAssignmentRequest(
            @NotBlank @Size(max = 30) String classroomLabel,
            @NotBlank @Pattern(regexp = "sol|pipa|estrela|foguete") String avatarId,
            @NotNull Activity activity,
            @NotNull DrawingPrompt drawingPrompt) {}

    public record AssignmentResponse(
            String deviceId,
            long version,
            String classroomLabel,
            String avatarId,
            Activity activity,
            DrawingPrompt drawingPrompt,
            Instant updatedAt) {}
}
