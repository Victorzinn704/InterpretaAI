package br.gov.interpretaai.server.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.gov.interpretaai.server.authoring.AuthoringJobQueueStore;
import br.gov.interpretaai.server.authoring.AuthoringJobWorker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "interpretaai.identity.oidc-enabled=true",
        "interpretaai.identity.issuer-uri=https://identity.test.example",
        "interpretaai.identity.audience=interpretaai-api",
        "interpretaai.conversation.provider=gemini",
        "interpretaai.gemini.api-key=",
        "interpretaai.speech.provider=kokoro",
        "interpretaai.kokoro.base-url=http://127.0.0.1:1",
        "spring.datasource.url=jdbc:h2:mem:authoring-jobs;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
class AuthoringJobControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @Autowired AuthoringJobQueueStore queue;
    @MockitoBean JwtDecoder jwtDecoder;

    @BeforeEach
    void seedInstitutionAndMedia() {
        jdbc.update("delete from authoring_job_queue");
        jdbc.update("delete from authoring_job");
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
        school("school_centro", now);
        school("school_norte", now);
        user("user_teacher", "oidc|teacher", now);
        user("user_other", "oidc|other", now);
        membership("user_teacher", "school_centro", now);
        membership("user_other", "school_centro", now);
        media("media_ready", "school_centro", "user_teacher", "READY", now);
        media("media_waiting", "school_centro", "user_teacher", "QUEUED", now);
        media("media_other", "school_norte", "user_teacher", "READY", now);
        media("media_colleague", "school_centro", "user_other", "READY", now);
    }

    @Test
    void createsIdempotentPersistentJobAndReturnsConditionalState() throws Exception {
        String key = "authoring-key-000001";
        var created = create(key, body("media_ready"), "oidc|teacher", "school_centro")
                .andExpect(status().isAccepted())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.progress.totalSteps").value(5))
                .andReturn();
        JsonNode response = mapper.readTree(created.getResponse().getContentAsString());
        String jobId = response.get("jobId").asText();
        String etag = created.getResponse().getHeader(HttpHeaders.ETAG);

        create(key, body("media_ready"), "oidc|teacher", "school_centro")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(jobId));
        assertThat(jdbc.queryForObject(
                "select count(*) from authoring_job where job_id = ?", Integer.class, jobId))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "select status from authoring_job_queue where job_id = ?", String.class, jobId))
                .isEqualTo("QUEUED");
        assertThat(jdbc.queryForObject("""
                select count(*) from institution_audit_event
                 where target_id = ? and action = 'AUTHORING_JOB_CREATED'
                """, Integer.class, jobId)).isEqualTo(1);

        retrieve(jobId, "oidc|teacher", "school_centro", etag)
                .andExpect(status().isNotModified());
        retrieve(jobId, "oidc|other", "school_centro", null)
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsIdempotencyConflictUnreadyMediaAndCrossSchoolSource() throws Exception {
        String key = "authoring-key-000002";
        create(key, body("media_ready"), "oidc|teacher", "school_centro")
                .andExpect(status().isAccepted());
        create(key, bodyWithDuration("media_ready", 9), "oidc|teacher", "school_centro")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("idempotency_conflict"));

        create("authoring-key-000003", body("media_waiting"),
                "oidc|teacher", "school_centro")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("authoring_media_not_ready"));
        create("authoring-key-000004", body("media_other"),
                "oidc|teacher", "school_centro")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("authoring_media_not_ready"));
        create("authoring-key-000004a", body("media_colleague"),
                "oidc|teacher", "school_centro")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("authoring_media_not_ready"));
        create("authoring-key-000005", body("media_ready"),
                "oidc|teacher", "school_norte")
                .andExpect(status().isForbidden());
    }

    @Test
    void anotherWorkerReclaimsThePersistedJobAfterLeaseExpiry() throws Exception {
        JsonNode created = mapper.readTree(create(
                "authoring-key-000006", body("media_ready"),
                "oidc|teacher", "school_centro")
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString());
        String jobId = created.get("jobId").asText();
        Instant firstClaimAt = Instant.now().plusSeconds(1);
        var first = queue.claimNext(firstClaimAt, Duration.ofSeconds(30)).orElseThrow();
        assertThat(first.jobId()).isEqualTo(jobId);
        assertThat(first.attempts()).isEqualTo(1);

        var restartedWorker = new AuthoringJobQueueStore(jdbc);
        assertThat(restartedWorker.claimNext(
                firstClaimAt.plusSeconds(29), Duration.ofSeconds(30))).isEmpty();
        var reclaimed = restartedWorker.claimNext(
                firstClaimAt.plusSeconds(31), Duration.ofSeconds(30)).orElseThrow();
        assertThat(reclaimed.jobId()).isEqualTo(jobId);
        assertThat(reclaimed.attempts()).isEqualTo(2);

        restartedWorker.markDelivered(jobId, firstClaimAt.plusSeconds(32));
        assertThat(jdbc.queryForMap("""
                select status, progress_step, completed_steps, revision
                  from authoring_job where job_id = ?
                """, jobId)).containsEntry("STATUS", "RETRIEVING_GUIDANCE")
                .containsEntry("COMPLETED_STEPS", 1);
        assertThat(jdbc.queryForObject(
                "select status from authoring_job_queue where job_id = ?",
                String.class, jobId)).isEqualTo("DELIVERED");
    }

    @Test
    void boundsRetriesAndExposesOnlyASafeFinalFailure() throws Exception {
        JsonNode created = mapper.readTree(create(
                "authoring-key-000009", body("media_ready"),
                "oidc|teacher", "school_centro")
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString());
        String jobId = created.get("jobId").asText();
        Instant now = Instant.now().plusSeconds(1);

        var first = queue.claimNext(now, Duration.ofSeconds(30)).orElseThrow();
        queue.retryOrFail(first, "provider_internal_detail", now.plusSeconds(1));
        assertThat(jdbc.queryForObject(
                "select status from authoring_job where job_id = ?", String.class, jobId))
                .isEqualTo("FAILED_RETRYABLE");

        var second = queue.claimNext(now.plusSeconds(4), Duration.ofSeconds(30)).orElseThrow();
        queue.retryOrFail(second, "provider_internal_detail", now.plusSeconds(5));
        var third = queue.claimNext(now.plusSeconds(10), Duration.ofSeconds(30)).orElseThrow();
        queue.retryOrFail(third, "provider_internal_detail", now.plusSeconds(11));

        var finalState = jdbc.queryForMap("""
                select status, failure_code, failure_safe_message
                  from authoring_job where job_id = ?
                """, jobId);
        assertThat(finalState).containsEntry("STATUS", "FAILED_FINAL")
                .containsEntry("FAILURE_CODE", "provider_internal_detail")
                .containsEntry("FAILURE_SAFE_MESSAGE",
                        "Não foi possível preparar o rascunho. Tente criar uma nova solicitação.");
        assertThat(jdbc.queryForObject(
                "select status from authoring_job_queue where job_id = ?",
                String.class, jobId)).isEqualTo("DEAD");
        retrieve(jobId, "oidc|teacher", "school_centro", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.failure.code").value("authoring_preparation_failed"))
                .andExpect(jsonPath("$.failure.retryable").value(false))
                .andExpect(jsonPath("$.failure.safeMessage")
                        .value("Não foi possível preparar o rascunho. Tente criar uma nova solicitação."));
    }

    @Test
    void isolatedWorkerValidatesThePersistedPayloadBeforeHandoff() throws Exception {
        JsonNode created = mapper.readTree(create(
                "authoring-key-000010", body("media_ready"),
                "oidc|teacher", "school_centro")
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString());
        String jobId = created.get("jobId").asText();
        var worker = new AuthoringJobWorker(queue, mapper, Clock.systemUTC());

        assertThat(worker.processOne()).isTrue();
        assertThat(jdbc.queryForObject(
                "select status from authoring_job where job_id = ?", String.class, jobId))
                .isEqualTo("RETRIEVING_GUIDANCE");
        assertThat(jdbc.queryForObject(
                "select status from authoring_job_queue where job_id = ?", String.class, jobId))
                .isEqualTo("DELIVERED");
    }

    @Test
    void isolatedWorkerNeverHandsOffACorruptPersistedPayload() throws Exception {
        JsonNode created = mapper.readTree(create(
                "authoring-key-000011", body("media_ready"),
                "oidc|teacher", "school_centro")
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString());
        String jobId = created.get("jobId").asText();
        jdbc.update("update authoring_job set request_json = '{\"unexpected\":true}' where job_id = ?",
                jobId);

        assertThat(new AuthoringJobWorker(queue, mapper, Clock.systemUTC()).processOne()).isTrue();
        assertThat(jdbc.queryForMap("""
                select status, failure_code from authoring_job where job_id = ?
                """, jobId)).containsEntry("STATUS", "FAILED_RETRYABLE")
                .containsEntry("FAILURE_CODE", "authoring_payload_invalid");
        assertThat(jdbc.queryForObject(
                "select status from authoring_job_queue where job_id = ?", String.class, jobId))
                .isEqualTo("RETRYABLE");
    }

    @Test
    void rejectsDuplicateObjectivesAndInvalidSourceShape() throws Exception {
        create("authoring-key-000007", body("media_ready")
                        .replace("[\"reconhecer_maca\"]",
                                "[\"reconhecer_maca\",\"reconhecer_maca\"]"),
                "oidc|teacher", "school_centro")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("authoring_request_has_duplicates"));
        create("authoring-key-000008", body("media_ready")
                        .replace("\"mediaId\":\"media_ready\"",
                                "\"mediaId\":\"media_ready\",\"theme\":\"bola\""),
                "oidc|teacher", "school_centro")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_authoring_source"));
    }

    private org.springframework.test.web.servlet.ResultActions create(
            String key, String body, String subject, String schoolId) throws Exception {
        return mvc.perform(post("/api/v2/authoring/jobs")
                .with(jwt().jwt(token -> token.subject(subject)
                        .audience(List.of("interpretaai-api"))))
                .header("X-School-Id", schoolId)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private org.springframework.test.web.servlet.ResultActions retrieve(
            String jobId, String subject, String schoolId, String etag) throws Exception {
        var request = get("/api/v2/authoring/jobs/{jobId}", jobId)
                .with(jwt().jwt(token -> token.subject(subject)
                        .audience(List.of("interpretaai-api"))))
                .header("X-School-Id", schoolId);
        if (etag != null) request.header(HttpHeaders.IF_NONE_MATCH, etag);
        return mvc.perform(request);
    }

    private String body(String mediaId) {
        return bodyWithDuration(mediaId, 8);
    }

    private String bodyWithDuration(String mediaId, int duration) {
        return """
                {
                  "objectiveIds":["reconhecer_maca"],
                  "yearRange":"1_YEAR",
                  "durationMinutes":%d,
                  "participationMode":"PAIR",
                  "source":{"type":"TEACHER_UPLOAD","mediaId":"%s"},
                  "confirmedWord":"MAÇÃ",
                  "requestedComponents":["COMIC","PUZZLE","WORD_BUILDER"]
                }
                """.formatted(duration, mediaId);
    }

    private void school(String schoolId, Instant now) {
        jdbc.update("""
                insert into institution_school(school_id, tenant_id, name, status, created_at)
                values (?, 'tenant_rio', ?, 'ACTIVE', ?)
                """, schoolId, schoolId, Timestamp.from(now));
    }

    private void user(String userId, String subject, Instant now) {
        jdbc.update("""
                insert into institution_adult_user(user_id, oidc_subject, status, created_at)
                values (?, ?, 'ACTIVE', ?)
                """, userId, subject, Timestamp.from(now));
    }

    private void membership(String userId, String schoolId, Instant now) {
        jdbc.update("""
                insert into institution_school_membership
                (user_id, school_id, role, status, created_at, updated_at)
                values (?, ?, 'TEACHER', 'ACTIVE', ?, ?)
                """, userId, schoolId, Timestamp.from(now), Timestamp.from(now));
    }

    private void media(
            String mediaId, String schoolId, String owner, String status, Instant now) {
        jdbc.update("""
                insert into media_upload_session
                (media_id, school_id, owner_user_id, idempotency_key, request_fingerprint,
                 original_file_name, media_type, declared_bytes, status, object_key,
                 actual_bytes, sha256, expires_at, created_at, updated_at)
                values (?, ?, ?, ?, 'fingerprint', 'maca.png', 'image/png', 3,
                        'UPLOADED', ?, 3, ?, ?, ?, ?)
                """, mediaId, schoolId, owner, "upload-" + mediaId,
                "raw/" + schoolId + "/" + mediaId, "a".repeat(64),
                Timestamp.from(now.plusSeconds(600)), Timestamp.from(now), Timestamp.from(now));
        jdbc.update("""
                insert into media_sanitization_job
                (media_id, status, attempts, available_at, sanitized_object_key,
                 sanitized_sha256, width, height, output_media_type, created_at, updated_at)
                values (?, ?, 1, ?, ?, ?, 100, 100, 'image/png', ?, ?)
                """, mediaId, status, Timestamp.from(now),
                "sanitized/" + schoolId + "/" + mediaId, "b".repeat(64),
                Timestamp.from(now), Timestamp.from(now));
    }
}
