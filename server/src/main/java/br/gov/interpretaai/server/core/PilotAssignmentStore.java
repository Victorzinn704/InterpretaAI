package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.PilotAssignmentModels.Activity;
import br.gov.interpretaai.server.api.PilotAssignmentModels.AssignmentMember;
import br.gov.interpretaai.server.api.PilotAssignmentModels.AssignmentResponse;
import br.gov.interpretaai.server.api.PilotAssignmentModels.DrawingPrompt;
import br.gov.interpretaai.server.api.PilotAssignmentModels.PublishAssignmentRequest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
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
        return publish(deviceId, request.classroomLabel(), request.activity(), request.drawingPrompt(),
                List.of(new AssignmentMember(request.effectiveLearnerAlias(), request.avatarId())), now);
    }

    @Transactional
    public synchronized AssignmentResponse publish(
            String deviceId,
            String classroomLabel,
            Activity activity,
            DrawingPrompt drawingPrompt,
            List<AssignmentMember> members,
            Instant now) {
        if (members.isEmpty()) throw new IllegalArgumentException("assignment members cannot be empty");
        AssignmentMember primary = members.get(0);
        long version = jdbc.queryForList(
                        "select version from pilot_assignment where device_id = ?", Long.class, deviceId)
                .stream().findFirst().orElse(0L) + 1;
        int changed = jdbc.update("""
                update pilot_assignment
                   set version = ?, classroom_label = ?, avatar_id = ?, learner_alias = ?, activity = ?,
                       drawing_prompt = ?, updated_at = ?
                 where device_id = ?
                """, version, classroomLabel, primary.avatarId(), primary.learnerAlias(),
                activity.name(), drawingPrompt.name(), Timestamp.from(now), deviceId);
        if (changed == 0) {
            jdbc.update("""
                    insert into pilot_assignment
                    (device_id, version, classroom_label, avatar_id, learner_alias,
                     activity, drawing_prompt, updated_at)
                    values (?, ?, ?, ?, ?, ?, ?, ?)
                    """, deviceId, version, classroomLabel, primary.avatarId(),
                    primary.learnerAlias(), activity.name(), drawingPrompt.name(),
                    Timestamp.from(now));
        }
        jdbc.update("delete from pilot_assignment_member where device_id = ?", deviceId);
        for (int index = 0; index < members.size(); index++) {
            AssignmentMember member = members.get(index);
            jdbc.update("""
                    insert into pilot_assignment_member
                    (device_id, learner_alias, avatar_id, roster_order) values (?, ?, ?, ?)
                    """, deviceId, member.learnerAlias(), member.avatarId(), index);
        }
        return new AssignmentResponse(deviceId, version, classroomLabel, primary.avatarId(),
                primary.learnerAlias(), activity, drawingPrompt, List.copyOf(members), now);
    }

    public Optional<AssignmentResponse> find(String deviceId) {
        return jdbc.query("""
                select device_id, version, classroom_label, avatar_id, learner_alias,
                       activity, drawing_prompt, updated_at
                  from pilot_assignment where device_id = ?
                """, (result, row) -> new AssignmentResponse(
                result.getString("device_id"),
                result.getLong("version"),
                result.getString("classroom_label"),
                result.getString("avatar_id"),
                result.getString("learner_alias"),
                Activity.valueOf(result.getString("activity")),
                DrawingPrompt.valueOf(result.getString("drawing_prompt")),
                members(deviceId),
                result.getTimestamp("updated_at").toInstant()), deviceId).stream().findFirst();
    }

    private List<AssignmentMember> members(String deviceId) {
        return jdbc.query("""
                select learner_alias, avatar_id from pilot_assignment_member
                 where device_id = ? order by roster_order
                """, (result, row) -> new AssignmentMember(
                result.getString("learner_alias"), result.getString("avatar_id")), deviceId);
    }
}
