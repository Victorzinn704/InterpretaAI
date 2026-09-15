package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.PilotAssignmentModels.Activity;
import br.gov.interpretaai.server.api.PilotAssignmentModels.AssignmentResponse;
import br.gov.interpretaai.server.api.PilotAssignmentModels.DrawingPrompt;
import br.gov.interpretaai.server.api.PilotAssignmentModels.PublishAssignmentRequest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class PilotAssignmentStore {
    private final JdbcTemplate jdbc;

    public PilotAssignmentStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public synchronized AssignmentResponse publish(
            String deviceId, PublishAssignmentRequest request, Instant now) {
        long version = jdbc.queryForList(
                        "select version from pilot_assignment where device_id = ?", Long.class, deviceId)
                .stream().findFirst().orElse(0L) + 1;
        int changed = jdbc.update("""
                update pilot_assignment
                   set version = ?, classroom_label = ?, avatar_id = ?, activity = ?,
                       drawing_prompt = ?, updated_at = ?
                 where device_id = ?
                """, version, request.classroomLabel(), request.avatarId(), request.activity().name(),
                request.drawingPrompt().name(), Timestamp.from(now), deviceId);
        if (changed == 0) {
            jdbc.update("""
                    insert into pilot_assignment
                    (device_id, version, classroom_label, avatar_id, activity, drawing_prompt, updated_at)
                    values (?, ?, ?, ?, ?, ?, ?)
                    """, deviceId, version, request.classroomLabel(), request.avatarId(),
                    request.activity().name(), request.drawingPrompt().name(), Timestamp.from(now));
        }
        return new AssignmentResponse(deviceId, version, request.classroomLabel(), request.avatarId(),
                request.activity(), request.drawingPrompt(), now);
    }

    public Optional<AssignmentResponse> find(String deviceId) {
        return jdbc.query("""
                select device_id, version, classroom_label, avatar_id, activity, drawing_prompt, updated_at
                  from pilot_assignment where device_id = ?
                """, (result, row) -> new AssignmentResponse(
                result.getString("device_id"),
                result.getLong("version"),
                result.getString("classroom_label"),
                result.getString("avatar_id"),
                Activity.valueOf(result.getString("activity")),
                DrawingPrompt.valueOf(result.getString("drawing_prompt")),
                result.getTimestamp("updated_at").toInstant()), deviceId).stream().findFirst();
    }
}
