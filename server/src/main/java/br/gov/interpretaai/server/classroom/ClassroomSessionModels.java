package br.gov.interpretaai.server.classroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class ClassroomSessionModels {
    private ClassroomSessionModels() {}

    public record ImportRosterRequest(
            @NotEmpty @Size(max = 40) List<@NotBlank @Size(max = 80) String> names) {}

    public record LearnerSeat(
            String learnerId, String displayName, String learnerAlias, int seatNumber) {}

    public record Roster(String classroomId, List<LearnerSeat> learners) {}

    public record OpenSessionResponse(
            String sessionId, String classroomId, String joinCode, Instant expiresAt,
            int learnerCount) {}

    public record JoinSessionRequest(
            @NotBlank @Size(min = 8, max = 9) String code,
            @NotBlank @Pattern(regexp = "[a-z0-9][a-z0-9_-]{2,63}") String learnerId) {}

    public record SessionChoice(
            String learnerId, String displayName, int seatNumber, boolean available) {}

    public record SessionLobby(
            String sessionId, String classroomId, Instant expiresAt, List<SessionChoice> learners) {}

    public record DeviceSeat(
            String sessionId, String classroomId, String learnerAlias, int seatNumber,
            String status) {}

    public record TeacherSeatStatus(
            String learnerId, String displayName, String learnerAlias, int seatNumber,
            String deviceId, boolean connected) {}

    public record TeacherSessionStatus(
            String sessionId, String classroomId, String status, Instant expiresAt,
            int connectedDevices, int learnerCount, List<TeacherSeatStatus> seats) {}
}
