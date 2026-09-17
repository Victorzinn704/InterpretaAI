package br.gov.interpretaai.server.authoring;

import static org.assertj.core.api.Assertions.assertThat;

import br.gov.interpretaai.server.api.AuthoringJobModels.Component;
import br.gov.interpretaai.server.api.AuthoringJobModels.CreateAuthoringJobRequest;
import br.gov.interpretaai.server.api.AuthoringJobModels.ParticipationMode;
import br.gov.interpretaai.server.api.AuthoringJobModels.Source;
import br.gov.interpretaai.server.api.AuthoringJobModels.SourceType;
import br.gov.interpretaai.server.api.AuthoringJobModels.YearRange;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:authoring-plan-worker;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "interpretaai.authoring.worker-enabled=false",
        "interpretaai.authoring.plan-worker-enabled=false"
})
class AuthoringPlanWorkerTest {
    private static final Instant NOW = Instant.parse("2026-09-17T12:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final byte[] CONTENT = "A maçã do lanche é uma pista para conversar."
            .getBytes(StandardCharsets.UTF_8);

    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @Autowired AuthoringJobStore jobs;
    @Autowired AuthoringJobQueueStore firstQueue;
    @Autowired AuthoringPlanQueueStore planQueue;

    @BeforeEach
    void reset() {
        jdbc.update("delete from authoring_plan_queue");
        jdbc.update("delete from authoring_job_queue");
        jdbc.update("delete from authoring_job");
        jdbc.update("delete from institution_adult_user");
        jdbc.update("delete from institution_school");
        jdbc.update("delete from institution_tenant");
        jdbc.update("""
                insert into institution_tenant(tenant_id, name, status, created_at)
                values ('network_a', 'Rede', 'ACTIVE', ?)
                """, Timestamp.from(NOW));
        jdbc.update("""
                insert into institution_school(school_id, tenant_id, name, status, created_at)
                values ('school_a', 'network_a', 'Escola', 'ACTIVE', ?)
                """, Timestamp.from(NOW));
        jdbc.update("""
                insert into institution_adult_user(user_id, oidc_subject, status, created_at)
                values ('teacher_a', 'oidc|teacher_a', 'ACTIVE', ?)
                """, Timestamp.from(NOW));
    }

    @Test
    void approvedGuidanceProducesPersistedPlanButNoPublishedStory() throws Exception {
        seedJob();
        var planner = new AuthoringPlanService(catalog("APPROVED"), mapper, request -> {
            assertThat(request.messages().toString()).contains("source_001", "MAÇÃ")
                    .doesNotContain("consent_secret_001", "nota privada");
            return validPlan();
        });

        assertThat(new AuthoringPlanWorker(planQueue, planner, mapper, CLOCK).processOne()).isTrue();

        var stored = jdbc.queryForMap("""
                select status, plan_json, plan_sha256 from authoring_plan_queue where job_id = ?
                """, "job_plan_001");
        assertThat(stored.get("STATUS")).isEqualTo("DELIVERED");
        assertThat(stored.get("PLAN_SHA256")).isEqualTo(sha256(
                stored.get("PLAN_JSON").toString().getBytes(StandardCharsets.UTF_8)));
        assertThat(mapper.readTree(stored.get("PLAN_JSON").toString()).path("title").asText())
                .isEqualTo("A maçã da LÉIA");
        assertThat(jdbc.queryForObject("select status from authoring_job where job_id = ?",
                String.class, "job_plan_001")).isEqualTo("GENERATING_STORY");
        assertThat(jdbc.queryForObject("select count(*) from story_version",
                Integer.class)).isZero();
    }

    @Test
    void candidateSourcePausesWithoutCallingProviderOrCreatingPlan() throws Exception {
        seedJob();
        var called = new AtomicBoolean(false);
        var planner = new AuthoringPlanService(catalog("CANDIDATE"), mapper, request -> {
            called.set(true);
            return validPlan();
        });

        assertThat(new AuthoringPlanWorker(planQueue, planner, mapper, CLOCK).processOne()).isTrue();

        assertThat(called).isFalse();
        assertThat(jdbc.queryForMap("""
                select status, failure_code from authoring_job where job_id = ?
                """, "job_plan_001"))
                .containsEntry("STATUS", "NEEDS_TEACHER_INPUT")
                .containsEntry("FAILURE_CODE", "guidance_not_approved");
        assertThat(jdbc.queryForObject("select status from authoring_plan_queue where job_id = ?",
                String.class, "job_plan_001")).isEqualTo("PAUSED");
        assertThat(new AuthoringPlanWorker(planQueue, planner, mapper, CLOCK).processOne()).isFalse();
    }

    @Test
    void staleLeaseCannotOverwriteAReclaimedPlan() throws Exception {
        seedJob();
        var first = planQueue.claimNext(NOW, Duration.ofSeconds(10)).orElseThrow();
        var second = planQueue.claimNext(NOW.plusSeconds(11), Duration.ofSeconds(10)).orElseThrow();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> planQueue.complete(
                first, "{}", NOW.plusSeconds(12)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("authoring_plan_lease_lost");
        planQueue.complete(second, "{}", NOW.plusSeconds(12));
        assertThat(jdbc.queryForObject("select attempts from authoring_plan_queue where job_id = ?",
                Integer.class, "job_plan_001")).isEqualTo(2);
    }

    @Test
    void repeatedPlannerFailureUsesDurableBackoffThenStops() throws Exception {
        seedJob();
        var first = planQueue.claimNext(NOW, Duration.ofSeconds(30)).orElseThrow();
        planQueue.retryOrFail(first, "authoring_plan_unavailable", NOW.plusSeconds(1));
        assertThat(jdbc.queryForObject("select status from authoring_job where job_id = ?",
                String.class, "job_plan_001")).isEqualTo("FAILED_RETRYABLE");

        var second = planQueue.claimNext(NOW.plusSeconds(4), Duration.ofSeconds(30)).orElseThrow();
        planQueue.retryOrFail(second, "authoring_plan_unavailable", NOW.plusSeconds(5));
        var third = planQueue.claimNext(NOW.plusSeconds(10), Duration.ofSeconds(30)).orElseThrow();
        planQueue.retryOrFail(third, "authoring_plan_unavailable", NOW.plusSeconds(11));

        assertThat(jdbc.queryForMap("""
                select status, failure_code from authoring_job where job_id = ?
                """, "job_plan_001"))
                .containsEntry("STATUS", "FAILED_FINAL")
                .containsEntry("FAILURE_CODE", "authoring_plan_unavailable");
        assertThat(jdbc.queryForObject("select status from authoring_plan_queue where job_id = ?",
                String.class, "job_plan_001")).isEqualTo("DEAD");
        assertThat(planQueue.claimNext(NOW.plusSeconds(100), Duration.ofSeconds(30))).isEmpty();
    }

    private void seedJob() throws Exception {
        var request = new CreateAuthoringJobRequest(List.of("interpretar_pista"), YearRange.YEAR_2,
                8, ParticipationMode.PAIR,
                new Source(SourceType.THEME, null, null, "lanche"), "MAÇÃ", "nota privada",
                List.of(Component.COMIC), false, "consent_secret_001");
        jobs.insertWithQueue(new AuthoringJobStore.Job("job_plan_001", "school_a", "teacher_a",
                "authoring-key-000001", "a".repeat(64), mapper.writeValueAsString(request),
                "THEME", "lanche", "QUEUED", "QUEUED", 0, 5, 1,
                null, null, NOW, NOW));
        var claim = firstQueue.claimNext(NOW, Duration.ofSeconds(45)).orElseThrow();
        firstQueue.markDelivered(claim, NOW);
    }

    private GuidanceSourceCatalog catalog(String status) {
        String approval = "APPROVED".equals(status) ? """
                ,"approvedBy":"curador_001","approvedAt":"2026-09-01T12:00:00Z",
                "validFrom":"2026-09-01"
                """ : "";
        String manifest = """
                {"schemaVersion":"1.0","generatedAt":"2026-09-01T12:00:00Z","sources":[{
                "sourceId":"source_001","title":"Pista da maçã","owner":"InterpretaAI",
                "curatorRole":"pedagogia","sourceVersion":"v1","collection":"methodology",
                "scope":"GLOBAL","licenseBasis":"autorizacao_interna_documentada",
                "yearRange":["2_YEAR"],"objectiveIds":["interpretar_pista"],
                "reviewStatus":"%s","contentPath":"source.md","contentSha256":"%s"%s}]}
                """.formatted(status, sha256(CONTENT), approval);
        Map<String, byte[]> resources = Map.of("manifest.json",
                manifest.getBytes(StandardCharsets.UTF_8), "source.md", CONTENT);
        return new GuidanceSourceCatalog(mapper, CLOCK, resources::get);
    }

    private static String validPlan() {
        return """
                {"title":"A maçã da LÉIA","context":"LÉIA procura uma fruta no lanche.",
                "clue":"Uma folha aparece perto da cesta.",
                "inferenceQuestion":"Que fruta pode estar perto da folha?",
                "confirmedWord":"MAÇÃ","groupInstruction":"Converse com a dupla sobre a pista.",
                "components":["COMIC"],"sourceIds":["source_001"]}
                """;
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception impossible) {
            throw new AssertionError(impossible);
        }
    }
}
