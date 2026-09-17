package br.gov.interpretaai.server.delivery;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DeliveryStore {
    public record AssignmentRecord(
            String assignmentId,
            String schoolId,
            String storyId,
            int storyVersion,
            String classroomId,
            String createdByUserId,
            String requestFingerprint,
            int priority,
            Instant availableFrom,
            Instant expiresAt,
            Instant createdAt) {}

    public record ManifestRecord(
            long deliverySequence,
            String assignmentId,
            String storyId,
            int storyVersion,
            int minAppVersion,
            String packSha256,
            String packJson,
            int priority,
            Instant expiresAt) {}

    public record AssetRecord(
            String objectKey,
            String mediaType,
            long bytes,
            String sha256) {}

    private final JdbcTemplate jdbc;

    public DeliveryStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<AssignmentRecord> findByIdempotency(
            String userId, String schoolId, String idempotencyKey) {
        return assignment("""
                select assignment_id, school_id, story_id, story_version, classroom_id,
                       created_by_user_id, request_fingerprint, priority, available_from,
                       expires_at, created_at
                  from story_assignment
                 where created_by_user_id = ? and school_id = ? and idempotency_key = ?
                """, userId, schoolId, idempotencyKey);
    }

    public boolean publishedVersionExists(String storyId, int version, String schoolId) {
        Integer count = jdbc.queryForObject("""
                select count(*) from story_version
                 where story_id = ? and version = ? and school_id = ? and state = 'PUBLISHED'
                """, Integer.class, storyId, version, schoolId);
        return count != null && count == 1;
    }

    public void insert(AssignmentRecord assignment, String idempotencyKey, Instant now) {
        jdbc.update("""
                insert into story_assignment
                (assignment_id, school_id, story_id, story_version, classroom_id,
                 created_by_user_id, idempotency_key, request_fingerprint, priority, status,
                 available_from, expires_at, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, ?, ?)
                """, assignment.assignmentId(), assignment.schoolId(), assignment.storyId(),
                assignment.storyVersion(), assignment.classroomId(), assignment.createdByUserId(),
                idempotencyKey, assignment.requestFingerprint(), assignment.priority(),
                Timestamp.from(assignment.availableFrom()), timestamp(assignment.expiresAt()),
                Timestamp.from(assignment.createdAt()), Timestamp.from(now));
    }

    public List<ManifestRecord> findManifest(
            String schoolId, String classroomId, int appVersion, long afterSequence, Instant now, int limit) {
        return jdbc.query("""
                select a.delivery_sequence, a.assignment_id, a.story_id, a.story_version,
                       s.min_app_version, s.pack_sha256, s.pack_json, a.priority, a.expires_at
                  from story_assignment a
                  join story_version s on s.story_id = a.story_id and s.version = a.story_version
                 where a.school_id = ? and a.classroom_id = ? and a.status = 'ACTIVE'
                   and s.school_id = a.school_id and s.state = 'PUBLISHED'
                   and s.min_app_version <= ? and a.available_from <= ?
                   and (a.expires_at is null or a.expires_at > ?)
                   and a.delivery_sequence > ?
                 order by a.delivery_sequence asc
                 limit ?
                """, (result, row) -> new ManifestRecord(
                result.getLong("delivery_sequence"),
                result.getString("assignment_id"),
                result.getString("story_id"),
                result.getInt("story_version"),
                result.getInt("min_app_version"),
                result.getString("pack_sha256"),
                result.getString("pack_json"),
                result.getInt("priority"),
                timestamp(result.getTimestamp("expires_at"))), schoolId, classroomId, appVersion,
                Timestamp.from(now), Timestamp.from(now), afterSequence, limit);
    }

    public Optional<ManifestRecord> findEligiblePack(
            String assignmentId, String schoolId, String classroomId, int appVersion, Instant now) {
        return jdbc.query("""
                select a.delivery_sequence, a.assignment_id, a.story_id, a.story_version,
                       s.min_app_version, s.pack_sha256, s.pack_json, a.priority, a.expires_at
                  from story_assignment a
                  join story_version s on s.story_id = a.story_id and s.version = a.story_version
                 where a.assignment_id = ? and a.school_id = ? and a.classroom_id = ?
                   and a.status = 'ACTIVE' and s.school_id = a.school_id and s.state = 'PUBLISHED'
                   and s.min_app_version <= ? and a.available_from <= ?
                   and (a.expires_at is null or a.expires_at > ?)
                """, (result, row) -> new ManifestRecord(
                result.getLong("delivery_sequence"),
                result.getString("assignment_id"),
                result.getString("story_id"),
                result.getInt("story_version"),
                result.getInt("min_app_version"),
                result.getString("pack_sha256"),
                result.getString("pack_json"),
                result.getInt("priority"),
                timestamp(result.getTimestamp("expires_at"))), assignmentId, schoolId, classroomId,
                appVersion, Timestamp.from(now), Timestamp.from(now)).stream().findFirst();
    }

    public Optional<AssetRecord> findEligibleAsset(
            String assignmentId,
            String assetId,
            String role,
            String schoolId,
            String classroomId,
            int appVersion,
            Instant now) {
        return jdbc.query("""
                select asset.object_key, asset.media_type, asset.bytes, asset.sha256
                  from story_assignment a
                  join story_version s on s.story_id = a.story_id and s.version = a.story_version
                  join story_version_asset asset
                    on asset.story_id = s.story_id and asset.story_version = s.version
                 where a.assignment_id = ? and asset.asset_id = ? and asset.role = ?
                   and a.school_id = ? and a.classroom_id = ? and a.status = 'ACTIVE'
                   and s.school_id = a.school_id and s.state = 'PUBLISHED'
                   and s.min_app_version <= ? and a.available_from <= ?
                   and (a.expires_at is null or a.expires_at > ?)
                """, (result, row) -> new AssetRecord(
                result.getString("object_key"), result.getString("media_type"),
                result.getLong("bytes"), result.getString("sha256")), assignmentId, assetId, role,
                schoolId, classroomId, appVersion, Timestamp.from(now), Timestamp.from(now)).stream().findFirst();
    }

    private Optional<AssignmentRecord> assignment(String sql, Object... values) {
        return jdbc.query(sql, (result, row) -> new AssignmentRecord(
                result.getString("assignment_id"),
                result.getString("school_id"),
                result.getString("story_id"),
                result.getInt("story_version"),
                result.getString("classroom_id"),
                result.getString("created_by_user_id"),
                result.getString("request_fingerprint"),
                result.getInt("priority"),
                result.getTimestamp("available_from").toInstant(),
                timestamp(result.getTimestamp("expires_at")),
                result.getTimestamp("created_at").toInstant()), values).stream().findFirst();
    }

    private static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static Instant timestamp(Timestamp value) {
        return value == null ? null : value.toInstant();
    }
}
