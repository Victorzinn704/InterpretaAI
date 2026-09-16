package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.PilotClassroomModels.ClassroomResponse;
import br.gov.interpretaai.server.api.PilotClassroomModels.ParticipantRequest;
import br.gov.interpretaai.server.api.PilotClassroomModels.UpsertClassroomRequest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PilotClassroomStore {
    private final JdbcTemplate jdbc;

    public PilotClassroomStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ClassroomResponse upsert(String classroomId, UpsertClassroomRequest request, Instant now) {
        int changed = jdbc.update("""
                update pilot_classroom set classroom_label = ?, updated_at = ? where classroom_id = ?
                """, request.classroomLabel(), Timestamp.from(now), classroomId);
        if (changed == 0) {
            jdbc.update("""
                    insert into pilot_classroom (classroom_id, classroom_label, updated_at) values (?, ?, ?)
                    """, classroomId, request.classroomLabel(), Timestamp.from(now));
        }
        jdbc.update("delete from pilot_classroom_participant where classroom_id = ?", classroomId);
        request.participants().forEach(participant -> jdbc.update("""
                insert into pilot_classroom_participant
                (classroom_id, learner_alias, avatar_id, device_id) values (?, ?, ?, ?)
                """, classroomId, participant.learnerAlias(), participant.avatarId(), participant.deviceId()));
        return new ClassroomResponse(classroomId, request.classroomLabel(), List.copyOf(request.participants()), now);
    }

    public Optional<ClassroomResponse> find(String classroomId) {
        Optional<ClassroomHeader> header = jdbc.query("""
                select classroom_id, classroom_label, updated_at
                  from pilot_classroom where classroom_id = ?
                """, (result, row) -> new ClassroomHeader(
                result.getString("classroom_id"),
                result.getString("classroom_label"),
                result.getTimestamp("updated_at").toInstant()), classroomId).stream().findFirst();
        return header.map(item -> new ClassroomResponse(
                item.classroomId(), item.classroomLabel(), participants(classroomId), item.updatedAt()));
    }

    public Optional<ParticipantLocation> findParticipantByDevice(String deviceId) {
        return jdbc.query("""
                select classroom_id,
                       case when count(*) = 1 then min(learner_alias) else null end learner_alias,
                       count(*) participant_count
                  from pilot_classroom_participant where device_id = ?
                 group by classroom_id
                """, (result, row) -> new ParticipantLocation(
                result.getString("classroom_id"), result.getString("learner_alias"),
                result.getInt("participant_count")), deviceId)
                .stream().findFirst();
    }

    private List<ParticipantRequest> participants(String classroomId) {
        return jdbc.query("""
                select learner_alias, avatar_id, device_id
                  from pilot_classroom_participant
                 where classroom_id = ? order by learner_alias
                """, (result, row) -> new ParticipantRequest(
                result.getString("learner_alias"),
                result.getString("avatar_id"),
                result.getString("device_id")), classroomId);
    }

    private record ClassroomHeader(String classroomId, String classroomLabel, Instant updatedAt) {}

    public record ParticipantLocation(String classroomId, String learnerAlias, int participantCount) {
        public String participationScope() {
            return participantCount > 1 ? "GROUP" : "INDIVIDUAL";
        }
    }
}
