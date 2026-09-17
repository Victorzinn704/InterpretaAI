package br.gov.interpretaai.server.authoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gov.interpretaai.server.api.AuthoringJobModels.Component;
import br.gov.interpretaai.server.api.AuthoringJobModels.CreateAuthoringJobRequest;
import br.gov.interpretaai.server.api.AuthoringJobModels.ParticipationMode;
import br.gov.interpretaai.server.api.AuthoringJobModels.Source;
import br.gov.interpretaai.server.api.AuthoringJobModels.SourceType;
import br.gov.interpretaai.server.api.AuthoringJobModels.YearRange;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class AuthoringPlanServiceTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-17T12:00:00Z"), ZoneOffset.UTC);
    private static final byte[] CONTENT = """
            A maçã aparece na história. A criança investiga uma pista e conta sua ideia à dupla.
            """.getBytes(StandardCharsets.UTF_8);

    @Test
    void approvedEvidenceProducesOnlyATeacherDraftWithTraceableSources() {
        var service = new AuthoringPlanService(catalog("APPROVED", "SCHOOL", "school_a"), JSON,
                request -> {
                    assertThat(request.responseFormat().jsonSchema().rootElement())
                            .isInstanceOf(JsonObjectSchema.class);
                    assertThat(request.messages().toString()).contains("source_001", "maçã");
                    return validPlan();
                });

        var plan = service.plan(input(), "school_a", "teacher_a");

        assertThat(plan.confirmedWord()).isEqualTo("MAÇÃ");
        assertThat(plan.components()).containsExactly(Component.COMIC, Component.PUZZLE,
                Component.WORD_BUILDER, Component.GROUP_HANDOFF);
        assertThat(plan.references()).singleElement().satisfies(reference -> {
            assertThat(reference.sourceId()).isEqualTo("source_001");
            assertThat(reference.sourceVersion()).isEqualTo("versao_1");
            assertThat(reference.contentSha256()).isEqualTo(sha256(CONTENT));
        });
    }

    @Test
    void modelPayloadDoesNotContainMediaIdentifiersConsentOrUnreviewedNotes() {
        var input = new CreateAuthoringJobRequest(List.of("interpretar_pista"), YearRange.YEAR_2, 8,
                ParticipationMode.PAIR,
                new Source(SourceType.TEACHER_UPLOAD, "media_secret_001", null, null),
                "MAÇÃ", "Nome da criança: segredo", List.of(Component.COMIC),
                false, "consent_secret_001");
        var evidence = catalog("APPROVED", "GLOBAL", null).retrieve(
                new GuidanceSourceCatalog.Query("maçã", java.util.Set.of("interpretar_pista"),
                        "2_YEAR", java.util.Set.of(), Map.of(), 5));

        String serialized = AuthoringPlanContract.request(JSON, input, evidence).messages().toString();

        assertThat(serialized).contains("interpretar_pista", "MAÇÃ", "source_001")
                .doesNotContain("media_secret_001", "consent_secret_001", "segredo");
    }

    @Test
    void candidateAndOtherSchoolEvidenceNeverCallTheModel() {
        var called = new AtomicBoolean(false);
        AuthoringPlanService.Provider provider = request -> {
            called.set(true);
            return validPlan();
        };
        for (var catalog : List.of(catalog("CANDIDATE", "SCHOOL", "school_a"),
                catalog("APPROVED", "SCHOOL", "school_b"),
                catalog("APPROVED", "TEACHER", "school_a"))) {
            assertThatThrownBy(() -> new AuthoringPlanService(catalog, JSON, provider)
                    .plan(input(), "school_a", "teacher_a"))
                    .isInstanceOf(AuthoringPlanService.NoApprovedGuidance.class);
        }
        assertThat(called).isFalse();
    }

    @Test
    void rejectsInventedCitationChangedWordOrUnrequestedMechanic() {
        var evidence = catalog("APPROVED", "GLOBAL", null).retrieve(
                new GuidanceSourceCatalog.Query("maçã", java.util.Set.of("interpretar_pista"),
                        "2_YEAR", java.util.Set.of(), Map.of(), 5));
        assertThatThrownBy(() -> AuthoringPlanContract.decode(JSON,
                validPlan().replace("source_001", "fonte_inventada"), input(), evidence))
                .isInstanceOf(AuthoringPlanContract.Rejected.class)
                .extracting("code").isEqualTo("plan_source_unknown");
        assertThatThrownBy(() -> AuthoringPlanContract.decode(JSON,
                validPlan().replace("MAÇÃ", "BOLA"), input(), evidence))
                .isInstanceOf(AuthoringPlanContract.Rejected.class)
                .extracting("code").isEqualTo("plan_didactic_mismatch");
        assertThatThrownBy(() -> AuthoringPlanContract.decode(JSON,
                validPlan().replace("PUZZLE", "CAMERA"), input(), evidence))
                .isInstanceOf(AuthoringPlanContract.Rejected.class)
                .extracting("code").isEqualTo("plan_components_mismatch");
        assertThatThrownBy(() -> AuthoringPlanContract.decode(JSON,
                validPlan().replace("Converse com a dupla", "Você errou. Converse com a dupla"),
                input(), evidence))
                .isInstanceOf(AuthoringPlanContract.Rejected.class)
                .extracting("code").isEqualTo("plan_unsafe_language");
        assertThatThrownBy(() -> AuthoringPlanContract.decode(JSON,
                validPlan() + "{}", input(), evidence))
                .isInstanceOf(AuthoringPlanContract.Rejected.class)
                .extracting("code").isEqualTo("plan_invalid_json");
        assertThatThrownBy(() -> AuthoringPlanContract.decode(JSON,
                validPlan().replace("\"title\":", "\"published\":true,\"title\":"),
                input(), evidence))
                .isInstanceOf(AuthoringPlanContract.Rejected.class)
                .extracting("code").isEqualTo("plan_invalid_fields");
        assertThatThrownBy(() -> AuthoringPlanContract.decode(JSON,
                validPlan().replace("\"title\":", "\"title\":\"BOLA\",\"title\":"),
                input(), evidence))
                .isInstanceOf(AuthoringPlanContract.Rejected.class)
                .extracting("code").isEqualTo("plan_invalid_json");
    }

    private static CreateAuthoringJobRequest input() {
        return new CreateAuthoringJobRequest(List.of("interpretar_pista"), YearRange.YEAR_2, 8,
                ParticipationMode.PAIR, new Source(SourceType.THEME, null, null, "lanche"),
                "MAÇÃ", null,
                List.of(Component.COMIC, Component.PUZZLE, Component.WORD_BUILDER,
                        Component.GROUP_HANDOFF), false, null);
    }

    private static String validPlan() {
        return """
                {"title":"A maçã da LÉIA","context":"LÉIA procura uma fruta no lanche.",
                "clue":"Uma folha aparece perto da cesta.",
                "inferenceQuestion":"Que fruta pode estar perto da folha?",
                "confirmedWord":"MAÇÃ","groupInstruction":"Converse com a dupla sobre a pista.",
                "components":["COMIC","PUZZLE","WORD_BUILDER","GROUP_HANDOFF"],
                "sourceIds":["source_001"]}
                """;
    }

    private static GuidanceSourceCatalog catalog(String status, String scope, String scopeId) {
        String approval = "APPROVED".equals(status) ? """
                ,"approvedBy":"curador_001","approvedAt":"2026-09-01T12:00:00Z",
                "validFrom":"2026-09-01"
                """ : "";
        String identity = scopeId == null ? "" : ",\"scopeId\":\"" + scopeId + "\"";
        String manifest = """
                {"schemaVersion":"1.0","generatedAt":"2026-09-01T12:00:00Z","sources":[{
                "sourceId":"source_001","title":"Pista da maçã","owner":"InterpretaAI",
                "curatorRole":"pedagogia","sourceVersion":"versao_1","collection":"methodology",
                "scope":"%s"%s,"licenseBasis":"autorizacao_interna_documentada",
                "yearRange":["2_YEAR"],"objectiveIds":["interpretar_pista"],
                "reviewStatus":"%s","contentPath":"source.md","contentSha256":"%s"%s}]}
                """.formatted(scope, identity, status, sha256(CONTENT), approval);
        Map<String, byte[]> resources = Map.of("manifest.json", manifest.getBytes(StandardCharsets.UTF_8),
                "source.md", CONTENT);
        return new GuidanceSourceCatalog(JSON, CLOCK, resources::get);
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (Exception impossible) {
            throw new AssertionError(impossible);
        }
    }
}
