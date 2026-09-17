package br.gov.interpretaai.server.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gov.interpretaai.server.api.DevicePairingModels.RedeemPairingRequest;
import br.gov.interpretaai.server.device.DevicePairingService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        "interpretaai.device-pairing.enabled=true",
        "interpretaai.device-pairing.secret=test-device-pairing-secret-with-more-than-32-characters",
        "interpretaai.device-pairing.max-attempts-per-minute=100",
        "interpretaai.conversation.provider=gemini",
        "interpretaai.gemini.api-key=",
        "interpretaai.speech.provider=kokoro",
        "interpretaai.kokoro.base-url=http://127.0.0.1:1",
        "spring.datasource.url=jdbc:h2:mem:delivery;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class DeliveryControllerTest {
    private static final Instant NOW = Instant.parse("2026-09-17T14:00:00Z");
    private static final String STORY_ID = "historia_maca";
    private static final String HASH = "fead1dca31fc1fb4c7829b7d6c259dcb016ac06979815db9d175c2aedba87982";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired DevicePairingService pairing;
    @MockitoBean JwtDecoder jwtDecoder;

    @BeforeEach
    void seedPublishedStoryAndClassroom() {
        jdbc.update("delete from story_assignment");
        jdbc.update("delete from story_version_transition");
        jdbc.update("delete from story_version");
        jdbc.update("delete from institution_audit_event");
        jdbc.update("delete from institution_device");
        jdbc.update("delete from device_pairing_code");
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
        classroom("class_2b", "school_norte");
        user("user_author", "oidc|author");
        membership("user_author", "school_centro", "TEACHER");
        link("user_author", "class_1a");
        published(STORY_ID, 1, "school_centro", "user_author", HASH, 20);
    }

    @Test
    void assignsOnlyAPublishedStoryAndThePairedTabletGetsAnIncrementalPrivateManifest() throws Exception {
        JsonNode assignment = mapper.readTree(create("assignment-key-000001", 80)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.target.type").value("CLASSROOM"))
                .andExpect(jsonPath("$.target.id").value("class_1a"))
                .andExpect(jsonPath("$.priority").value(80))
                .andReturn().getResponse().getContentAsString());
        String assignmentId = assignment.get("assignmentId").asText();
        var device = paired("installation-tablet-0001");

        JsonNode manifest = mapper.readTree(deviceGet(device.deviceId(), device.deviceToken(), "manifest")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].assignmentId").value(assignmentId))
                .andExpect(jsonPath("$.items[0].storyId").value(STORY_ID))
                .andExpect(jsonPath("$.items[0].storyVersion").value(1))
                .andExpect(jsonPath("$.items[0].minAppVersion").value(20))
                .andExpect(jsonPath("$.items[0].packSha256").value(HASH))
                .andExpect(jsonPath("$.items[0].bytes").isNumber())
                .andReturn().getResponse().getContentAsString());
        String cursor = manifest.get("nextCursor").asText();
        assertThat(cursor).startsWith("d1.");

        deviceGet(device.deviceId(), device.deviceToken(), "assignments/" + assignmentId + "/pack")
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"" + HASH + "\""))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.title").value("A maçã do lanche"));
        mvc.perform(get("/api/v2/devices/{deviceId}/manifest", device.deviceId())
                .param("after", cursor)
                .header("Authorization", "Bearer " + device.deviceToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.nextCursor").value(cursor));
        assertThat(jdbc.queryForObject("""
                select count(*) from institution_audit_event
                 where action = 'STORY_ASSIGNED_TO_CLASSROOM' and target_id = ?
                """, Integer.class, assignmentId)).isEqualTo(1);
    }

    @Test
    void rejectsDraftsUnsupportedTargetsAndCrossClassroomDelivery() throws Exception {
        create("assignment-key-000002", 50).andExpect(status().isCreated());
        var own = paired("installation-tablet-0002");
        var foreign = pairForOtherSchool();
        deviceGet(foreign.deviceId(), foreign.deviceToken(), "manifest")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
        deviceGet(foreign.deviceId(), own.deviceToken(), "manifest")
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v2/assignments")
                .with(adult())
                .header("X-School-Id", "school_centro")
                .header("Idempotency-Key", "assignment-key-group-00001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"storyId":"historia_maca","storyVersion":1,
                         "target":{"type":"GROUP","id":"group_um"},
                         "availableFrom":"2020-01-01T00:00:00Z"}
                        """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("assignment_target_unavailable"));

        published("historia_rascunho", 1, "school_centro", "user_author", "e".repeat(64), 20, "DRAFT");
        mvc.perform(post("/api/v2/assignments")
                .with(adult())
                .header("X-School-Id", "school_centro")
                .header("Idempotency-Key", "assignment-key-draft-00001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"storyId":"historia_rascunho","storyVersion":1,
                         "target":{"type":"CLASSROOM","id":"class_1a"},
                         "availableFrom":"2020-01-01T00:00:00Z"}
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("story_not_published"));
    }

    @Test
    void assignmentCreationIsIdempotentAndRejectsDifferentPayloadForTheSameKey() throws Exception {
        JsonNode first = mapper.readTree(create("assignment-key-000003", 50)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        create("assignment-key-000003", 50)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignmentId").value(first.get("assignmentId").asText()));
        create("assignment-key-000003", 51)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("idempotency_conflict"));
        assertThat(jdbc.queryForObject("select count(*) from story_assignment", Integer.class)).isEqualTo(1);
    }

    @Test
    void blocksCorruptedPackBytesBeforeTheyAppearInTheManifest() throws Exception {
        create("assignment-key-000004", 50).andExpect(status().isCreated());
        var device = paired("installation-tablet-0004");
        jdbc.update("""
                update story_version set pack_json = '{"title":"conteudo alterado"}'
                 where story_id = ? and version = 1
                """, STORY_ID);

        deviceGet(device.deviceId(), device.deviceToken(), "manifest")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("delivery_pack_integrity_invalid"));
    }

    private org.springframework.test.web.servlet.ResultActions create(String key, int priority) throws Exception {
        return mvc.perform(post("/api/v2/assignments")
                .with(adult())
                .header("X-School-Id", "school_centro")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"storyId":"historia_maca","storyVersion":1,
                         "target":{"type":"CLASSROOM","id":"class_1a"},
                         "availableFrom":"2020-01-01T00:00:00Z","priority":%d}
                        """.formatted(priority)));
    }

    private org.springframework.test.web.servlet.ResultActions deviceGet(
            String deviceId, String token, String suffix) throws Exception {
        return mvc.perform(get("/api/v2/devices/{deviceId}/{suffix}", deviceId, suffix)
                .header("Authorization", "Bearer " + token));
    }

    private DevicePairingModels.DeviceCredential paired(String installationId) {
        var code = pairing.createCode("oidc|author", "school_centro", "class_1a");
        return pairing.redeem(new RedeemPairingRequest(
                code.code(), installationId, 21, "ARM64", 800, 1280));
    }

    private DevicePairingModels.DeviceCredential pairForOtherSchool() {
        user("user_other", "oidc|other");
        membership("user_other", "school_norte", "TEACHER");
        link("user_other", "class_2b");
        var code = pairing.createCode("oidc|other", "school_norte", "class_2b");
        return pairing.redeem(new RedeemPairingRequest(
                code.code(), "installation-tablet-other-01", 21, "ARM64", 800, 1280));
    }

    private org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adult() {
        return jwt().jwt(token -> token.subject("oidc|author").audience(List.of("interpretaai-api")));
    }

    private void published(String storyId, int version, String schoolId, String userId, String hash, int minApp) {
        published(storyId, version, schoolId, userId, hash, minApp, "PUBLISHED");
    }

    private void published(
            String storyId, int version, String schoolId, String userId, String hash, int minApp, String state) {
        boolean published = "PUBLISHED".equals(state);
        jdbc.update("""
                insert into story_version
                (story_id, version, school_id, author_user_id, pack_json, pack_sha256, min_app_version,
                 state, revision, approved_by_user_id, approved_at, published_by_user_id, published_at,
                 created_at, updated_at)
                values (?, ?, ?, ?, '{"title":"A maçã do lanche"}', ?, ?, ?, 1, ?, ?, ?, ?, ?, ?)
                """, storyId, version, schoolId, userId, hash, minApp, state,
                published ? userId : null, published ? Timestamp.from(NOW) : null,
                published ? userId : null, published ? Timestamp.from(NOW) : null,
                Timestamp.from(NOW), Timestamp.from(NOW));
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
}
