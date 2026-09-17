package br.gov.interpretaai.server.story;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Immutable map from a declared story variant to the private, sanitized bytes that implement it.
 * Object keys remain server-only; the Android client only addresses an eligible assignment asset.
 */
@Repository
public class StoryVersionAssetStore {
    public record Binding(String assetId, String role, String mediaId) {}

    public record ReadyMedia(
            String mediaId, String objectKey, String mediaType, long bytes, String sha256) {}

    public record BoundAsset(
            String assetId,
            String role,
            String mediaId,
            String objectKey,
            String mediaType,
            long bytes,
            String sha256) {}

    private final JdbcTemplate jdbc;

    public StoryVersionAssetStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Only a sanitized derivative owned by the same school can be attached to a story version. */
    public Optional<ReadyMedia> findReadyMedia(String mediaId, String schoolId) {
        return jdbc.query("""
                select j.media_id, j.sanitized_object_key, j.output_media_type,
                       j.sanitized_bytes, j.sanitized_sha256
                  from media_sanitization_job j
                  join media_upload_session u on u.media_id = j.media_id
                 where j.media_id = ? and u.school_id = ? and j.status = 'READY'
                   and j.sanitized_object_key is not null
                   and j.output_media_type is not null
                   and j.sanitized_bytes is not null
                   and j.sanitized_sha256 is not null
                """, (result, row) -> new ReadyMedia(
                result.getString("media_id"),
                result.getString("sanitized_object_key"),
                result.getString("output_media_type"),
                result.getLong("sanitized_bytes"),
                result.getString("sanitized_sha256")), mediaId, schoolId).stream().findFirst();
    }

    /** Called in the same transaction that writes the immutable story bytes. */
    public void insertAll(
            String storyId,
            int version,
            List<BoundAsset> assets,
            Instant now) {
        for (BoundAsset asset : assets) {
            jdbc.update("""
                    insert into story_version_asset
                    (story_id, story_version, asset_id, role, media_id, object_key,
                     media_type, bytes, sha256, created_at)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, storyId, version, asset.assetId(), asset.role(), asset.mediaId(),
                    asset.objectKey(), asset.mediaType(), asset.bytes(), asset.sha256(),
                    Timestamp.from(now));
        }
    }

    public List<BoundAsset> findForVersion(String storyId, int version) {
        return jdbc.query("""
                select asset_id, role, media_id, object_key, media_type, bytes, sha256
                  from story_version_asset
                 where story_id = ? and story_version = ?
                 order by asset_id, role
                """, (result, row) -> new BoundAsset(
                result.getString("asset_id"), result.getString("role"),
                result.getString("media_id"), result.getString("object_key"),
                result.getString("media_type"), result.getLong("bytes"), result.getString("sha256")),
                storyId, version);
    }
}
