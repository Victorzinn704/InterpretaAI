package br.gov.interpretaai.server.authoring;

import static org.assertj.core.api.Assertions.assertThat;

import br.gov.interpretaai.server.api.AuthoringJobModels.Component;
import br.gov.interpretaai.server.api.AuthoringJobModels.CreateAuthoringJobRequest;
import br.gov.interpretaai.server.api.AuthoringJobModels.ParticipationMode;
import br.gov.interpretaai.server.api.AuthoringJobModels.Source;
import br.gov.interpretaai.server.api.AuthoringJobModels.SourceType;
import br.gov.interpretaai.server.api.AuthoringJobModels.YearRange;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.model.ollama.OllamaChatModel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Opt-in contract smoke against a local model. Uses synthetic pedagogical data only. */
class AuthoringPlanOllamaSmokeTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_AUTHORING_MODEL_SMOKE", matches = "true")
    void modelProducesAValidGroundedTeacherPlan() throws Exception {
        String modelName = System.getenv().getOrDefault("AUTHORING_SMOKE_MODEL", "qwen2.5:1.5b");
        byte[] content = """
                Exemplo sintético para teste: a maçã aparece no lanche. A criança observa
                uma folha na cesta, diz o que percebe e conversa com a dupla antes de formar
                a palavra. Valorize a explicação, sem classificar a criança.
                """.getBytes(StandardCharsets.UTF_8);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        String manifest = """
                {"schemaVersion":"1.0","generatedAt":"2026-09-01T12:00:00Z","sources":[{
                "sourceId":"smoke_source_001","title":"Maçã do lanche","owner":"Teste sintético",
                "curatorRole":"teste","sourceVersion":"v1","collection":"methodology",
                "scope":"GLOBAL","licenseBasis":"conteudo_sintetico_de_teste",
                "yearRange":["2_YEAR"],"objectiveIds":["interpretar_pista"],
                "reviewStatus":"APPROVED","approvedBy":"teste","approvedAt":"2026-09-01T12:00:00Z",
                "validFrom":"2026-09-01","contentPath":"source.md","contentSha256":"%s"}]}
                """.formatted(hash);
        var resources = Map.of("manifest.json", manifest.getBytes(StandardCharsets.UTF_8),
                "source.md", content);
        var mapper = new ObjectMapper();
        var guidance = new GuidanceSourceCatalog(mapper,
                Clock.fixed(Instant.parse("2026-09-17T12:00:00Z"), ZoneOffset.UTC), resources::get);
        var model = OllamaChatModel.builder()
                .baseUrl("http://127.0.0.1:11434")
                .modelName(modelName)
                .temperature(0.2)
                .think(false)
                .numPredict(600)
                .timeout(Duration.ofSeconds(60))
                .maxRetries(0)
                .logRequests(false)
                .logResponses(false)
                .build();
        var lastOutput = new AtomicReference<String>();
        var planner = new AuthoringPlanService(guidance, mapper, request -> {
            String output = model.chat(request).aiMessage().text();
            lastOutput.set(output);
            return output;
        });
        var input = new CreateAuthoringJobRequest(List.of("interpretar_pista"), YearRange.YEAR_2,
                8, ParticipationMode.PAIR, new Source(SourceType.THEME, null, null, "lanche"),
                "MAÇÃ", null, List.of(Component.COMIC, Component.PUZZLE,
                Component.WORD_BUILDER, Component.GROUP_HANDOFF), false, null);

        int iterations = Integer.parseInt(System.getenv().getOrDefault(
                "AUTHORING_SMOKE_ITERATIONS", "3"));
        assertThat(iterations).isBetween(1, 10);
        var durations = new ArrayList<Long>();
        var rejections = new ArrayList<String>();
        for (int index = 0; index < iterations; index++) {
            long started = System.nanoTime();
            try {
                var plan = planner.plan(input, "school_test", "teacher_test");
                assertThat(plan.confirmedWord()).isEqualTo("MAÇÃ");
                assertThat(plan.references()).singleElement().satisfies(reference ->
                        assertThat(reference.sourceId()).isEqualTo("smoke_source_001"));
            } catch (AuthoringPlanContract.Rejected rejected) {
                rejections.add(rejected.code() + ":" + fieldLengths(mapper, lastOutput.get()));
            } finally {
                durations.add(Duration.ofNanos(System.nanoTime() - started).toMillis());
            }
        }
        durations.sort(Long::compareTo);
        System.out.printf("authoring_model_smoke model=%s accepted=%d/%d p50_ms=%d p95_ms=%d rejections=%s%n",
                modelName, iterations - rejections.size(), iterations,
                durations.get(durations.size() / 2), durations.get(durations.size() - 1), rejections);
        assertThat(rejections).isEmpty();
    }

    private static String fieldLengths(ObjectMapper mapper, String raw) {
        if (raw == null) return "no_output";
        try {
            JsonNode root = mapper.readTree(raw);
            var lengths = new ArrayList<String>();
            for (String field : List.of("title", "context", "clue", "inferenceQuestion",
                    "confirmedWord", "groupInstruction")) {
                JsonNode value = root.path(field);
                lengths.add(field + "=" + (value.isTextual() ? value.asText().length() : "not_text"));
            }
            return String.join(",", lengths);
        } catch (Exception invalid) {
            return "unparseable";
        }
    }
}
