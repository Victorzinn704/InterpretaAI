package br.gov.interpretaai.server.identity;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionAuditStore {
    private final JdbcTemplate jdbc;

    public InstitutionAuditStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void append(
            String actorUserId,
            String schoolId,
            String action,
            String targetType,
            String targetId,
            Instant occurredAt) {
        jdbc.update("""
                insert into institution_audit_event
                (actor_user_id, school_id, action, target_type, target_id, occurred_at)
                values (?, ?, ?, ?, ?, ?)
                """, actorUserId, schoolId, action, targetType, targetId,
                Timestamp.from(occurredAt));
    }
}
