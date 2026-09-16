package br.gov.interpretaai.server.authoring;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuthoringJobStore {
    public record Job(
            String jobId,
            String schoolId,
            String requestedByUserId,
            String idempotencyKey,
            String requestFingerprint,
            String requestJson,
            String sourceType,
            String sourceRef,
            String status,
            String progressStep,
            int completedSteps,
            int totalSteps,
            long revision,
            String failureCode,
            String failureSafeMessage,
            Instant createdAt,
            Instant updatedAt) {}

    private final JdbcTemplate jdbc;

    public AuthoringJobStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Job> findByIdempotency(
            String userId, String schoolId, String idempotencyKey) {
        return query("""
                select * from authoring_job
                 where requested_by_user_id = ? and school_id = ? and idempotency_key = ?
                """, userId, schoolId, idempotencyKey);
    }

    public Optional<Job> findInSchool(String jobId, String schoolId) {
        return query("""
                select * from authoring_job where job_id = ? and school_id = ?
                """, jobId, schoolId);
    }

    public boolean sanitizedMediaIsReadyForOwner(
            String mediaId, String schoolId, String ownerUserId) {
        Integer count = jdbc.queryForObject("""
                select count(*)
                  from media_sanitization_job j
                  join media_upload_session u on u.media_id = j.media_id
                 where j.media_id = ? and u.school_id = ? and u.owner_user_id = ?
                   and j.status = 'READY'
                """, Integer.class, mediaId, schoolId, ownerUserId);
        return count != null && count == 1;
    }

    public void insertWithQueue(Job job) {
        jdbc.update("""
                insert into authoring_job
                (job_id, school_id, requested_by_user_id, idempotency_key,
                 request_fingerprint, request_json, source_type, source_ref, status,
                 progress_step, completed_steps, total_steps, revision, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, job.jobId(), job.schoolId(), job.requestedByUserId(), job.idempotencyKey(),
                job.requestFingerprint(), job.requestJson(), job.sourceType(), job.sourceRef(),
                job.status(), job.progressStep(), job.completedSteps(), job.totalSteps(),
                job.revision(), Timestamp.from(job.createdAt()), Timestamp.from(job.updatedAt()));
        jdbc.update("""
                insert into authoring_job_queue
                (job_id, status, attempts, available_at, created_at, updated_at)
                values (?, 'QUEUED', 0, ?, ?, ?)
                """, job.jobId(), Timestamp.from(job.createdAt()),
                Timestamp.from(job.createdAt()), Timestamp.from(job.updatedAt()));
    }

    private Optional<Job> query(String sql, Object... parameters) {
        return jdbc.query(sql, (result, row) -> new Job(
                result.getString("job_id"),
                result.getString("school_id"),
                result.getString("requested_by_user_id"),
                result.getString("idempotency_key"),
                result.getString("request_fingerprint"),
                result.getString("request_json"),
                result.getString("source_type"),
                result.getString("source_ref"),
                result.getString("status"),
                result.getString("progress_step"),
                result.getInt("completed_steps"),
                result.getInt("total_steps"),
                result.getLong("revision"),
                result.getString("failure_code"),
                result.getString("failure_safe_message"),
                result.getTimestamp("created_at").toInstant(),
                result.getTimestamp("updated_at").toInstant()), parameters).stream().findFirst();
    }
}
