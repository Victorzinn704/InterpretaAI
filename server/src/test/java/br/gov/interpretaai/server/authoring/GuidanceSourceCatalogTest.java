package br.gov.interpretaai.server.authoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GuidanceSourceCatalogTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-09-17T12:00:00Z"), ZoneOffset.UTC);
    private static final byte[] CONTENT = """
            # Orientação revisada

            Convide a criança a interpretar a pista e explicar sua ideia para a dupla.
            """.getBytes(StandardCharsets.UTF_8);

    @Test
    void retrievesOnlyApprovedIntegrityCheckedEvidenceWithProvenance() {
        var catalog = catalog(manifest("APPROVED", sha256(CONTENT), "autorizacao_interna_documentada"), CONTENT);

        var found = catalog.retrieve(new GuidanceSourceCatalog.Query(
                "interpretar a pista", Set.of("interpretar_pista"), "2_YEAR",
                Set.of("methodology"), Map.of(), 3));

        assertThat(catalog.approvedSourceCount()).isEqualTo(1);
        assertThat(found).singleElement().satisfies(evidence -> {
            assertThat(evidence.sourceId()).isEqualTo("metodo_teste");
            assertThat(evidence.sourceVersion()).isEqualTo("versao_1");
            assertThat(evidence.contentSha256()).isEqualTo(sha256(CONTENT));
            assertThat(evidence.passage()).contains("explicar sua ideia");
            assertThat(evidence.lexicalScore()).isPositive();
        });
    }

    @Test
    void candidateContentNeverEntersRetrieval() {
        var catalog = catalog(manifest("CANDIDATE", sha256(CONTENT),
                "conteudo_proprio_pendente_de_aprovacao"), CONTENT);

        assertThat(catalog.approvedSourceCount()).isZero();
        assertThat(catalog.retrieve(new GuidanceSourceCatalog.Query(
                "interpretar", Set.of(), "2_YEAR", Set.of(), Map.of(), 3))).isEmpty();
    }

    @Test
    void rejectsTamperingAndAnApprovedSourceWithPendingLicense() {
        assertThatThrownBy(() -> catalog(manifest("APPROVED", "a".repeat(64),
                "autorizacao_interna_documentada"), CONTENT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("guidance_hash_mismatch");
        assertThatThrownBy(() -> catalog(manifest("APPROVED", sha256(CONTENT),
                "permissao_pendente"), CONTENT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("guidance_license_pending");
    }

    private GuidanceSourceCatalog catalog(String manifest, byte[] content) {
        Map<String, byte[]> resources = Map.of(
                "manifest.json", manifest.getBytes(StandardCharsets.UTF_8),
                "source.md", content);
        return new GuidanceSourceCatalog(new ObjectMapper(), CLOCK, path -> resources.get(path));
    }

    private String manifest(String status, String hash, String license) {
        String approval = "APPROVED".equals(status) ? """
                ,"approvedBy":"curador_001","approvedAt":"2026-09-01T12:00:00Z",
                 "validFrom":"2026-09-01"
                """ : "";
        return """
                {"schemaVersion":"1.0","generatedAt":"2026-09-01T12:00:00Z","sources":[{
                  "sourceId":"metodo_teste","title":"Método de teste","owner":"InterpretaAI",
                  "curatorRole":"pedagogia","sourceVersion":"versao_1","collection":"methodology",
                  "scope":"GLOBAL","licenseBasis":"%s","yearRange":["2_YEAR"],
                  "objectiveIds":["interpretar_pista"],"reviewStatus":"%s",
                  "contentPath":"source.md","contentSha256":"%s"%s}]}
                """.formatted(license, status, hash, approval).replace("\n", "");
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception impossible) {
            throw new AssertionError(impossible);
        }
    }
}
