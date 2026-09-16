package br.gov.interpretaai.server.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "interpretaai.identity.oidc-enabled=true",
        "interpretaai.identity.issuer-uri=https://identity.test.example",
        "interpretaai.identity.audience=interpretaai-api",
        "interpretaai.media.local-root=build/test-private-media",
        "interpretaai.conversation.provider=gemini",
        "interpretaai.gemini.api-key=",
        "interpretaai.speech.provider=kokoro",
        "interpretaai.kokoro.base-url=http://127.0.0.1:1",
        "spring.datasource.url=jdbc:h2:mem:media-upload;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class MediaUploadControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @MockitoBean JwtDecoder jwtDecoder;

    @BeforeEach
    void seedInstitution() {
        jdbc.update("delete from institution_audit_event");
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
        school("school_centro", now);
        school("school_norte", now);
        jdbc.update("""
                insert into institution_adult_user(user_id, oidc_subject, status, created_at)
                values ('user_teacher', 'oidc|teacher', 'ACTIVE', ?)
                """, Timestamp.from(now));
        jdbc.update("""
                insert into institution_school_membership
                (user_id, school_id, role, status, created_at, updated_at)
                values ('user_teacher', 'school_centro', 'TEACHER', 'ACTIVE', ?, ?)
                """, Timestamp.from(now), Timestamp.from(now));
    }

    @Test
    void createsReceivesAndIdempotentlyReplaysAPrivateUpload() throws Exception {
        String key = "upload-key-00000001";
        String sessionJson = create(key, body("maca.png", 3))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mediaId").isString())
                .andExpect(jsonPath("$.uploadUrl").value(org.hamcrest.Matchers.containsString(
                        "/api/v2/media/uploads/")))
                .andReturn().getResponse().getContentAsString();
        JsonNode session = mapper.readTree(sessionJson);
        String mediaId = session.get("mediaId").asText();

        assertThat(create(key, body("maca.png", 3))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).contains(mediaId);

        upload(mediaId, new byte[] {1, 2, 3}, MediaType.IMAGE_PNG)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPLOADED"))
                .andExpect(jsonPath("$.bytes").value(3));
        upload(mediaId, new byte[] {1, 2, 3}, MediaType.IMAGE_PNG)
                .andExpect(status().isOk());
        upload(mediaId, new byte[] {3, 2, 1}, MediaType.IMAGE_PNG)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("media_already_uploaded"));

        var row = jdbc.queryForMap(
                "select status, object_key, sha256 from media_upload_session where media_id = ?",
                mediaId);
        assertThat(row.get("status")).isEqualTo("UPLOADED");
        assertThat(Files.readAllBytes(Path.of("build/test-private-media")
                        .resolve((String) row.get("object_key"))))
                .containsExactly(1, 2, 3);
        assertThat(jdbc.queryForObject(
                "select count(*) from institution_audit_event where target_id = ?",
                Integer.class, mediaId)).isEqualTo(2);
        mvc.perform(get("/raw/school_centro/{mediaId}/{hash}", mediaId, row.get("sha256")))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsIdempotencyReuseWithDifferentMetadata() throws Exception {
        String key = "upload-key-00000002";
        create(key, body("maca.png", 3)).andExpect(status().isCreated());

        create(key, body("outra.png", 3))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("idempotency_conflict"));
    }

    @Test
    void neverLetsTheTeacherCrossTheSchoolBoundary() throws Exception {
        create("upload-key-00000003", body("maca.png", 3), "school_norte")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("institutional_access_denied"));

        JsonNode session = mapper.readTree(create("upload-key-00000004", body("maca.png", 3))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        upload(session.get("mediaId").asText(), new byte[] {1, 2, 3},
                        MediaType.IMAGE_PNG, "school_norte")
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsTraversalSizeAndMediaTypeMismatchBeforeApproval() throws Exception {
        create("upload-key-00000005", body("../maca.png", 3))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_file_name"));

        JsonNode session = mapper.readTree(create("upload-key-00000006", body("maca.png", 2))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        String mediaId = session.get("mediaId").asText();
        upload(mediaId, new byte[] {1, 2, 3}, MediaType.IMAGE_PNG)
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("media_too_large"));
        upload(mediaId, new byte[] {1, 2}, MediaType.IMAGE_JPEG)
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void rejectsAnExpiredUploadSessionAndPersistsItsTechnicalState() throws Exception {
        JsonNode session = mapper.readTree(create("upload-key-00000007", body("maca.png", 3))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        String mediaId = session.get("mediaId").asText();
        jdbc.update("update media_upload_session set expires_at = ? where media_id = ?",
                Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")), mediaId);

        upload(mediaId, new byte[] {1, 2, 3}, MediaType.IMAGE_PNG)
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("media_upload_expired"));
        assertThat(jdbc.queryForObject(
                "select status from media_upload_session where media_id = ?",
                String.class, mediaId)).isEqualTo("EXPIRED");
    }

    private org.springframework.test.web.servlet.ResultActions create(String key, String body)
            throws Exception {
        return create(key, body, "school_centro");
    }

    private org.springframework.test.web.servlet.ResultActions create(
            String key, String body, String schoolId) throws Exception {
        return mvc.perform(post("/api/v2/media/uploads")
                .with(jwt().jwt(token -> token
                        .subject("oidc|teacher")
                        .audience(List.of("interpretaai-api"))))
                .header("X-School-Id", schoolId)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private org.springframework.test.web.servlet.ResultActions upload(
            String mediaId, byte[] content, MediaType type) throws Exception {
        return upload(mediaId, content, type, "school_centro");
    }

    private org.springframework.test.web.servlet.ResultActions upload(
            String mediaId, byte[] content, MediaType type, String schoolId) throws Exception {
        return mvc.perform(put("/api/v2/media/uploads/{mediaId}/content", mediaId)
                .with(jwt().jwt(token -> token
                        .subject("oidc|teacher")
                        .audience(List.of("interpretaai-api"))))
                .header("X-School-Id", schoolId)
                .contentType(type)
                .content(content));
    }

    private String body(String fileName, long bytes) {
        return """
                {"fileName":"%s","mediaType":"image/png","bytes":%d,"purpose":"STORY_SOURCE"}
                """.formatted(fileName, bytes);
    }

    private void school(String schoolId, Instant now) {
        jdbc.update("""
                insert into institution_school(school_id, tenant_id, name, status, created_at)
                values (?, 'tenant_rio', ?, 'ACTIVE', ?)
                """, schoolId, schoolId, Timestamp.from(now));
    }
}
