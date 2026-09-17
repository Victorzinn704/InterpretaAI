package br.gov.interpretaai.server.story;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Persistence boundary for immutable pack bytes and their human publication state.
 *
 * <p>No method updates {@code pack_json} or {@code pack_sha256}: editing a story must create a
 * new version before it reaches this store.
 */
@Repository
public class StoryVersionStore {
    public record Version(
            String storyId,
            int version,
            String schoolId,
            String authorUserId,
            String packSha256,
            String state,
            long revision,
            Instant updatedAt) {}

    public record Transition(String requestFingerprint, long appliedRevision) {}

    private final JdbcTemplate jdbc;

    public StoryVersionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Internal ingress used by the future validated authoring worker, never by an adult HTTP route. */
    void insertReviewableDraft(
            String storyId,
            int version,
            String schoolId,
            String authorUserId,
            String authoringJobId,
            String packJson,
            String packSha256,
            Instant now) {
        jdbc.update("""
                insert into story_version
                (story_id, version, school_id, author_user_id, authoring_job_id,
                 pack_json, pack_sha256, state, revision, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, 'DRAFT', 1, ?, ?)
                """, storyId, version, schoolId, authorUserId, authoringJobId,
                packJson, packSha256, Timestamp.from(now), Timestamp.from(now));
    }

    public Optional<Version> findInSchool(String storyId, int version, String schoolId) {
        return jdbc.query("""
                select story_id, version, school_id, author_user_id, pack_sha256,
                       state, revision, updated_at
                  from story_version
                 where story_id = ? and version = ? and school_id = ?
                """, (result, row) -> new Version(
                result.getString("story_id"),
                result.getInt("version"),
                result.getString("school_id"),
                result.getString("author_user_id"),
                result.getString("pack_sha256"),
                result.getString("state"),
                result.getLong("revision"),
                result.getTimestamp("updated_at").toInstant()), storyId, version, schoolId)
                .stream().findFirst();
    }

    public Optional<Transition> findTransition(
            String storyId, int version, String action, String actorUserId, String idempotencyKey) {
        return jdbc.query("""
                select request_fingerprint, applied_revision
                  from story_version_transition
                 where story_id = ? and version = ? and action = ?
                   and actor_user_id = ? and idempotency_key = ?
                """, (result, row) -> new Transition(
                result.getString("request_fingerprint"), result.getLong("applied_revision")),
                storyId, version, action, actorUserId, idempotencyKey).stream().findFirst();
    }

    public boolean approve(
            String storyId, int version, long expectedRevision, String actorUserId, Instant now) {
        return jdbc.update("""
                update story_version
                   set state = 'APPROVED', revision = revision + 1,
                       approved_by_user_id = ?, approved_at = ?, updated_at = ?
                 where story_id = ? and version = ? and state = 'DRAFT' and revision = ?
                """, actorUserId, Timestamp.from(now), Timestamp.from(now),
                storyId, version, expectedRevision) == 1;
    }

    public boolean publish(String storyId, int version, String actorUserId, Instant now) {
        return jdbc.update("""
                update story_version
                   set state = 'PUBLISHED', revision = revision + 1,
                       published_by_user_id = ?, published_at = ?, updated_at = ?
                 where story_id = ? and version = ? and state = 'APPROVED'
                """, actorUserId, Timestamp.from(now), Timestamp.from(now), storyId, version) == 1;
    }

    public void insertTransition(
            String storyId,
            int version,
            String action,
            String actorUserId,
            String idempotencyKey,
            String requestFingerprint,
            long appliedRevision,
            Instant now) {
        jdbc.update("""
                insert into story_version_transition
                (story_id, version, action, actor_user_id, idempotency_key,
                 request_fingerprint, applied_revision, created_at)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """, storyId, version, action, actorUserId, idempotencyKey,
                requestFingerprint, appliedRevision, Timestamp.from(now));
    }
}
