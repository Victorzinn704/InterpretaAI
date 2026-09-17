package br.gov.interpretaai.server.authoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Small, deterministic retrieval layer for approved pedagogical guidance. It deliberately starts
 * without vectors: provenance and scope are hard filters, while lexical score only orders evidence.
 */
@Component
public class GuidanceSourceCatalog {
    private static final Pattern SAFE_PATH = Pattern.compile("[a-zA-Z0-9_-]+\\.md");
    private static final Pattern TERM = Pattern.compile("[a-z0-9]{3,}");
    private static final int MAX_PASSAGE_CHARS = 1_200;

    public record Query(
            String text,
            Set<String> objectiveIds,
            String yearRange,
            Set<String> collections,
            Set<String> allowedScopeIds,
            int limit) {
        public Query {
            objectiveIds = objectiveIds == null ? Set.of() : Set.copyOf(objectiveIds);
            collections = collections == null ? Set.of() : Set.copyOf(collections);
            allowedScopeIds = allowedScopeIds == null ? Set.of() : Set.copyOf(allowedScopeIds);
            limit = Math.max(1, Math.min(limit, 8));
        }
    }

    public record Evidence(
            String sourceId,
            String sourceVersion,
            String title,
            String collection,
            String contentSha256,
            String passage,
            int lexicalScore) {}

    @FunctionalInterface
    interface ResourceReader {
        byte[] read(String path) throws IOException;
    }

    private record SourceDocument(
            String sourceId,
            String sourceVersion,
            String title,
            String collection,
            String scope,
            String scopeId,
            Set<String> yearRange,
            Set<String> objectiveIds,
            String reviewStatus,
            LocalDate validFrom,
            LocalDate validUntil,
            String contentSha256,
            String content) {}

    private final Clock clock;
    private final List<SourceDocument> sources;

    @Autowired
    public GuidanceSourceCatalog(ObjectMapper mapper, Clock clock) {
        this(mapper, clock, GuidanceSourceCatalog::readClasspath);
    }

    GuidanceSourceCatalog(ObjectMapper mapper, Clock clock, ResourceReader resources) {
        this.clock = clock;
        this.sources = load(mapper, resources);
    }

    public List<Evidence> retrieve(Query query) {
        LocalDate today = LocalDate.now(clock);
        Set<String> terms = terms(query.text());
        return sources.stream()
                .filter(source -> "APPROVED".equals(source.reviewStatus()))
                .filter(source -> !today.isBefore(source.validFrom()))
                .filter(source -> source.validUntil() == null || !today.isAfter(source.validUntil()))
                .filter(source -> query.yearRange() == null
                        || source.yearRange().contains(query.yearRange())
                        || source.yearRange().contains("MIXED"))
                .filter(source -> query.objectiveIds().isEmpty()
                        || source.objectiveIds().stream().anyMatch(query.objectiveIds()::contains))
                .filter(source -> query.collections().isEmpty()
                        || query.collections().contains(source.collection()))
                .filter(source -> "GLOBAL".equals(source.scope())
                        || query.allowedScopeIds().contains(source.scopeId()))
                .map(source -> evidence(source, terms))
                .filter(evidence -> terms.isEmpty() || evidence.lexicalScore() > 0)
                .sorted(Comparator.comparingInt(Evidence::lexicalScore).reversed()
                        .thenComparing(Evidence::sourceId))
                .limit(query.limit())
                .toList();
    }

    public int approvedSourceCount() {
        return (int) sources.stream().filter(source -> "APPROVED".equals(source.reviewStatus())).count();
    }

    private static Evidence evidence(SourceDocument source, Set<String> terms) {
        String searchable = normalize(source.title() + " " + source.content());
        int score = terms.stream().mapToInt(term -> occurrences(searchable, term)).sum();
        String passage = source.content().length() <= MAX_PASSAGE_CHARS
                ? source.content()
                : source.content().substring(0, MAX_PASSAGE_CHARS) + "…";
        return new Evidence(source.sourceId(), source.sourceVersion(), source.title(),
                source.collection(), source.contentSha256(), passage, score);
    }

    private static List<SourceDocument> load(ObjectMapper mapper, ResourceReader resources) {
        try {
            JsonNode manifest = mapper.readTree(resources.read("manifest.json"));
            require("1.0".equals(text(manifest, "schemaVersion")), "guidance_schema_invalid");
            OffsetDateTime.parse(text(manifest, "generatedAt"));
            JsonNode entries = manifest.path("sources");
            require(entries.isArray() && !entries.isEmpty(), "guidance_sources_missing");
            var ids = new HashSet<String>();
            var loaded = new ArrayList<SourceDocument>();
            for (JsonNode entry : entries) {
                String sourceId = text(entry, "sourceId");
                require(ids.add(sourceId), "guidance_source_duplicate");
                String path = text(entry, "contentPath");
                require(SAFE_PATH.matcher(path).matches(), "guidance_path_invalid");
                byte[] bytes = resources.read(path);
                String expectedHash = text(entry, "contentSha256");
                require(expectedHash.matches("[a-f0-9]{64}") && expectedHash.equals(sha256(bytes)),
                        "guidance_hash_mismatch");
                String status = text(entry, "reviewStatus");
                require(Set.of("CANDIDATE", "APPROVED", "SUSPENDED", "EXPIRED").contains(status),
                        "guidance_review_status_invalid");
                LocalDate validFrom = null;
                LocalDate validUntil = optionalDate(entry, "validUntil");
                if ("APPROVED".equals(status)) {
                    require(present(entry, "approvedBy") && present(entry, "approvedAt")
                            && present(entry, "validFrom"), "guidance_approval_incomplete");
                    OffsetDateTime.parse(text(entry, "approvedAt"));
                    validFrom = LocalDate.parse(text(entry, "validFrom"));
                    require(!text(entry, "licenseBasis").toLowerCase(Locale.ROOT).contains("pendente"),
                            "guidance_license_pending");
                }
                String scope = text(entry, "scope");
                String scopeId = optionalText(entry, "scopeId");
                require("GLOBAL".equals(scope) || scopeId != null, "guidance_scope_id_missing");
                loaded.add(new SourceDocument(sourceId, text(entry, "sourceVersion"),
                        text(entry, "title"), text(entry, "collection"), scope, scopeId,
                        strings(entry.path("yearRange")), strings(entry.path("objectiveIds")),
                        status, validFrom, validUntil, expectedHash,
                        new String(bytes, StandardCharsets.UTF_8)));
            }
            return List.copyOf(loaded);
        } catch (IOException | RuntimeException failure) {
            if (failure instanceof IllegalStateException invalid) throw invalid;
            throw new IllegalStateException("guidance_catalog_invalid", failure);
        }
    }

    private static Set<String> strings(JsonNode values) {
        require(values.isArray() && !values.isEmpty(), "guidance_filter_missing");
        var result = new HashSet<String>();
        values.forEach(value -> result.add(value.asText()));
        require(result.size() == values.size(), "guidance_filter_duplicate");
        return Set.copyOf(result);
    }

    private static Set<String> terms(String value) {
        var result = new HashSet<String>();
        var matcher = TERM.matcher(normalize(value == null ? "" : value));
        while (matcher.find()) result.add(matcher.group());
        return result;
    }

    private static int occurrences(String value, String term) {
        int count = 0;
        for (int at = value.indexOf(term); at >= 0; at = value.indexOf(term, at + term.length())) count++;
        return count;
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }

    private static String text(JsonNode node, String field) {
        String value = optionalText(node, field);
        require(value != null, "guidance_field_missing_" + field);
        return value;
    }

    private static String optionalText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || !value.isTextual() || value.asText().isBlank() ? null : value.asText();
    }

    private static LocalDate optionalDate(JsonNode node, String field) {
        String value = optionalText(node, field);
        return value == null ? null : LocalDate.parse(value);
    }

    private static boolean present(JsonNode node, String field) {
        return optionalText(node, field) != null;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static byte[] readClasspath(String path) throws IOException {
        try (var input = new ClassPathResource("guidance/" + path).getInputStream()) {
            return input.readAllBytes();
        }
    }

    private static void require(boolean condition, String code) {
        if (!condition) throw new IllegalStateException(code);
    }
}
