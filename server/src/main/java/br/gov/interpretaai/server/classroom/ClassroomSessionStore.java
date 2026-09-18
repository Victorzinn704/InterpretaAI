package br.gov.interpretaai.server.classroom;

import br.gov.interpretaai.server.classroom.ClassroomSessionModels.LearnerSeat;
import br.gov.interpretaai.server.classroom.ClassroomSessionModels.TeacherSeatStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ClassroomSessionStore {
    public record SessionRecord(
            String sessionId, String schoolId, String classroomId, String createdByUserId,
            String status, Instant expiresAt) {}
    public record ActiveDeviceSession(String sessionId, String classroomId, String learnerAlias) {}

    private final JdbcTemplate jdbc;

    public ClassroomSessionStore(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean hasActiveSession(String classroomId, Instant now) {
        Integer count = jdbc.queryForObject("""
                select count(*) from classroom_session
                 where classroom_id = ? and status = 'ACTIVE' and expires_at > ?
                """, Integer.class, classroomId, Timestamp.from(now));
        return count != null && count > 0;
    }

    public void expireSessions(String classroomId, Instant now) {
        jdbc.update("""
                update classroom_session set status = 'EXPIRED', closed_at = ?
                 where classroom_id = ? and status = 'ACTIVE' and expires_at <= ?
                """, Timestamp.from(now), classroomId, Timestamp.from(now));
    }

    public void replaceRoster(String classroomId, List<LearnerSeat> learners, Instant now) {
        jdbc.update("delete from institution_classroom_learner where classroom_id = ?", classroomId);
        learners.forEach(learner -> jdbc.update("""
                insert into institution_classroom_learner
                (learner_id, classroom_id, display_name, learner_alias, seat_number,
                 status, created_at, updated_at)
                values (?, ?, ?, ?, ?, 'ACTIVE', ?, ?)
                """, learner.learnerId(), classroomId, learner.displayName(),
                learner.learnerAlias(), learner.seatNumber(), Timestamp.from(now), Timestamp.from(now)));
    }

    public List<LearnerSeat> roster(String classroomId) {
        return jdbc.query("""
                select learner_id, display_name, learner_alias, seat_number
                  from institution_classroom_learner
                 where classroom_id = ? and status = 'ACTIVE'
                 order by seat_number
                """, (result, row) -> new LearnerSeat(
                result.getString("learner_id"), result.getString("display_name"),
                result.getString("learner_alias"), result.getInt("seat_number")), classroomId);
    }

    public void insertSession(
            String sessionId, String classroomId, String userId, String codeHash,
            Instant expiresAt, Instant now) {
        jdbc.update("""
                insert into classroom_session
                (session_id, classroom_id, created_by_user_id, join_code_hash,
                 status, expires_at, created_at)
                values (?, ?, ?, ?, 'ACTIVE', ?, ?)
                """, sessionId, classroomId, userId, codeHash,
                Timestamp.from(expiresAt), Timestamp.from(now));
    }

    public Optional<SessionRecord> findByCode(String codeHash, Instant now) {
        return session("""
                select s.session_id, c.school_id, s.classroom_id, s.created_by_user_id,
                       s.status, s.expires_at
                  from classroom_session s
                  join institution_classroom c on c.classroom_id = s.classroom_id
                 where s.join_code_hash = ? and s.status = 'ACTIVE' and s.expires_at > ?
                """, codeHash, Timestamp.from(now));
    }

    public Optional<SessionRecord> find(String sessionId) {
        return session("""
                select s.session_id, c.school_id, s.classroom_id, s.created_by_user_id,
                       s.status, s.expires_at
                  from classroom_session s
                  join institution_classroom c on c.classroom_id = s.classroom_id
                 where s.session_id = ?
                """, sessionId);
    }

    public boolean learnerAvailable(String sessionId, String learnerId) {
        Integer count = jdbc.queryForObject("""
                select count(*)
                  from classroom_session s
                  join institution_classroom_learner l on l.classroom_id = s.classroom_id
                 where s.session_id = ? and l.learner_id = ? and l.status = 'ACTIVE'
                   and not exists (
                       select 1 from classroom_session_device d
                        where d.session_id = s.session_id and d.learner_id = l.learner_id)
                """, Integer.class, sessionId, learnerId);
        return count != null && count == 1;
    }

    public void join(String sessionId, String deviceId, String learnerId, Instant now) {
        jdbc.update("delete from classroom_session_device where session_id = ? and device_id = ?",
                sessionId, deviceId);
        jdbc.update("""
                insert into classroom_session_device (session_id, device_id, learner_id, joined_at)
                values (?, ?, ?, ?)
                """, sessionId, deviceId, learnerId, Timestamp.from(now));
    }

    public Optional<ActiveDeviceSession> activeForDevice(String deviceId, Instant now) {
        return jdbc.query("""
                select s.session_id, s.classroom_id, l.learner_alias
                  from classroom_session_device d
                  join classroom_session s on s.session_id = d.session_id
                  join institution_classroom_learner l on l.learner_id = d.learner_id
                 where d.device_id = ? and s.status = 'ACTIVE' and s.expires_at > ?
                 order by d.joined_at desc limit 1
                """, (result, row) -> new ActiveDeviceSession(
                result.getString("session_id"), result.getString("classroom_id"),
                result.getString("learner_alias")), deviceId, Timestamp.from(now))
                .stream().findFirst();
    }

    public List<TeacherSeatStatus> seats(String sessionId) {
        return jdbc.query("""
                select l.learner_id, l.display_name, l.learner_alias, l.seat_number, d.device_id
                  from classroom_session s
                  join institution_classroom_learner l on l.classroom_id = s.classroom_id
                  left join classroom_session_device d
                    on d.session_id = s.session_id and d.learner_id = l.learner_id
                 where s.session_id = ? and l.status = 'ACTIVE'
                 order by l.seat_number
                """, (result, row) -> {
                    String deviceId = result.getString("device_id");
                    return new TeacherSeatStatus(
                            result.getString("learner_id"), result.getString("display_name"),
                            result.getString("learner_alias"), result.getInt("seat_number"),
                            deviceId, deviceId != null);
                }, sessionId);
    }

    public boolean close(String sessionId, Instant now) {
        return jdbc.update("""
                update classroom_session set status = 'CLOSED', closed_at = ?
                 where session_id = ? and status = 'ACTIVE'
                """, Timestamp.from(now), sessionId) == 1;
    }

    private Optional<SessionRecord> session(String sql, Object... parameters) {
        return jdbc.query(sql, (result, row) -> new SessionRecord(
                result.getString("session_id"), result.getString("school_id"),
                result.getString("classroom_id"), result.getString("created_by_user_id"),
                result.getString("status"), result.getTimestamp("expires_at").toInstant()),
                parameters).stream().findFirst();
    }
}
