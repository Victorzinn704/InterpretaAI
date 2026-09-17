package br.gov.interpretaai.server.authoring;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Durable, fenced handoff from an approved guidance lookup to a teacher-only draft plan. */
@Repository
public class AuthoringPlanQueueStore {
    public record ClaimedPlan(String jobId, String schoolId, String userId,
            String requestJson, int attempts) {}
    public record StoredPlan(String planJson, String sha256) {}

    private final JdbcTemplate jdbc;

    public AuthoringPlanQueueStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<StoredPlan> findDeliveredInSchool(String jobId, String schoolId) {
        return jdbc.query("""
                select p.plan_json, p.plan_sha256
                  from authoring_plan_queue p join authoring_job j on j.job_id = p.job_id
                 where p.job_id = ? and j.school_id = ? and p.status = 'DELIVERED'
                """, (result, row) -> new StoredPlan(result.getString("plan_json"),
                result.getString("plan_sha256")), jobId, schoolId).stream().findFirst();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<ClaimedPlan> claimNext(Instant now, Duration lease) {
        var candidates = jdbc.query("""
                select p.job_id from authoring_plan_queue p
                  join authoring_job j on j.job_id = p.job_id
                 where j.status in ('RETRIEVING_GUIDANCE', 'FAILED_RETRYABLE')
                   and ((p.status in ('QUEUED', 'RETRYABLE') and p.available_at <= ?)
                     or (p.status = 'PROCESSING' and p.lease_until <= ?))
                 order by p.available_at, p.job_id limit 1
                """, (result, row) -> result.getString("job_id"),
                Timestamp.from(now), Timestamp.from(now));
        if (candidates.isEmpty()) return Optional.empty();
        String jobId = candidates.get(0);
        int changed = jdbc.update("""
                update authoring_plan_queue
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
                   set status = 'RETRIEVING_GUIDANCE', progress_step = 'RETRIEVING_GUIDANCE',
                       failure_code = null, failure_safe_message = null,
                       revision = revision + 1, updated_at = ?
                 where job_id = ? and status in ('RETRIEVING_GUIDANCE', 'FAILED_RETRYABLE')
                """, Timestamp.from(now), jobId);
        if (jobChanged != 1) throw new IllegalStateException("authoring_plan_not_claimable");
        return jdbc.query("""
                select j.job_id, j.school_id, j.requested_by_user_id, j.request_json, p.attempts
                  from authoring_job j join authoring_plan_queue p on p.job_id = j.job_id
                 where j.job_id = ? and p.status = 'PROCESSING'
                """, (result, row) -> new ClaimedPlan(result.getString("job_id"),
                result.getString("school_id"), result.getString("requested_by_user_id"),
                result.getString("request_json"), result.getInt("attempts")), jobId)
                .stream().findFirst();
    }

    @Transactional
    public void complete(ClaimedPlan claim, String planJson, Instant now) {
        if (planJson == null || planJson.isBlank()) throw new IllegalArgumentException("plan_empty");
        int changed = jdbc.update("""
                update authoring_plan_queue
                   set status = 'DELIVERED', lease_until = null, last_error_code = null,
                       plan_json = ?, plan_sha256 = ?, updated_at = ?
                 where job_id = ? and status = 'PROCESSING' and attempts = ? and lease_until > ?
                """, planJson, sha256(planJson), Timestamp.from(now), claim.jobId(),
                claim.attempts(), Timestamp.from(now));
        if (changed != 1) throw new IllegalStateException("authoring_plan_lease_lost");
        int jobChanged = jdbc.update("""
                update authoring_job
                   set status = 'GENERATING_STORY', progress_step = 'GENERATING_STORY',
                       completed_steps = 2, revision = revision + 1, updated_at = ?
                 where job_id = ? and status = 'RETRIEVING_GUIDANCE'
                """, Timestamp.from(now), claim.jobId());
        if (jobChanged != 1) throw new IllegalStateException("authoring_plan_not_processing");
    }

    @Transactional
    public void pauseForGuidance(ClaimedPlan claim, Instant now) {
        int changed = jdbc.update("""
                update authoring_plan_queue
                   set status = 'PAUSED', lease_until = null,
                       last_error_code = 'guidance_not_approved', updated_at = ?
                 where job_id = ? and status = 'PROCESSING' and attempts = ? and lease_until > ?
                """, Timestamp.from(now), claim.jobId(), claim.attempts(), Timestamp.from(now));
        if (changed != 1) throw new IllegalStateException("authoring_plan_lease_lost");
        updateFailure(claim.jobId(), "NEEDS_TEACHER_INPUT", "guidance_not_approved",
                "Ainda não há orientação pedagógica aprovada para este pedido."
                        + " A professora pode planejar manualmente.", now);
    }

    @Transactional
    public void retryOrFail(ClaimedPlan claim, String errorCode, Instant now) {
        boolean finalFailure = claim.attempts() >= 3;
        Instant availableAt = now.plusSeconds(1L << Math.min(claim.attempts(), 6));
        int changed = jdbc.update("""
                update authoring_plan_queue
                   set status = ?, available_at = ?, lease_until = null,
                       last_error_code = ?, updated_at = ?
                 where job_id = ? and status = 'PROCESSING' and attempts = ? and lease_until > ?
                """, finalFailure ? "DEAD" : "RETRYABLE", Timestamp.from(availableAt),
                errorCode, Timestamp.from(now), claim.jobId(), claim.attempts(), Timestamp.from(now));
        if (changed != 1) throw new IllegalStateException("authoring_plan_lease_lost");
        updateFailure(claim.jobId(), finalFailure ? "FAILED_FINAL" : "FAILED_RETRYABLE",
                errorCode, finalFailure
                        ? "Não foi possível preparar o plano. Crie uma nova solicitação."
                        : "O planejamento teve uma falha temporária e será retomado.", now);
    }

    private void updateFailure(String jobId, String status, String code,
            String safeMessage, Instant now) {
        int changed = jdbc.update("""
                update authoring_job
                   set status = ?, progress_step = ?, failure_code = ?, failure_safe_message = ?,
                       revision = revision + 1, updated_at = ?
                 where job_id = ? and status = 'RETRIEVING_GUIDANCE'
                """, status, status, code, safeMessage, Timestamp.from(now), jobId);
        if (changed != 1) throw new IllegalStateException("authoring_plan_not_processing");
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
