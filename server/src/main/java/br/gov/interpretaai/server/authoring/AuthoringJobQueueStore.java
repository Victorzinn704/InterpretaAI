package br.gov.interpretaai.server.authoring;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AuthoringJobQueueStore {
    public record ClaimedJob(
            String jobId,
            String schoolId,
            String requestedByUserId,
            String requestJson,
            int attempts) {}

    private final JdbcTemplate jdbc;

    public AuthoringJobQueueStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ClaimedJob> claimNext(Instant now, Duration lease) {
        var candidates = jdbc.query("""
                select job_id from authoring_job_queue
                 where (status in ('QUEUED', 'RETRYABLE') and available_at <= ?)
                    or (status = 'PROCESSING' and lease_until <= ?)
                 order by available_at, job_id limit 1
                """, (result, row) -> result.getString("job_id"),
                Timestamp.from(now), Timestamp.from(now));
        if (candidates.isEmpty()) return Optional.empty();
        String jobId = candidates.get(0);
        int changed = jdbc.update("""
                update authoring_job_queue
                   set status = 'PROCESSING', attempts = attempts + 1,
                       lease_until = ?, updated_at = ?
                 where job_id = ?
                   and ((status in ('QUEUED', 'RETRYABLE') and available_at <= ?)
                     or (status = 'PROCESSING' and lease_until <= ?))
                """, Timestamp.from(now.plus(lease)), Timestamp.from(now), jobId,
                Timestamp.from(now), Timestamp.from(now));
        if (changed != 1) return Optional.empty();
        int jobChanged = jdbc.update("""
                update authoring_job
                   set status = 'ANALYZING_MEDIA', progress_step = 'ANALYZING_MEDIA',
                       failure_code = null, failure_safe_message = null,
                       revision = revision + 1, updated_at = ?
                 where job_id = ? and status in ('QUEUED', 'FAILED_RETRYABLE', 'ANALYZING_MEDIA')
                """, Timestamp.from(now), jobId);
        if (jobChanged != 1) throw new IllegalStateException("authoring_job_not_claimable");
        return findClaim(jobId);
    }

    @Transactional
    public void markDelivered(ClaimedJob claim, Instant now) {
        int changed = jdbc.update("""
                update authoring_job_queue
                   set status = 'DELIVERED', lease_until = null, last_error_code = null, updated_at = ?
                 where job_id = ? and status = 'PROCESSING' and attempts = ? and lease_until > ?
                """, Timestamp.from(now), claim.jobId(), claim.attempts(), Timestamp.from(now));
        if (changed != 1) throw new IllegalStateException("authoring_job_lease_lost");
        int jobChanged = jdbc.update("""
                update authoring_job
                   set status = 'RETRIEVING_GUIDANCE', progress_step = 'RETRIEVING_GUIDANCE',
                       completed_steps = 1, revision = revision + 1, updated_at = ?
                 where job_id = ? and status = 'ANALYZING_MEDIA'
                """, Timestamp.from(now), claim.jobId());
        if (jobChanged != 1) throw new IllegalStateException("authoring_job_not_processing");
        jdbc.update("""
                insert into authoring_plan_queue
                (job_id, status, attempts, available_at, created_at, updated_at)
                values (?, 'QUEUED', 0, ?, ?, ?)
                """, claim.jobId(), Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
    }

    @Transactional
    public void retryOrFail(ClaimedJob job, String errorCode, Instant now) {
        boolean finalFailure = job.attempts() >= 3;
        if (finalFailure) {
            int queueChanged = jdbc.update("""
                    update authoring_job_queue
                       set status = 'DEAD', lease_until = null, last_error_code = ?, updated_at = ?
                     where job_id = ? and status = 'PROCESSING' and attempts = ? and lease_until > ?
                    """, errorCode, Timestamp.from(now), job.jobId(), job.attempts(),
                    Timestamp.from(now));
            if (queueChanged != 1) throw new IllegalStateException("authoring_job_lease_lost");
            updateFailure(job.jobId(), "FAILED_FINAL", errorCode,
                    "Não foi possível preparar o rascunho. Tente criar uma nova solicitação.", now);
            return;
        }
        Instant retryAt = now.plusSeconds(1L << Math.min(job.attempts(), 6));
        int queueChanged = jdbc.update("""
                update authoring_job_queue
                   set status = 'RETRYABLE', available_at = ?, lease_until = null,
                       last_error_code = ?, updated_at = ?
                 where job_id = ? and status = 'PROCESSING' and attempts = ? and lease_until > ?
                """, Timestamp.from(retryAt), errorCode, Timestamp.from(now), job.jobId(),
                job.attempts(), Timestamp.from(now));
        if (queueChanged != 1) throw new IllegalStateException("authoring_job_lease_lost");
        updateFailure(job.jobId(), "FAILED_RETRYABLE", errorCode,
                "A preparação teve uma falha temporária e será retomada.", now);
    }

    private void updateFailure(
            String jobId, String status, String code, String safeMessage, Instant now) {
        int changed = jdbc.update("""
                update authoring_job
                   set status = ?, progress_step = ?, failure_code = ?, failure_safe_message = ?,
                       revision = revision + 1, updated_at = ?
                 where job_id = ? and status = 'ANALYZING_MEDIA'
                """, status, status, code, safeMessage, Timestamp.from(now), jobId);
        if (changed != 1) throw new IllegalStateException("authoring_job_not_processing");
    }

    private Optional<ClaimedJob> findClaim(String jobId) {
        return jdbc.query("""
                select j.job_id, j.school_id, j.requested_by_user_id, j.request_json, q.attempts
                  from authoring_job j
                  join authoring_job_queue q on q.job_id = j.job_id
                 where j.job_id = ? and q.status = 'PROCESSING'
                """, (result, row) -> new ClaimedJob(
                        result.getString("job_id"),
                        result.getString("school_id"),
                        result.getString("requested_by_user_id"),
                        result.getString("request_json"),
                        result.getInt("attempts")), jobId).stream().findFirst();
    }
}
