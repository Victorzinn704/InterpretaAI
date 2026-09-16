package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.PilotLearningModels.ClassroomSummary;
import br.gov.interpretaai.server.api.PilotLearningModels.LearningEventRequest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PilotLearningStore {
    private final JdbcTemplate jdbc;

    public PilotLearningStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int insert(
            String deviceId,
            PilotClassroomStore.ParticipantLocation participant,
            LearningEventRequest event,
            Instant receivedAt) {
        return jdbc.update("""
                insert into pilot_learning_event
                    (event_id, device_id, classroom_id, learner_alias, activity_id, event_type,
                     modality, duration_ms, observation_category, participation_scope,
                     participant_count, occurred_at, received_at)
                select ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                 where not exists (select 1 from pilot_learning_event where event_id = ?)
                """,
                event.eventId(), deviceId, participant.classroomId(), participant.learnerAlias(),
                event.activityId(), event.type().name(), event.modality().name(), event.durationMs(),
                event.observationCategory().name(), participant.participationScope(),
                participant.participantCount(), Timestamp.from(event.occurredAt()),
                Timestamp.from(receivedAt), event.eventId());
    }

    public ClassroomSummary summary(String classroomId) {
        return jdbc.queryForObject("""
                select c.classroom_id, c.classroom_label, c.updated_at,
                       count(distinct p.learner_alias) participants,
                       count(distinct case when e.event_type = 'SESSION_STARTED' then e.event_id end) sessions,
                       count(distinct case when e.event_type in ('RESPONSE_SUBMITTED', 'OBSERVATION_RECORDED') then e.event_id end) participations,
                       count(distinct case when e.event_type = 'STAGE_COMPLETED' then e.event_id end) completed_stages,
                       count(distinct case when e.event_type = 'HELP_REQUESTED' then e.event_id end) help_requests,
                       count(distinct case when e.event_type = 'RESPONSE_SUBMITTED' and e.modality = 'VOICE' then e.event_id end) voice_responses,
                       coalesce(avg(case when e.event_type = 'RESPONSE_SUBMITTED' then e.duration_ms end), 0) average_response_ms
                  from pilot_classroom c
                  left join pilot_classroom_participant p on p.classroom_id = c.classroom_id
                  left join pilot_learning_event e on e.classroom_id = c.classroom_id
                 where c.classroom_id = ?
                 group by c.classroom_id, c.classroom_label, c.updated_at
                """, (result, row) -> new ClassroomSummary(
                result.getString("classroom_id"), result.getString("classroom_label"),
                result.getInt("participants"), result.getLong("sessions"),
                result.getLong("participations"), result.getLong("completed_stages"),
                result.getLong("help_requests"), result.getLong("voice_responses"),
                Math.round(result.getDouble("average_response_ms")),
                result.getTimestamp("updated_at").toInstant()), classroomId);
    }

    public List<ClassroomSummary> summaries() {
        return jdbc.query("""
                select c.classroom_id, c.classroom_label, c.updated_at,
                       count(distinct p.learner_alias) participants,
                       count(distinct case when e.event_type = 'SESSION_STARTED' then e.event_id end) sessions,
                       count(distinct case when e.event_type in ('RESPONSE_SUBMITTED', 'OBSERVATION_RECORDED') then e.event_id end) participations,
                       count(distinct case when e.event_type = 'STAGE_COMPLETED' then e.event_id end) completed_stages,
                       count(distinct case when e.event_type = 'HELP_REQUESTED' then e.event_id end) help_requests,
                       count(distinct case when e.event_type = 'RESPONSE_SUBMITTED' and e.modality = 'VOICE' then e.event_id end) voice_responses,
                       coalesce(avg(case when e.event_type = 'RESPONSE_SUBMITTED' then e.duration_ms end), 0) average_response_ms
                  from pilot_classroom c
                  left join pilot_classroom_participant p on p.classroom_id = c.classroom_id
                  left join pilot_learning_event e on e.classroom_id = c.classroom_id
                 group by c.classroom_id, c.classroom_label, c.updated_at
                 order by c.classroom_label, c.classroom_id
                """, (result, row) -> new ClassroomSummary(
                result.getString("classroom_id"), result.getString("classroom_label"),
                result.getInt("participants"), result.getLong("sessions"),
                result.getLong("participations"), result.getLong("completed_stages"),
                result.getLong("help_requests"), result.getLong("voice_responses"),
                Math.round(result.getDouble("average_response_ms")),
                result.getTimestamp("updated_at").toInstant()));
    }

    public void audit(String role, String scope, Instant accessedAt) {
        jdbc.update("""
                insert into pilot_administrative_access_audit
                    (access_role, resource_scope, accessed_at) values (?, ?, ?)
                """, role, scope, Timestamp.from(accessedAt));
    }
}
