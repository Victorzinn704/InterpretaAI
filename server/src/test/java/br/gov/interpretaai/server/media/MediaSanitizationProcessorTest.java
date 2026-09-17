package br.gov.interpretaai.server.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "interpretaai.media.local-root=build/test-sanitization-media",
        "interpretaai.media.worker-enabled=false",
        "interpretaai.conversation.provider=gemini",
        "interpretaai.gemini.api-key=",
        "interpretaai.speech.provider=kokoro",
        "interpretaai.kokoro.base-url=http://127.0.0.1:1",
        "spring.datasource.url=jdbc:h2:mem:media-sanitization;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
class MediaSanitizationProcessorTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PrivateObjectStore objects;
    @Autowired MediaSanitizationProcessor processor;

    @BeforeEach
    void seedInstitution() {
        jdbc.update("delete from institution_audit_event");
        jdbc.update("delete from media_sanitization_job");
        jdbc.update("delete from media_upload_session");
        jdbc.update("delete from institution_teacher_classroom");
        jdbc.update("delete from institution_school_membership");
        jdbc.update("delete from institution_classroom");
        jdbc.update("delete from institution_adult_user");
        jdbc.update("delete from institution_school");
        jdbc.update("delete from institution_tenant");
        Instant now = Instant.parse("2026-09-16T12:00:00Z");
        jdbc.update("""
                insert into institution_tenant(tenant_id, name, status, created_at)
                values ('tenant_rio', 'Rede Rio', 'ACTIVE', ?)
                """, Timestamp.from(now));
        jdbc.update("""
                insert into institution_school(school_id, tenant_id, name, status, created_at)
                values ('school_centro', 'tenant_rio', 'Escola Centro', 'ACTIVE', ?)
                """, Timestamp.from(now));
        jdbc.update("""
                insert into institution_adult_user(user_id, oidc_subject, status, created_at)
                values ('user_teacher', 'oidc|teacher', 'ACTIVE', ?)
                """, Timestamp.from(now));
    }

    @Test
    void reencodesTheImageAndMarksOnlyTheSanitizedDerivativeReady() throws Exception {
        insertUploaded("media_valid", imagePng(3, 2), "image/png", true);

        assertThat(processor.processOne()).isTrue();

        var row = jdbc.queryForMap("""
                select status, sanitized_object_key, sanitized_bytes, width, height, output_media_type
                  from media_sanitization_job where media_id = 'media_valid'
                """);
        assertThat(row.get("status")).isEqualTo("READY");
        assertThat(row.get("width")).isEqualTo(3);
        assertThat(row.get("height")).isEqualTo(2);
        assertThat(row.get("output_media_type")).isEqualTo("image/png");
        Path derivative = Path.of("build/test-sanitization-media")
                .resolve((String) row.get("sanitized_object_key"));
        assertThat(derivative).exists();
        assertThat(row.get("sanitized_bytes")).isEqualTo(Files.size(derivative));
        assertThat(ImageIO.read(derivative.toFile()).getWidth()).isEqualTo(3);
        assertThat(jdbc.queryForObject("""
                select count(*) from institution_audit_event
                 where target_id = 'media_valid' and action = 'MEDIA_SANITIZED'
                """, Integer.class)).isEqualTo(1);
    }

    @Test
    void rejectsBytesThatAreNotAnImageWithoutRetrying() throws Exception {
        insertUploaded("media_invalid", new byte[] {1, 2, 3}, "image/png", true);

        assertThat(processor.processOne()).isTrue();

        var row = jdbc.queryForMap("""
                select status, attempts, error_code from media_sanitization_job
                 where media_id = 'media_invalid'
                """);
        assertThat(row.get("status")).isEqualTo("REJECTED");
        assertThat(row.get("attempts")).isEqualTo(1);
        assertThat(row.get("error_code")).isEqualTo("image_unreadable");
    }

    @Test
    void retriesStorageFailureThenStopsAfterThreeLeasedAttempts() throws Exception {
        insertUploaded("media_missing", new byte[] {1}, "image/png", false);

        for (int attempt = 1; attempt <= 3; attempt++) {
            assertThat(processor.processOne()).isTrue();
            var row = jdbc.queryForMap("""
                    select status, attempts from media_sanitization_job
                     where media_id = 'media_missing'
                    """);
            assertThat(row.get("attempts")).isEqualTo(attempt);
            assertThat(row.get("status")).isEqualTo(attempt == 3 ? "FAILED_FINAL" : "RETRYABLE");
            jdbc.update("""
                    update media_sanitization_job set available_at = ? where media_id = 'media_missing'
                    """, Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")));
        }
        assertThat(processor.processOne()).isFalse();
    }

    private void insertUploaded(
            String mediaId, byte[] bytes, String mediaType, boolean storeRaw) throws Exception {
        String objectKey = "raw/school_centro/" + mediaId + "/original";
        String sha = "0000000000000000000000000000000000000000000000000000000000000000";
        if (storeRaw) {
            var staged = objects.stage(mediaId, new ByteArrayInputStream(bytes), bytes.length);
            objects.commit(staged, objectKey);
            sha = staged.sha256();
        }
        Instant now = Instant.now();
        jdbc.update("""
                insert into media_upload_session
                (media_id, school_id, owner_user_id, idempotency_key, request_fingerprint,
                 original_file_name, media_type, declared_bytes, status, object_key,
                 actual_bytes, sha256, expires_at, created_at, updated_at)
                values (?, 'school_centro', 'user_teacher', ?, ?, 'image.png', ?, ?,
                        'UPLOADED', ?, ?, ?, ?, ?, ?)
                """, mediaId, "key-" + mediaId, sha, mediaType, bytes.length,
                objectKey, bytes.length, sha, Timestamp.from(now.plusSeconds(900)),
                Timestamp.from(now), Timestamp.from(now));
        jdbc.update("""
                insert into media_sanitization_job
                (media_id, status, attempts, available_at, created_at, updated_at)
                values (?, 'QUEUED', 0, ?, ?, ?)
                """, mediaId, Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
    }

    private byte[] imagePng(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(Color.BLUE);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        try (var output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } finally {
            image.flush();
        }
    }
}
