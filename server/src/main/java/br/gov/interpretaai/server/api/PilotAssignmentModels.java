package br.gov.interpretaai.server.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class PilotAssignmentModels {
    private PilotAssignmentModels() {}

    public enum Activity {
        COMIC, PUZZLE, DRAWING, SOUND_M, NUMBER_PATH, CONNECT_DOTS, IMAGE_LETTERS,
        STORY_SEQUENCE_2, CAUSE_AND_EFFECT_3,
        FACT_OR_OPINION_4, COMPARE_SOURCES_5
    }
    public enum DrawingPrompt { BALL, APPLE, HOUSE, TREE }

    public record PublishAssignmentRequest(
            @NotBlank @Size(max = 30) String classroomLabel,
            @NotBlank @Pattern(regexp = "sol|pipa|estrela|foguete") String avatarId,
            @Pattern(regexp = "(sol|pipa|estrela|foguete)-[0-9]{2,3}") String learnerAlias,
            @NotNull Activity activity,
            @NotNull DrawingPrompt drawingPrompt) {
        public String effectiveLearnerAlias() {
            return learnerAlias == null ? avatarId + "-01" : learnerAlias;
        }

        @JsonIgnore
        @AssertTrue(message = "learnerAlias deve corresponder ao avatar")
        public boolean isLearnerAliasCompatible() {
            return learnerAlias == null || learnerAlias.startsWith(avatarId + "-");
        }
    }

    public record AssignmentMember(String learnerAlias, String avatarId) {}

    public record AssignmentResponse(
            String deviceId,
            long version,
            String classroomLabel,
            String avatarId,
            String learnerAlias,
            Activity activity,
            DrawingPrompt drawingPrompt,
            List<AssignmentMember> members,
            Instant updatedAt) {}
}
