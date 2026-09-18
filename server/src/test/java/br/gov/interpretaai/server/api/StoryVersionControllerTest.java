package br.gov.interpretaai.server.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gov.interpretaai.server.media.PrivateObjectStore;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
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
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "interpretaai.identity.oidc-enabled=true",
        "interpretaai.identity.issuer-uri=https://identity.test.example",
        "interpretaai.identity.audience=interpretaai-api",
        "interpretaai.conversation.provider=gemini",
        "interpretaai.gemini.api-key=",
        "interpretaai.speech.provider=kokoro",
        "interpretaai.kokoro.base-url=http://127.0.0.1:1",
        "spring.datasource.url=jdbc:h2:mem:story-versions;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class StoryVersionControllerTest {
    private static final String STORY_ID = "historia_maca";
    private static final String HASH = sha256(draftPack(STORY_ID));
    private static final Instant NOW = Instant.parse("2026-09-17T12:00:00Z");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean PrivateObjectStore objects;

    @BeforeEach
    void seedInstitutionAndDraft() {
        jdbc.update("delete from story_version_transition");
        jdbc.update("delete from story_version_asset");
        jdbc.update("delete from story_version");
        jdbc.update("delete from media_sanitization_job");
        jdbc.update("delete from media_upload_session");
        jdbc.update("delete from institution_audit_event");
        jdbc.update("delete from institution_teacher_classroom");
        jdbc.update("delete from institution_school_membership");
        jdbc.update("delete from institution_classroom");
        jdbc.update("delete from institution_adult_user");
        jdbc.update("delete from institution_school");
        jdbc.update("delete from institution_tenant");

        jdbc.update("""
                insert into institution_tenant(tenant_id, name, status, created_at)
                values ('tenant_rio', 'Rede Rio', 'ACTIVE', ?)
                """, Timestamp.from(NOW));
        school("school_centro");
        school("school_norte");
        classroom("class_1a", "school_centro");
        user("user_author", "oidc|author");
        user("user_other", "oidc|other");
        user("user_coord", "oidc|coord");
        membership("user_author", "school_centro", "TEACHER");
        membership("user_other", "school_centro", "TEACHER");
        membership("user_coord", "school_centro", "COORDINATOR");
        link("user_author", "class_1a");
        link("user_other", "class_1a");
        draft(STORY_ID, 1, "school_centro", "user_author");
    }

    @Test
    void onlyTheAuthorCanApproveAndPublishAReviewableVersionWithIdempotency() throws Exception {
        publish("oidc|author", "school_centro", STORY_ID, 1, "publish-before-approve-0001")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("story_transition_invalid"));

        approve("oidc|other", "school_centro", STORY_ID, 1, 1,
                "approve-other-author-0001")
                .andExpect(status().isForbidden());
        approve("oidc|author", "school_centro", STORY_ID, 1, 99,
                "approve-stale-version-0001")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("story_revision_conflict"));

        approve("oidc|author", "school_centro", STORY_ID, 1, 1,
                "approve-story-version-0001")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("APPROVED"))
                .andExpect(jsonPath("$.packSha256").exists());
        approve("oidc|author", "school_centro", STORY_ID, 1, 1,
                "approve-story-version-0001")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("APPROVED"));
        assertThat(auditCount("STORY_VERSION_APPROVED")).isEqualTo(1);

        publish("oidc|author", "school_centro", STORY_ID, 1, "publish-story-version-0001")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PUBLISHED"));
        publish("oidc|author", "school_centro", STORY_ID, 1, "publish-story-version-0001")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PUBLISHED"));
        assertThat(auditCount("STORY_VERSION_PUBLISHED")).isEqualTo(1);
        var published = jdbc.queryForMap("""
                select state, revision, approved_by_user_id, published_by_user_id,
                       pack_json, pack_sha256
                  from story_version where story_id = ? and version = 1
                """, STORY_ID);
        assertThat(published).containsEntry("STATE", "PUBLISHED")
                .containsEntry("REVISION", 3L)
                .containsEntry("APPROVED_BY_USER_ID", "user_author")
                .containsEntry("PUBLISHED_BY_USER_ID", "user_author");
        assertThat(published.get("PACK_JSON").toString()).contains("\"approvedBy\":\"user_author\"");
        assertThat(published.get("PACK_SHA256")).isEqualTo(sha256(published.get("PACK_JSON").toString()));
        assertThat(published.get("PACK_SHA256")).isNotEqualTo(HASH);
    }

    @Test
    void coordinatorCanReviewWithinTheSchoolButNotCrossSchoolOrConflictingKeys() throws Exception {
        approve("oidc|coord", "school_centro", STORY_ID, 1, 1,
                "approve-coordinator-version-0001")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("APPROVED"));
        publish("oidc|coord", "school_centro", STORY_ID, 1, "publish-coordinator-version-0001")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PUBLISHED"));

        draft("historia_outra", 1, "school_centro", "user_author");
        approve("oidc|author", "school_norte", "historia_outra", 1, 1,
                "approve-cross-school-version-0001")
                .andExpect(status().isForbidden());
        approve("oidc|author", "school_centro", "historia_outra", 1, 1,
                "approve-key-conflict-version-01")
                .andExpect(status().isOk());
        approve("oidc|author", "school_centro", "historia_outra", 1, 2,
                "approve-key-conflict-version-01")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("idempotency_conflict"));
    }

    @Test
    void rejectsWarningsThatCannotExistAndNeverChangesPackBytes() throws Exception {
        mvc.perform(post("/api/v2/stories/{storyId}/versions/{version}/approve", STORY_ID, 1)
                .with(user("oidc|author"))
                .header("X-School-Id", "school_centro")
                .header("Idempotency-Key", "approve-warning-version-0001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedRevision\":1,\"expectedPackSha256\":\"%s\",\"confirmedAssets\":[],\"confirmedWarningIds\":[\"warning_one\"]}".formatted(HASH)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("story_warning_unknown"));
        assertThat(jdbc.queryForObject("""
                select pack_sha256 from story_version where story_id = ? and version = ?
                """, String.class, STORY_ID, 1)).isEqualTo(HASH);
    }

    @Test
    void reviewIsPrivateAndApprovalIsBoundToTheExactDraftHash() throws Exception {
        mvc.perform(get("/api/v2/stories/{storyId}/versions/{version}/review", STORY_ID, 1)
                .with(user("oidc|author"))
                .header("X-School-Id", "school_centro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("DRAFT"))
                .andExpect(jsonPath("$.revision").value(1))
                .andExpect(jsonPath("$.packSha256").value(HASH))
                .andExpect(jsonPath("$.packJson").isString());
        mvc.perform(get("/api/v2/stories/{storyId}/versions/{version}/review", STORY_ID, 1)
                .with(user("oidc|other"))
                .header("X-School-Id", "school_centro"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v2/stories/{storyId}/versions/{version}/review/assets/{assetId}/{role}",
                        STORY_ID, 1, "missing_asset", "PHONE")
                .with(user("oidc|other"))
                .header("X-School-Id", "school_centro"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v2/stories/{storyId}/versions/{version}/review/assets/{assetId}/{role}",
                        STORY_ID, 1, "missing_asset", "PHONE")
                .with(user("oidc|author"))
                .header("X-School-Id", "school_centro"))
                .andExpect(status().isNotFound());

        jdbc.update("""
                insert into media_upload_session
                (media_id, school_id, owner_user_id, idempotency_key, request_fingerprint,
                 original_file_name, media_type, declared_bytes, status, object_key,
                 actual_bytes, sha256, expires_at, created_at, updated_at)
                values ('media_story_001','school_centro','user_author','media-story-key-0001',?,
                        'maca.png','image/png',8,'UPLOADED','raw/maca.png',8,?, ?, ?, ?)
                """, "c".repeat(64), "a".repeat(64), Timestamp.from(NOW.plusSeconds(900)),
                Timestamp.from(NOW), Timestamp.from(NOW));
        jdbc.update("""
                insert into story_version_asset
                (story_id, story_version, asset_id, role, media_id, object_key, media_type,
                 bytes, sha256, created_at)
                values (?,1,'maca_objeto','PHONE','media_story_001',
                        'sanitized/school_centro/maca.png','image/png',8,?,?)
                """, STORY_ID, "a".repeat(64), Timestamp.from(NOW));
        given(objects.open("sanitized/school_centro/maca.png"))
                .willAnswer(ignored -> new ByteArrayInputStream("maca-png".getBytes(StandardCharsets.UTF_8)));
        MvcResult pending = mvc.perform(get(
                        "/api/v2/stories/{storyId}/versions/{version}/review/assets/{assetId}/{role}",
                        STORY_ID, 1, "maca_objeto", "PHONE")
                .with(user("oidc|author"))
                .header("X-School-Id", "school_centro"))
                .andExpect(request().asyncStarted())
                .andReturn();
        mvc.perform(asyncDispatch(pending))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v2/stories/{storyId}/versions/{version}/approve", STORY_ID, 1)
                .with(user("oidc|author"))
                .header("X-School-Id", "school_centro")
                .header("Idempotency-Key", "approve-stale-hash-0001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedRevision\":1,\"expectedPackSha256\":\"%s\",\"confirmedAssets\":[],\"confirmedWarningIds\":[]}".formatted("a".repeat(64))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("story_review_stale"));
        assertThat(jdbc.queryForMap("""
                select state, pack_json, pack_sha256 from story_version
                 where story_id = ? and version = 1
                """, STORY_ID)).containsEntry("STATE", "DRAFT")
                .containsEntry("PACK_SHA256", HASH)
                .containsEntry("PACK_JSON", draftPack(STORY_ID));
    }

    private org.springframework.test.web.servlet.ResultActions approve(
            String subject, String schoolId, String storyId, int version, long revision, String key)
            throws Exception {
        return mvc.perform(post("/api/v2/stories/{storyId}/versions/{version}/approve", storyId, version)
                .with(user(subject))
                .header("X-School-Id", schoolId)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedRevision\":%d,\"expectedPackSha256\":\"%s\",\"confirmedAssets\":[],\"confirmedWarningIds\":[]}".formatted(
                        revision, sha256(draftPack(storyId)))));
    }

    private org.springframework.test.web.servlet.ResultActions publish(
            String subject, String schoolId, String storyId, int version, String key) throws Exception {
        return mvc.perform(post("/api/v2/stories/{storyId}/versions/{version}/publish", storyId, version)
                .with(user(subject))
                .header("X-School-Id", schoolId)
                .header("Idempotency-Key", key));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor user(
            String subject) {
        return jwt().jwt(token -> token.subject(subject).audience(List.of("interpretaai-api")));
    }

    private int auditCount(String action) {
        return jdbc.queryForObject("select count(*) from institution_audit_event where action = ?",
                Integer.class, action);
    }

    private void school(String schoolId) {
        jdbc.update("""
                insert into institution_school(school_id, tenant_id, name, status, created_at)
                values (?, 'tenant_rio', ?, 'ACTIVE', ?)
                """, schoolId, schoolId, Timestamp.from(NOW));
    }

    private void classroom(String classroomId, String schoolId) {
        jdbc.update("""
                insert into institution_classroom(classroom_id, school_id, name, status, created_at)
                values (?, ?, ?, 'ACTIVE', ?)
                """, classroomId, schoolId, classroomId, Timestamp.from(NOW));
    }

    private void user(String userId, String subject) {
        jdbc.update("""
                insert into institution_adult_user(user_id, oidc_subject, status, created_at)
                values (?, ?, 'ACTIVE', ?)
                """, userId, subject, Timestamp.from(NOW));
    }

    private void membership(String userId, String schoolId, String role) {
        jdbc.update("""
                insert into institution_school_membership
                (user_id, school_id, role, status, created_at, updated_at)
                values (?, ?, ?, 'ACTIVE', ?, ?)
                """, userId, schoolId, role, Timestamp.from(NOW), Timestamp.from(NOW));
    }

    private void link(String userId, String classroomId) {
        jdbc.update("""
                insert into institution_teacher_classroom
                (user_id, classroom_id, status, created_at, updated_at)
                values (?, ?, 'ACTIVE', ?, ?)
                """, userId, classroomId, Timestamp.from(NOW), Timestamp.from(NOW));
    }

    private void draft(String storyId, int version, String schoolId, String authorUserId) {
        String pack = draftPack(storyId);
        jdbc.update("""
                insert into story_version
                (story_id, version, school_id, author_user_id, pack_json, pack_sha256,
                 state, revision, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, 'DRAFT', 1, ?, ?)
                """, storyId, version, schoolId, authorUserId, pack, sha256(pack),
                Timestamp.from(NOW), Timestamp.from(NOW));
    }

    private static String draftPack(String storyId) {
        return """
                {"schemaVersion":"1.0","packId":"pack_%s","storyId":"%s",
                 "version":1,"minAppVersion":1,"title":"Conversa da turma","methodology":"LEIA",
                 "objectiveIds":["explicar_ideia"],"startNodeId":"conversa_001",
                 "nodes":[{"id":"conversa_001","type":"GROUP_HANDOFF",
                   "objectiveIds":["explicar_ideia"],"instruction":"Conte sua ideia à dupla.",
                   "nextNodeId":"fim_001"},
                  {"id":"fim_001","type":"END","objectiveIds":["explicar_ideia"],
                   "closingSpeech":"Vocês terminaram a conversa."}],
                 "assets":[],"accessibility":{"minTouchTargetDp":48,
                   "reducedStimuliSupported":true,"spokenInstructions":true,"noRequiredScroll":true},
                 "provenance":{"createdBy":"TEACHER","sourceRefs":[],"assetOrigins":[]}}
                """.formatted(storyId, storyId);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
