package br.gov.interpretaai.server.story;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "interpretaai.conversation.provider=gemini",
        "interpretaai.gemini.api-key=",
        "interpretaai.speech.provider=kokoro",
        "interpretaai.kokoro.base-url=http://127.0.0.1:1",
        "spring.datasource.url=jdbc:h2:mem:story-materialization;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
class StoryVersionMaterializationTest {
    private static final Instant NOW = Instant.parse("2026-09-17T13:00:00Z");

    @Autowired StoryVersionService stories;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seedReadyAuthoringJob() {
        jdbc.update("delete from story_version_transition");
        jdbc.update("delete from story_version");
        jdbc.update("delete from authoring_job_queue");
        jdbc.update("delete from authoring_job");
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
        jdbc.update("""
                insert into institution_school(school_id, tenant_id, name, status, created_at)
                values ('school_centro', 'tenant_rio', 'Centro', 'ACTIVE', ?)
                """, Timestamp.from(NOW));
        jdbc.update("""
                insert into institution_adult_user(user_id, oidc_subject, status, created_at)
                values ('user_author', 'oidc|author', 'ACTIVE', ?)
                """, Timestamp.from(NOW));
        jdbc.update("""
                insert into authoring_job
                (job_id, school_id, requested_by_user_id, idempotency_key, request_fingerprint,
                 request_json, source_type, source_ref, status, progress_step, completed_steps,
                 total_steps, revision, created_at, updated_at)
                values ('job_story_001', 'school_centro', 'user_author',
                        'materialization-job-key-0001', ?, '{}', 'THEME', 'maçã',
                        'VALIDATING', 'VALIDATING', 4, 5, 1, ?, ?)
                """, "c".repeat(64), Timestamp.from(NOW), Timestamp.from(NOW));
    }

    @Test
    void admitsOnlyAValidatedPackAndFreezesItsExactBytesAndHash() throws Exception {
        String pack = example();

        var state = stories.materializeValidatedDraft("job_story_001", pack);

        assertThat(state.storyId()).isEqualTo("historia_lanche_leia");
        assertThat(state.version()).isEqualTo(1);
        assertThat(state.state()).isEqualTo("DRAFT");
        assertThat(state.packSha256()).isEqualTo(sha256(pack));
        assertThat(jdbc.queryForMap("""
                select pack_json, pack_sha256, state, authoring_job_id
                  from story_version where story_id = 'historia_lanche_leia' and version = 1
                """)).containsEntry("PACK_JSON", pack)
                .containsEntry("PACK_SHA256", sha256(pack))
                .containsEntry("STATE", "DRAFT")
                .containsEntry("AUTHORING_JOB_ID", "job_story_001");
        assertThat(jdbc.queryForObject("""
                select count(*) from institution_audit_event
                 where action = 'STORY_VERSION_CREATED' and target_id = 'historia_lanche_leia@1'
                """, Integer.class)).isEqualTo(1);

        assertThatThrownBy(() -> stories.materializeValidatedDraft("job_story_001", pack))
                .isInstanceOf(StoryVersionException.class)
                .extracting("code").isEqualTo("story_version_conflict");
    }

    @Test
    void rejectsInvalidDataBeforeItCanBecomeADraft() throws Exception {
        String invalid = example().replace("\"noRequiredScroll\": true", "\"noRequiredScroll\": false");

        assertThatThrownBy(() -> stories.materializeValidatedDraft("job_story_001", invalid))
                .isInstanceOf(StoryVersionException.class)
                .extracting("code").isEqualTo("story_contract_invalid");
        assertThat(jdbc.queryForObject("select count(*) from story_version", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from institution_audit_event", Integer.class)).isZero();
    }

    @Test
    void refusesToMaterializeBeforeTheAuthoringWorkflowReachesValidation() throws Exception {
        jdbc.update("update authoring_job set status = 'RETRIEVING_GUIDANCE' where job_id = 'job_story_001'");

        assertThatThrownBy(() -> stories.materializeValidatedDraft("job_story_001", example()))
                .isInstanceOf(StoryVersionException.class)
                .extracting("code").isEqualTo("authoring_job_not_ready");
        assertThat(jdbc.queryForObject("select count(*) from story_version", Integer.class)).isZero();
    }

    private String example() throws Exception {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path root = Files.exists(current.resolve("docs")) ? current : current.getParent();
        return Files.readString(root.resolve("docs/v2/contracts/example-apple-story-pack.json"));
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
