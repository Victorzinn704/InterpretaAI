package br.gov.interpretaai.server.media;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class MediaUploadStore {
    public record Upload(
            String mediaId,
            String schoolId,
            String ownerUserId,
            String idempotencyKey,
            String requestFingerprint,
            String fileName,
            String mediaType,
            long declaredBytes,
            String status,
            String objectKey,
            Long actualBytes,
            String sha256,
            Instant expiresAt,
            Instant createdAt) {}

    private final JdbcTemplate jdbc;

    public MediaUploadStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Upload> findByIdempotency(
            String ownerUserId, String schoolId, String idempotencyKey) {
        return query("""
                select * from media_upload_session
                 where owner_user_id = ? and school_id = ? and idempotency_key = ?
                """, ownerUserId, schoolId, idempotencyKey);
    }

    public Optional<Upload> findOwned(String mediaId, String ownerUserId, String schoolId) {
        return query("""
                select * from media_upload_session
                 where media_id = ? and owner_user_id = ? and school_id = ?
                """, mediaId, ownerUserId, schoolId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insert(Upload upload, Instant now) {
        jdbc.update("""
                insert into media_upload_session
                (media_id, school_id, owner_user_id, idempotency_key, request_fingerprint,
                 original_file_name, media_type, declared_bytes, status, object_key,
                 expires_at, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, upload.mediaId(), upload.schoolId(), upload.ownerUserId(),
                upload.idempotencyKey(), upload.requestFingerprint(), upload.fileName(),
                upload.mediaType(), upload.declaredBytes(), upload.status(), upload.objectKey(),
                Timestamp.from(upload.expiresAt()), Timestamp.from(upload.createdAt()),
                Timestamp.from(now));
    }

    public boolean markUploaded(
            String mediaId, String objectKey, long actualBytes, String sha256, Instant now) {
        return jdbc.update("""
                update media_upload_session
                   set status = 'UPLOADED', object_key = ?, actual_bytes = ?, sha256 = ?, updated_at = ?
                 where media_id = ? and status = 'PENDING_UPLOAD'
                """, objectKey, actualBytes, sha256, Timestamp.from(now), mediaId) == 1;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markExpired(String mediaId, Instant now) {
        jdbc.update("""
                update media_upload_session set status = 'EXPIRED', updated_at = ?
                 where media_id = ? and status = 'PENDING_UPLOAD'
                """, Timestamp.from(now), mediaId);
    }

    private Optional<Upload> query(String sql, Object... parameters) {
        return jdbc.query(sql, (result, row) -> new Upload(
                result.getString("media_id"),
                result.getString("school_id"),
                result.getString("owner_user_id"),
                result.getString("idempotency_key"),
                result.getString("request_fingerprint"),
                result.getString("original_file_name"),
                result.getString("media_type"),
                result.getLong("declared_bytes"),
                result.getString("status"),
                result.getString("object_key"),
                result.getObject("actual_bytes", Long.class),
                result.getString("sha256"),
                result.getTimestamp("expires_at").toInstant(),
                result.getTimestamp("created_at").toInstant()), parameters).stream().findFirst();
    }
}
