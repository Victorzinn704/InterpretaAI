package br.gov.interpretaai.server.media;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MediaSanitizationStore {
    public record Job(
            String mediaId,
            String schoolId,
            String ownerUserId,
            String rawObjectKey,
            String declaredMediaType,
            int attempts) {}

    private final JdbcTemplate jdbc;

    public MediaSanitizationStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void enqueue(String mediaId, Instant now) {
        jdbc.update("""
                insert into media_sanitization_job
                (media_id, status, attempts, available_at, created_at, updated_at)
                values (?, 'QUEUED', 0, ?, ?, ?)
                """, mediaId, Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<Job> claimNext(Instant now, Duration lease) {
        var candidates = jdbc.query("""
                select j.media_id
                  from media_sanitization_job j
                 where (j.status in ('QUEUED', 'RETRYABLE') and j.available_at <= ?)
                    or (j.status = 'PROCESSING' and j.lease_until <= ?)
                 order by j.available_at, j.media_id limit 1
                """, (result, row) -> result.getString("media_id"),
                Timestamp.from(now), Timestamp.from(now));
        if (candidates.isEmpty()) return Optional.empty();
        String mediaId = candidates.get(0);
        int changed = jdbc.update("""
                update media_sanitization_job
                   set status = 'PROCESSING', attempts = attempts + 1,
                       lease_until = ?, updated_at = ?
                 where media_id = ?
                   and ((status in ('QUEUED', 'RETRYABLE') and available_at <= ?)
                     or (status = 'PROCESSING' and lease_until <= ?))
                """, Timestamp.from(now.plus(lease)), Timestamp.from(now), mediaId,
                Timestamp.from(now), Timestamp.from(now));
        return changed == 1 ? findJob(mediaId) : Optional.empty();
    }

    public void markReady(
            String mediaId,
            String objectKey,
            String sha256,
            long bytes,
            int width,
            int height,
            Instant now) {
        int changed = jdbc.update("""
                update media_sanitization_job
                   set status = 'READY', sanitized_object_key = ?, sanitized_sha256 = ?,
                       sanitized_bytes = ?,
                       width = ?, height = ?, output_media_type = 'image/png',
                       lease_until = null, error_code = null, updated_at = ?
                 where media_id = ? and status = 'PROCESSING'
                """, objectKey, sha256, bytes, width, height, Timestamp.from(now), mediaId);
        if (changed != 1) throw new IllegalStateException("sanitization_job_not_processing");
    }

    public void reject(String mediaId, String errorCode, Instant now) {
        int changed = jdbc.update("""
                update media_sanitization_job
                   set status = 'REJECTED', error_code = ?, lease_until = null, updated_at = ?
                 where media_id = ? and status = 'PROCESSING'
                """, errorCode, Timestamp.from(now), mediaId);
        if (changed != 1) throw new IllegalStateException("sanitization_job_not_processing");
    }

    public void retryOrFail(Job job, String errorCode, Instant now) {
        if (job.attempts() >= 3) {
            jdbc.update("""
                    update media_sanitization_job
                       set status = 'FAILED_FINAL', error_code = ?, lease_until = null, updated_at = ?
                     where media_id = ? and status = 'PROCESSING'
                    """, errorCode, Timestamp.from(now), job.mediaId());
            return;
        }
        long delaySeconds = 1L << Math.min(job.attempts(), 6);
        jdbc.update("""
                update media_sanitization_job
                   set status = 'RETRYABLE', error_code = ?, available_at = ?,
                       lease_until = null, updated_at = ?
                 where media_id = ? and status = 'PROCESSING'
                """, errorCode, Timestamp.from(now.plusSeconds(delaySeconds)),
                Timestamp.from(now), job.mediaId());
    }

    private Optional<Job> findJob(String mediaId) {
        return jdbc.query("""
                select j.media_id, u.school_id, u.owner_user_id, u.object_key,
                       u.media_type, j.attempts
                  from media_sanitization_job j
                  join media_upload_session u on u.media_id = j.media_id
                 where j.media_id = ?
                """, (result, row) -> new Job(
                        result.getString("media_id"),
                        result.getString("school_id"),
                        result.getString("owner_user_id"),
                        result.getString("object_key"),
                        result.getString("media_type"),
                        result.getInt("attempts")), mediaId).stream().findFirst();
    }
}
