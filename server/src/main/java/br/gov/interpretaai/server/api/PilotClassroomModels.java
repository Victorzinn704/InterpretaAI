package br.gov.interpretaai.server.api;

import br.gov.interpretaai.server.api.PilotAssignmentModels.Activity;
import br.gov.interpretaai.server.api.PilotAssignmentModels.AssignmentResponse;
import br.gov.interpretaai.server.api.PilotAssignmentModels.DrawingPrompt;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class PilotClassroomModels {
    private PilotClassroomModels() {}

    public record ParticipantRequest(
            @NotBlank @Pattern(regexp = "(sol|pipa|estrela|foguete)-[0-9]{2,3}") String learnerAlias,
            @NotBlank @Pattern(regexp = "sol|pipa|estrela|foguete") String avatarId,
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{6,64}") String deviceId) {
        @JsonIgnore
        @AssertTrue(message = "learnerAlias deve corresponder ao avatar")
        public boolean isLearnerAliasCompatible() {
            return learnerAlias == null || avatarId == null || learnerAlias.startsWith(avatarId + "-");
        }
    }

    public record UpsertClassroomRequest(
            @NotBlank @Size(max = 30) String classroomLabel,
            @NotEmpty @Size(max = 40) List<@Valid ParticipantRequest> participants) {}

    public record ClassroomResponse(
            String classroomId,
            String classroomLabel,
            List<ParticipantRequest> participants,
            Instant updatedAt) {}

    public record PublishClassroomAssignmentRequest(
            @Size(max = 40) List<@Pattern(regexp = "(sol|pipa|estrela|foguete)-[0-9]{2,3}") String> learnerAliases,
            @NotNull Activity activity,
            @NotNull DrawingPrompt drawingPrompt) {
        public List<String> effectiveLearnerAliases() {
            return learnerAliases == null ? List.of() : learnerAliases;
        }
    }

    public record ClassroomAssignmentResponse(
            String classroomId,
            int targetCount,
            Instant publishedAt,
            List<AssignmentResponse> assignments) {}
}
