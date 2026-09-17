package br.gov.interpretaai.server.studio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import br.gov.interpretaai.server.media.PrivateObjectStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "interpretaai.studio.enabled=true",
        "interpretaai.identity.oidc-enabled=true",
        "interpretaai.identity.issuer-uri=https://identity.test.example",
        "interpretaai.identity.audience=interpretaai-api",
        "interpretaai.conversation.provider=gemini",
        "interpretaai.gemini.api-key=",
        "interpretaai.speech.provider=kokoro",
        "interpretaai.kokoro.base-url=http://127.0.0.1:1",
        "spring.datasource.url=jdbc:h2:mem:studio-review;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class StudioControllerTest {
    private static final Instant NOW = Instant.parse("2026-09-17T13:00:00Z");
    private static final String PACK = """
            {"schemaVersion":"1.0","packId":"pack_studio_001","storyId":"historia_studio_001",
             "version":1,"minAppVersion":1,"title":"A bola da turma","methodology":"LEIA",
             "objectiveIds":["explicar_ideia"],"startNodeId":"cena_001",
             "nodes":[{"id":"cena_001","type":"GROUP_HANDOFF","objectiveIds":["explicar_ideia"],
               "instruction":"Conte sua ideia à dupla.","nextNodeId":"fim_001"},
              {"id":"fim_001","type":"END","objectiveIds":["explicar_ideia"],
               "closingSpeech":"Vocês terminaram a conversa."}],
             "assets":[],"accessibility":{"minTouchTargetDp":48,"reducedStimuliSupported":true,
               "spokenInstructions":true,"noRequiredScroll":true},
             "provenance":{"createdBy":"TEACHER","sourceRefs":[],"assetOrigins":[]}}
            """;

    @TestConfiguration
    static class LoginFixture {
        @Bean
        ClientRegistrationRepository registrations() {
            return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("studio")
                    .clientId("studio-test-client").clientSecret("local-test-only")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("openid", "profile")
                    .authorizationUri("https://identity.test.example/authorize")
                    .tokenUri("https://identity.test.example/token")
                    .jwkSetUri("https://identity.test.example/jwks")
                    .issuerUri("https://identity.test.example")
                    .userInfoUri("https://identity.test.example/userinfo")
                    .userNameAttributeName("sub")
                    .build());
        }
    }

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean PrivateObjectStore objects;

    @BeforeEach
    void seed() {
        jdbc.update("delete from story_version_asset");
        jdbc.update("delete from story_assignment");
        jdbc.update("delete from story_version_transition");
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
        jdbc.update("insert into institution_tenant(tenant_id,name,status,created_at) values ('tenant_studio','Rede','ACTIVE',?)",
                Timestamp.from(NOW));
        jdbc.update("insert into institution_school(school_id,tenant_id,name,status,created_at) values ('school_studio','tenant_studio','Escola','ACTIVE',?)",
                Timestamp.from(NOW));
        jdbc.update("insert into institution_classroom(classroom_id,school_id,name,status,created_at) values ('class_own','school_studio','Turma Sol','ACTIVE',?)",
                Timestamp.from(NOW));
        jdbc.update("insert into institution_classroom(classroom_id,school_id,name,status,created_at) values ('class_other','school_studio','Turma Lua','ACTIVE',?)",
                Timestamp.from(NOW));
        jdbc.update("insert into institution_adult_user(user_id,oidc_subject,status,created_at) values ('user_author','oidc|author','ACTIVE',?)",
                Timestamp.from(NOW));
        jdbc.update("insert into institution_adult_user(user_id,oidc_subject,status,created_at) values ('user_other','oidc|other','ACTIVE',?)",
                Timestamp.from(NOW));
        jdbc.update("""
                insert into institution_school_membership
                (user_id,school_id,role,status,created_at,updated_at)
                values (?,'school_studio','TEACHER','ACTIVE',?,?)
                """, "user_author", Timestamp.from(NOW), Timestamp.from(NOW));
        jdbc.update("""
                insert into institution_school_membership
                (user_id,school_id,role,status,created_at,updated_at)
                values (?,'school_studio','TEACHER','ACTIVE',?,?)
                """, "user_other", Timestamp.from(NOW), Timestamp.from(NOW));
        jdbc.update("""
                insert into institution_teacher_classroom
                (user_id,classroom_id,status,created_at,updated_at)
                values ('user_author','class_own','ACTIVE',?,?)
                """, Timestamp.from(NOW), Timestamp.from(NOW));
        jdbc.update("""
                insert into institution_teacher_classroom
                (user_id,classroom_id,status,created_at,updated_at)
                values ('user_other','class_other','ACTIVE',?,?)
                """, Timestamp.from(NOW), Timestamp.from(NOW));
        jdbc.update("""
                insert into story_version
                (story_id,version,school_id,author_user_id,pack_json,pack_sha256,state,revision,created_at,updated_at)
                values ('historia_studio_001',1,'school_studio','user_author',?,?,'DRAFT',1,?,?)
                """, PACK, sha256(PACK), Timestamp.from(NOW), Timestamp.from(NOW));
    }

    @Test
    void previewIsPrivateToTheAuthorAndStreamsTheBoundVariant() throws Exception {
        jdbc.update("""
                insert into media_upload_session
                (media_id, school_id, owner_user_id, idempotency_key, request_fingerprint,
                 original_file_name, media_type, declared_bytes, status, object_key,
                 actual_bytes, sha256, expires_at, created_at, updated_at)
                values ('media_studio_001','school_studio','user_author','media-studio-key-0001',?,
                        'bola.png','image/png',9,'UPLOADED','raw/bola.png',9,?, ?, ?, ?)
                """, "c".repeat(64), "a".repeat(64), Timestamp.from(NOW.plusSeconds(900)),
                Timestamp.from(NOW), Timestamp.from(NOW));
        jdbc.update("""
                insert into story_version_asset
                (story_id, story_version, asset_id, role, media_id, object_key, media_type,
                 bytes, sha256, created_at)
                values ('historia_studio_001',1,'bola_objeto','PHONE','media_studio_001',
                        'sanitized/school_studio/bola.png','image/png',9,?,?)
                """, "a".repeat(64), Timestamp.from(NOW));
        given(objects.open("sanitized/school_studio/bola.png"))
                .willAnswer(ignored -> new ByteArrayInputStream("bola-png!".getBytes(StandardCharsets.UTF_8)));
        String base = "/studio/api/schools/school_studio/stories/historia_studio_001/versions/1";
        mvc.perform(get(base + "/review")
                .with(oidcLogin().idToken(token -> token.subject("oidc|author"))))
                .andExpect(jsonPath("$.assets[0].previewUrl")
                        .value(base + "/assets/bola_objeto/PHONE"));
        mvc.perform(get(base + "/assets/bola_objeto/PHONE")
                .with(oidcLogin().idToken(token -> token.subject("oidc|other"))))
                .andExpect(status().isForbidden());
        mvc.perform(get(base + "/assets/bola_objeto/PHONE")
                .with(oidcLogin().idToken(token -> token.subject("oidc|author"))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(content().bytes("bola-png!".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void requiresLoginAndShowsOnlyTheAuthorsReview() throws Exception {
        mvc.perform(get("/studio/").with(oidcLogin().idToken(token -> token.subject("oidc|author"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/studio/index.html"));
        mvc.perform(get("/studio/index.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/studio/index.html")
                .with(oidcLogin().idToken(token -> token.subject("oidc|author"))))
                .andExpect(status().isOk());
        mvc.perform(get("/studio/api/me").with(oidcLogin().idToken(token -> token.subject("oidc|author"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schools[0].schoolId").value("school_studio"))
                .andExpect(jsonPath("$.schools[0].name").value("Escola"));
        mvc.perform(get("/studio/api/schools/school_studio/reviews")
                .with(oidcLogin().idToken(token -> token.subject("oidc|author"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("A bola da turma"));
        mvc.perform(get("/studio/api/schools/school_studio/reviews")
                .with(oidcLogin().idToken(token -> token.subject("oidc|other"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/studio/api/schools/school_studio/stories/historia_studio_001/versions/1/review")
                .with(oidcLogin().idToken(token -> token.subject("oidc|other"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void approvalNeedsCsrfAndTheReviewedHash() throws Exception {
        String path = "/studio/api/schools/school_studio/stories/historia_studio_001/versions/1/approve";
        String payload = "{\"expectedRevision\":1,\"expectedPackSha256\":\"%s\",\"confirmedAssets\":[],\"confirmedWarningIds\":[]}".formatted(sha256(PACK));
        mvc.perform(post(path).with(oidcLogin().idToken(token -> token.subject("oidc|author")))
                .header("Idempotency-Key", "studio-approve-00001")
                .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());
        var csrfResponse = mvc.perform(get("/studio/api/csrf")
                .with(oidcLogin().idToken(token -> token.subject("oidc|author"))))
                .andExpect(status().isOk()).andReturn().getResponse();
        String csrfValue = mapper.readTree(csrfResponse.getContentAsString()).path("token").asText();
        var cookie = csrfResponse.getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        mvc.perform(post(path).with(oidcLogin().idToken(token -> token.subject("oidc|author")))
                .cookie(cookie).header("X-XSRF-TOKEN", csrfValue)
                .header("Idempotency-Key", "studio-approve-00001")
                .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("APPROVED"));
        mvc.perform(post("/studio/api/schools/school_studio/stories/historia_studio_001/versions/1/publish")
                .with(oidcLogin().idToken(token -> token.subject("oidc|author")))
                .cookie(cookie).header("X-XSRF-TOKEN", csrfValue)
                .header("Idempotency-Key", "studio-publish-00001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("PUBLISHED"));
        mvc.perform(get("/studio/api/schools/school_studio/reviews")
                .with(oidcLogin().idToken(token -> token.subject("oidc|author"))))
                .andExpect(jsonPath("$[0].state").value("PUBLISHED"));
        mvc.perform(get("/studio/api/schools/school_studio/classrooms")
                .with(oidcLogin().idToken(token -> token.subject("oidc|author"))))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Turma Sol"));
        String assignmentPath = "/studio/api/schools/school_studio/stories/historia_studio_001/versions/1/assignments";
        String assignmentBody = "{\"classroomId\":\"class_own\",\"availableFrom\":\"2020-01-01T00:00:00Z\"}";
        mvc.perform(post(assignmentPath)
                .with(oidcLogin().idToken(token -> token.subject("oidc|author")))
                .cookie(cookie).header("X-XSRF-TOKEN", csrfValue)
                .header("Idempotency-Key", "studio-assignment-00001")
                .contentType(MediaType.APPLICATION_JSON).content(assignmentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.target.id").value("class_own"));
        mvc.perform(post(assignmentPath)
                .with(oidcLogin().idToken(token -> token.subject("oidc|author")))
                .cookie(cookie).header("X-XSRF-TOKEN", csrfValue)
                .header("Idempotency-Key", "studio-assignment-00001")
                .contentType(MediaType.APPLICATION_JSON).content(assignmentBody))
                .andExpect(status().isOk());
        mvc.perform(get(assignmentPath)
                .with(oidcLogin().idToken(token -> token.subject("oidc|author"))))
                .andExpect(jsonPath("$[0].target.id").value("class_own"));
        mvc.perform(get(assignmentPath)
                .with(oidcLogin().idToken(token -> token.subject("oidc|other"))))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post(assignmentPath)
                .with(oidcLogin().idToken(token -> token.subject("oidc|author")))
                .cookie(cookie).header("X-XSRF-TOKEN", csrfValue)
                .header("Idempotency-Key", "studio-assignment-00002")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"classroomId\":\"class_other\",\"availableFrom\":\"2020-01-01T00:00:00Z\"}"))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("select count(*) from story_assignment", Integer.class)).isEqualTo(1);
        jdbc.update("update institution_teacher_classroom set status = 'REVOKED' where user_id = 'user_author'");
        mvc.perform(get("/studio/api/schools/school_studio/classrooms")
                .with(oidcLogin().idToken(token -> token.subject("oidc|author"))))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get(assignmentPath)
                .with(oidcLogin().idToken(token -> token.subject("oidc|author"))))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post(assignmentPath)
                .with(oidcLogin().idToken(token -> token.subject("oidc|author")))
                .cookie(cookie).header("X-XSRF-TOKEN", csrfValue)
                .header("Idempotency-Key", "studio-assignment-00003")
                .contentType(MediaType.APPLICATION_JSON).content(assignmentBody))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("select pack_json from story_version where story_id = 'historia_studio_001'",
                String.class)).contains("\"approvedBy\":\"user_author\"");
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
