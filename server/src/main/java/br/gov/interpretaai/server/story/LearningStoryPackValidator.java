package br.gov.interpretaai.server.story;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Deterministic admission validator for content produced by the authoring worker.
 *
 * <p>The Android repeats the defensive validation before rendering. This server-side pass exists
 * so invalid data never becomes a reviewable or publishable version in the first place. It does
 * not call a model and does not interpret an asset's pixels; media validation remains a separate
 * worker gate.
 */
@Component
public class LearningStoryPackValidator {
    public record Issue(String code, String path, String message) {}

    public record Result(String storyId, int version, int minAppVersion, List<Issue> issues) {
        public boolean valid() {
            return issues.isEmpty();
        }
    }

    private record Reference(String id, String path, String kind) {}

    private static final int MAX_PACK_CHARS = 262_144;
    private static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9_-]{2,63}");
    private static final Pattern SHA256 = Pattern.compile("[a-f0-9]{64}");
    private static final Pattern SAFE_PATH = Pattern.compile("[a-zA-Z0-9_./-]+");
    private static final Map<String, Integer> SUPPORT_ORDER = Map.of(
            "VOICE_REPEAT", 0, "VISUAL_CUE", 1, "CHOICE_REVEALED", 2);
    private static final List<PatternIssue> BANNED = List.of(
            new PatternIssue("punitive_error", Pattern.compile(
                    "\\b(voc[eê]\\s+errou|resposta\\s+errada|est[aá]\\s+errado)\\b",
                    Pattern.CASE_INSENSITIVE)),
            new PatternIssue("diagnosis", Pattern.compile(
                    "\\b(diagn[oó]stic|transtorno|d[eé]ficit|dislexia)\\w*\\b",
                    Pattern.CASE_INSENSITIVE)),
            new PatternIssue("guilt", Pattern.compile(
                    "\\b(n[aã]o\\s+me\\s+abandone|voc[eê]\\s+est[aá]\\s+demorando|preste\\s+aten[cç][aã]o)\\b",
                    Pattern.CASE_INSENSITIVE)),
            new PatternIssue("grading", Pattern.compile(
                    "\\b(nota|ranking|reprovad[oa])\\b", Pattern.CASE_INSENSITIVE)));

    private record PatternIssue(String code, Pattern pattern) {}

    private final ObjectMapper mapper;

    public LearningStoryPackValidator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Result validate(String rawJson) {
        List<Issue> issues = new ArrayList<>();
        if (rawJson == null || rawJson.length() > MAX_PACK_CHARS) {
            issues.add(issue("pack_too_large", "$", "O pacote excede o limite permitido."));
            return result(null, 0, 0, issues);
        }
        JsonNode pack;
        try {
            pack = mapper.reader()
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readTree(rawJson);
        } catch (Exception invalidJson) {
            issues.add(issue("invalid_structure", "$", "O pacote não possui JSON compatível."));
            return result(null, 0, 0, issues);
        }
        if (pack == null || !pack.isObject()) {
            issues.add(issue("invalid_structure", "$", "O pacote precisa ser um objeto."));
            return result(null, 0, 0, issues);
        }

        exactFields(pack, "$", ROOT_FIELDS, ROOT_REQUIRED, issues);
        String packId = identifier(pack, "packId", "$.packId", issues);
        String storyId = identifier(pack, "storyId", "$.storyId", issues);
        int version = positiveInt(pack, "version", "$.version", issues);
        int minAppVersion = positiveInt(pack, "minAppVersion", "$.minAppVersion", issues);
        exactText(pack, "schemaVersion", "$.schemaVersion", 1, 8, Set.of("1.0"), issues);
        exactText(pack, "methodology", "$.methodology", 1, 16, Set.of("LEIA"), issues);
        text(pack, "title", "$.title", 1, 120, issues);
        if (pack.has("estimatedMinutes")) boundedInt(
                pack.get("estimatedMinutes"), "$.estimatedMinutes", 3, 40, issues);
        Set<String> objectives = identifiers(pack.get("objectiveIds"), "$.objectiveIds", 1, 8, issues);
        String startNodeId = identifier(pack, "startNodeId", "$.startNodeId", issues);

        JsonNode nodesNode = array(pack.get("nodes"), "$.nodes", 2, 24, issues);
        JsonNode assetsNode = array(pack.get("assets"), "$.assets", 0, 100, issues);
        validateAccessibility(pack.get("accessibility"), issues);
        Map<String, Boolean> originReviewed = validateProvenance(pack.get("provenance"), issues);

        Map<String, JsonNode> nodes = new HashMap<>();
        Map<String, JsonNode> assets = new HashMap<>();
        List<Reference> assetReferences = new ArrayList<>();
        List<Reference> visualTargets = new ArrayList<>();
        List<TextValue> textValues = new ArrayList<>();
        textValues.add(new TextValue("$.title", stringValue(pack.get("title"))));

        if (nodesNode != null) {
            for (int index = 0; index < nodesNode.size(); index++) {
                JsonNode node = nodesNode.get(index);
                String path = "$.nodes[" + index + "]";
                String nodeId = validateNode(
                        node, path, objectives, assetReferences, visualTargets, textValues, issues);
                if (nodeId != null && nodes.putIfAbsent(nodeId, node) != null) {
                    issues.add(issue("duplicate_node_id", "$.nodes", "Identificadores de cena devem ser únicos."));
                }
            }
        }
        if (assetsNode != null) {
            for (int index = 0; index < assetsNode.size(); index++) {
                JsonNode asset = assetsNode.get(index);
                String path = "$.assets[" + index + "]";
                String assetId = validateAsset(asset, path, issues);
                if (assetId != null && assets.putIfAbsent(assetId, asset) != null) {
                    issues.add(issue("duplicate_asset_id", "$.assets", "Identificadores de mídia devem ser únicos."));
                }
            }
        }

        if (packId == null) {
            issues.add(issue("invalid_structure", "$.packId", "O identificador do pacote é obrigatório."));
        }
        if (startNodeId == null || !nodes.containsKey(startNodeId)) {
            issues.add(issue("start_not_found", "$.startNodeId", "A cena inicial não existe."));
        }
        validateReferences(nodes, assets, assetReferences, visualTargets, issues);
        validateGraph(nodes, startNodeId, issues);
        validateAssetOrigins(assets.keySet(), originReviewed, issues);
        validateBannedLanguage(textValues, issues);
        return result(storyId, version, minAppVersion, issues);
    }

    private String validateNode(
            JsonNode node,
            String path,
            Set<String> rootObjectives,
            List<Reference> assetReferences,
            List<Reference> visualTargets,
            List<TextValue> textValues,
            List<Issue> issues) {
        if (node == null || !node.isObject()) {
            issues.add(issue("invalid_structure", path, "A cena precisa ser um objeto."));
            return null;
        }
        String type = text(node, "type", path + ".type", 1, 32, issues);
        Set<String> fields = switch (type == null ? "" : type) {
            case "COMIC" -> COMIC_FIELDS;
            case "PUZZLE" -> PUZZLE_FIELDS;
            case "WORD_BUILDER" -> WORD_FIELDS;
            case "GROUP_HANDOFF" -> GROUP_FIELDS;
            case "END" -> END_FIELDS;
            default -> {
                issues.add(issue("component_unknown", path + ".type", "O componente não é suportado."));
                yield BASE_FIELDS;
            }
        };
        Set<String> required = switch (type == null ? "" : type) {
            case "COMIC" -> COMIC_REQUIRED;
            case "PUZZLE" -> PUZZLE_REQUIRED;
            case "WORD_BUILDER" -> WORD_REQUIRED;
            case "GROUP_HANDOFF" -> GROUP_REQUIRED;
            case "END" -> END_REQUIRED;
            default -> BASE_REQUIRED;
        };
        exactFields(node, path, fields, required, issues);
        String id = identifier(node, "id", path + ".id", issues);
        Set<String> objectives = identifiers(node.get("objectiveIds"), path + ".objectiveIds", 1, 4, issues);
        if (!rootObjectives.containsAll(objectives)) {
            issues.add(issue("objective_not_declared", path + ".objectiveIds",
                    "A cena usa objetivo que não foi declarado no pacote."));
        }
        validateSupports(node.get("supports"), path + ".supports", visualTargets, textValues, issues);

        switch (type == null ? "" : type) {
            case "COMIC" -> validateComic(node, path, assetReferences, textValues, issues);
            case "PUZZLE" -> validatePuzzle(node, path, assetReferences, textValues, issues);
            case "WORD_BUILDER" -> validateWordBuilder(node, path, assetReferences, textValues, issues);
            case "GROUP_HANDOFF" -> {
                textValue(node, "instruction", path + ".instruction", 1, 280, textValues, issues);
                nextReference(node, path, assetReferences, false, issues);
            }
            case "END" -> textValue(node, "closingSpeech", path + ".closingSpeech", 1, 280, textValues, issues);
            default -> { }
        }
        return id;
    }

    private void validateComic(
            JsonNode node,
            String path,
            List<Reference> assetReferences,
            List<TextValue> textValues,
            List<Issue> issues) {
        assetReference(node, "visualAssetId", path, assetReferences, issues);
        textValue(node, "altText", path + ".altText", 1, 240, textValues, issues);
        if (node.has("prompt")) textValue(node, "prompt", path + ".prompt", 1, 280, textValues, issues);
        JsonNode dialogue = array(node.get("dialogue"), path + ".dialogue", 1, 6, issues);
        if (dialogue != null) {
            for (int index = 0; index < dialogue.size(); index++) {
                JsonNode line = dialogue.get(index);
                String linePath = path + ".dialogue[" + index + "]";
                exactFields(line, linePath, DIALOGUE_FIELDS, DIALOGUE_REQUIRED, issues);
                exactText(line, "speaker", linePath + ".speaker", 1, 32,
                        Set.of("LEIA_TEACHER", "NARRATOR", "CHILD_CHARACTER", "DOG"), issues);
                if (line != null && line.has("characterId")) {
                    identifier(line, "characterId", linePath + ".characterId", issues);
                }
                textValue(line, "text", linePath + ".text", 1, 280, textValues, issues);
                if (line != null && line.has("audioAssetId")) {
                    assetReference(line, "audioAssetId", linePath, assetReferences, issues);
                }
            }
        }
        nextReference(node, path, assetReferences, false, issues);
    }

    private void validatePuzzle(
            JsonNode node,
            String path,
            List<Reference> assetReferences,
            List<TextValue> textValues,
            List<Issue> issues) {
        assetReference(node, "imageAssetId", path, assetReferences, issues);
        exactText(node, "grid", path + ".grid", 1, 4, Set.of("2x2", "3x2"), issues);
        JsonNode modes = array(node.get("interactionModes"), path + ".interactionModes", 1, 2, issues);
        if (modes != null) {
            Set<String> actual = new LinkedHashSet<>(stringList(
                    modes, path + ".interactionModes", 1, 2, 3, 16, true, issues));
            if (!Set.of("TAP_SWAP", "DRAG").containsAll(actual)) {
                issues.add(issue("interaction_mode_invalid", path + ".interactionModes",
                        "O puzzle precisa usar toque ou arraste suportado."));
            }
        }
        textValue(node, "instruction", path + ".instruction", 1, 280, textValues, issues);
        textValue(node, "completionSpeech", path + ".completionSpeech", 1, 280, textValues, issues);
        nextReference(node, path, assetReferences, false, issues);
    }

    private void validateWordBuilder(
            JsonNode node,
            String path,
            List<Reference> assetReferences,
            List<TextValue> textValues,
            List<Issue> issues) {
        assetReference(node, "imageAssetId", path, assetReferences, issues);
        String word = text(node, "targetWord", path + ".targetWord", 2, 24, issues);
        JsonNode tiles = array(node.get("letterTiles"), path + ".letterTiles", 2, 32, issues);
        List<String> tileValues = tiles == null ? List.of()
                : stringList(tiles, path + ".letterTiles", 2, 32, 1, 2, false, issues);
        if (word != null && tiles != null && !canBuild(word, tileValues)) {
            issues.add(issue("word_cannot_be_built", path + ".letterTiles",
                    "Faltam letras para formar a palavra."));
        }
        if (node.has("syllables")) stringList(
                array(node.get("syllables"), path + ".syllables", 0, 12, issues),
                path + ".syllables", 0, 12, 1, 8, false, issues);
        if (node.has("initialLetterName")) text(node, "initialLetterName", path + ".initialLetterName", 1, 24, issues);
        if (node.has("initialPhonemeCue")) text(node, "initialPhonemeCue", path + ".initialPhonemeCue", 1, 80, issues);
        textValue(node, "instruction", path + ".instruction", 1, 280, textValues, issues);
        textValue(node, "completionSpeech", path + ".completionSpeech", 1, 280, textValues, issues);
        nextReference(node, path, assetReferences, false, issues);
    }

    private String validateAsset(JsonNode asset, String path, List<Issue> issues) {
        exactFields(asset, path, ASSET_FIELDS, ASSET_REQUIRED, issues);
        String id = identifier(asset, "id", path + ".id", issues);
        exactText(asset, "kind", path + ".kind", 1, 32,
                Set.of("SCENE_IMAGE", "OBJECT_IMAGE", "CHARACTER_IMAGE", "NARRATION_AUDIO"), issues);
        if (asset == null || !asset.path("required").isBoolean()) {
            issues.add(issue("invalid_structure", path + ".required", "A obrigatoriedade da mídia é inválida."));
        }
        JsonNode variants = array(asset == null ? null : asset.get("variants"), path + ".variants", 1, 4, issues);
        Set<String> roles = new HashSet<>();
        if (variants != null) {
            for (int index = 0; index < variants.size(); index++) {
                JsonNode variant = variants.get(index);
                String variantPath = path + ".variants[" + index + "]";
                exactFields(variant, variantPath, VARIANT_FIELDS, VARIANT_REQUIRED, issues);
                String role = exactText(variant, "role", variantPath + ".role", 1, 16,
                        Set.of("PHONE", "TABLET", "THUMBNAIL", "AUDIO"), issues);
                if (role != null && !roles.add(role)) {
                    issues.add(issue("duplicate_asset_variant", variantPath,
                            "A mídia repete a mesma variante."));
                }
                String filePath = text(variant, "path", variantPath + ".path", 1, 255, issues);
                if (filePath != null && (!SAFE_PATH.matcher(filePath).matches()
                        || List.of(filePath.split("/")).stream().anyMatch(
                                part -> part.isBlank() || part.equals(".") || part.equals("..")))) {
                    issues.add(issue("asset_path_invalid", variantPath + ".path", "O caminho da mídia não é seguro."));
                }
                exactText(variant, "mediaType", variantPath + ".mediaType", 1, 32,
                        Set.of("image/jpeg", "image/png", "image/webp", "audio/ogg", "audio/wav"), issues);
                boundedLong(variant == null ? null : variant.get("bytes"), variantPath + ".bytes", 1, 8_388_608, issues);
                String hash = text(variant, "sha256", variantPath + ".sha256", 64, 64, issues);
                if (hash != null && !SHA256.matcher(hash).matches()) {
                    issues.add(issue("asset_hash_invalid", variantPath + ".sha256", "O hash da mídia é inválido."));
                }
                optionalBoundedInt(variant, "width", variantPath + ".width", 1, 4096, issues);
                optionalBoundedInt(variant, "height", variantPath + ".height", 1, 4096, issues);
            }
        }
        return id;
    }

    private void validateSupports(
            JsonNode supports,
            String path,
            List<Reference> visualTargets,
            List<TextValue> textValues,
            List<Issue> issues) {
        if (supports == null || supports.isMissingNode()) return;
        JsonNode values = array(supports, path, 0, 3, issues);
        if (values == null) return;
        int previous = -1;
        Set<String> seen = new HashSet<>();
        for (int index = 0; index < values.size(); index++) {
            JsonNode support = values.get(index);
            String supportPath = path + "[" + index + "]";
            exactFields(support, supportPath, SUPPORT_FIELDS, SUPPORT_REQUIRED, issues);
            String level = exactText(support, "level", supportPath + ".level", 1, 32,
                    SUPPORT_ORDER.keySet(), issues);
            if (level != null) {
                if (!seen.add(level)) issues.add(issue("duplicate_support", path, "Um apoio foi repetido."));
                int order = SUPPORT_ORDER.get(level);
                if (order < previous) issues.add(issue("support_order", path, "Apoios devem avançar progressivamente."));
                previous = Math.max(previous, order);
            }
            textValue(support, "spokenHint", supportPath + ".spokenHint", 1, 280, textValues, issues);
            if (support != null && support.has("visualTargetId")) {
                String target = identifier(support, "visualTargetId", supportPath + ".visualTargetId", issues);
                if (target != null) visualTargets.add(new Reference(target, supportPath + ".visualTargetId", "visual"));
            }
        }
    }

    private Map<String, Boolean> validateProvenance(JsonNode provenance, List<Issue> issues) {
        Map<String, Boolean> origins = new HashMap<>();
        exactFields(provenance, "$.provenance", PROVENANCE_FIELDS, PROVENANCE_REQUIRED, issues);
        exactText(provenance, "createdBy", "$.provenance.createdBy", 1, 16,
                Set.of("TEACHER", "ASSISTED"), issues);
        identifier(provenance, "approvedBy", "$.provenance.approvedBy", issues);
        String approvedAt = text(provenance, "approvedAt", "$.provenance.approvedAt", 20, 40, issues);
        if (approvedAt != null) {
            try { Instant.parse(approvedAt); } catch (RuntimeException invalid) {
                issues.add(issue("invalid_timestamp", "$.provenance.approvedAt", "A data de aprovação é inválida."));
            }
        }
        JsonNode sourceRefs = array(value(provenance, "sourceRefs"), "$.provenance.sourceRefs", 0, 30, issues);
        if (sourceRefs != null) for (int index = 0; index < sourceRefs.size(); index++) {
            JsonNode source = sourceRefs.get(index);
            String path = "$.provenance.sourceRefs[" + index + "]";
            exactFields(source, path, SOURCE_REF_FIELDS, SOURCE_REF_FIELDS, issues);
            identifier(source, "sourceId", path + ".sourceId", issues);
            identifier(source, "sourceVersion", path + ".sourceVersion", issues);
        }
        JsonNode assetOrigins = array(value(provenance, "assetOrigins"), "$.provenance.assetOrigins", 0, 100, issues);
        if (assetOrigins != null) for (int index = 0; index < assetOrigins.size(); index++) {
            JsonNode origin = assetOrigins.get(index);
            String path = "$.provenance.assetOrigins[" + index + "]";
            exactFields(origin, path, ORIGIN_FIELDS, ORIGIN_REQUIRED, issues);
            String assetId = identifier(origin, "assetId", path + ".assetId", issues);
            exactText(origin, "origin", path + ".origin", 1, 32,
                    Set.of("TEACHER_UPLOAD", "APPROVED_LIBRARY", "AI_GENERATED", "AI_EDITED"), issues);
            optionalText(origin, "provider", path + ".provider", 1, 80, issues);
            optionalText(origin, "model", path + ".model", 1, 120, issues);
            optionalText(origin, "generationId", path + ".generationId", 1, 120, issues);
            boolean reviewed = origin != null && origin.path("reviewedByTeacher").isBoolean()
                    && origin.path("reviewedByTeacher").asBoolean();
            if (!reviewed) issues.add(issue("asset_not_reviewed", path + ".reviewedByTeacher",
                    "A mídia ainda não foi revisada pela professora."));
            if (assetId != null && origins.putIfAbsent(assetId, reviewed) != null) {
                issues.add(issue("asset_provenance_mismatch", "$.provenance.assetOrigins",
                        "Cada mídia precisa de uma origem."));
            }
        }
        return origins;
    }

    private void validateAccessibility(JsonNode accessibility, List<Issue> issues) {
        exactFields(accessibility, "$.accessibility", ACCESSIBILITY_FIELDS, ACCESSIBILITY_FIELDS, issues);
        boundedInt(value(accessibility, "minTouchTargetDp"), "$.accessibility.minTouchTargetDp", 48, Integer.MAX_VALUE, issues);
        requiredTrue(accessibility, "reducedStimuliSupported", "$.accessibility.reducedStimuliSupported", issues);
        requiredTrue(accessibility, "spokenInstructions", "$.accessibility.spokenInstructions", issues);
        requiredTrue(accessibility, "noRequiredScroll", "$.accessibility.noRequiredScroll", issues);
    }

    private void validateReferences(
            Map<String, JsonNode> nodes,
            Map<String, JsonNode> assets,
            List<Reference> assetReferences,
            List<Reference> visualTargets,
            List<Issue> issues) {
        for (JsonNode node : nodes.values()) {
            String type = stringValue(node.get("type"));
            if (!"END".equals(type)) {
                String next = stringValue(node.get("nextNodeId"));
                if (next == null || !nodes.containsKey(next)) {
                    issues.add(issue("next_not_found", "$.nodes", "A próxima cena não existe."));
                }
            }
        }
        for (Reference reference : assetReferences) {
            if ("asset".equals(reference.kind()) && !assets.containsKey(reference.id())) {
                issues.add(issue("asset_not_found", reference.path(), "A mídia referenciada não existe."));
            }
        }
        for (Reference reference : visualTargets) {
            if (!nodes.containsKey(reference.id()) && !assets.containsKey(reference.id())) {
                issues.add(issue("visual_target_not_found", reference.path(), "O alvo visual não existe."));
            }
        }
    }

    private void validateGraph(Map<String, JsonNode> nodes, String start, List<Issue> issues) {
        if (start == null || !nodes.containsKey(start)) return;
        Set<String> visited = new HashSet<>();
        Set<String> active = new HashSet<>();
        ArrayDeque<String> path = new ArrayDeque<>();
        boolean[] reachesEnd = {false};
        walk(start, nodes, visited, active, path, reachesEnd, issues);
        if (visited.size() != nodes.size()) {
            issues.add(issue("unreachable_nodes", "$.nodes", "Há cenas inalcançáveis."));
        }
        if (!reachesEnd[0]) {
            issues.add(issue("end_not_reached", "$.nodes", "Não existe caminho até o encerramento."));
        }
    }

    private void walk(
            String id,
            Map<String, JsonNode> nodes,
            Set<String> visited,
            Set<String> active,
            ArrayDeque<String> path,
            boolean[] reachesEnd,
            List<Issue> issues) {
        if (active.contains(id)) {
            issues.add(issue("cycle_detected", "$.nodes", "A história contém um ciclo."));
            return;
        }
        if (!visited.add(id)) return;
        active.add(id);
        path.addLast(id);
        JsonNode node = nodes.get(id);
        if ("END".equals(stringValue(node.get("type")))) {
            reachesEnd[0] = true;
        } else {
            String next = stringValue(node.get("nextNodeId"));
            if (next != null && nodes.containsKey(next)) walk(next, nodes, visited, active, path, reachesEnd, issues);
        }
        path.removeLast();
        active.remove(id);
    }

    private void validateAssetOrigins(
            Collection<String> assetIds, Map<String, Boolean> origins, List<Issue> issues) {
        if (!new HashSet<>(assetIds).equals(origins.keySet())) {
            issues.add(issue("asset_provenance_mismatch", "$.provenance.assetOrigins",
                    "Cada mídia precisa de uma origem."));
        }
    }

    private void validateBannedLanguage(List<TextValue> textValues, List<Issue> issues) {
        for (TextValue value : textValues) {
            if (value.value() == null) continue;
            for (PatternIssue banned : BANNED) {
                if (banned.pattern().matcher(value.value()).find()) {
                    issues.add(issue(banned.code(), value.path(), "Linguagem bloqueada para a jornada infantil."));
                }
            }
        }
    }

    private record TextValue(String path, String value) {}

    private static boolean canBuild(String word, List<String> tiles) {
        Map<Integer, Integer> required = counts(word);
        Map<Integer, Integer> available = new HashMap<>();
        for (String tile : tiles) {
            counts(tile).forEach((letter, count) -> available.merge(letter, count, Integer::sum));
        }
        return required.entrySet().stream().allMatch(
                entry -> available.getOrDefault(entry.getKey(), 0) >= entry.getValue());
    }

    private static Map<Integer, Integer> counts(String value) {
        Map<Integer, Integer> counts = new HashMap<>();
        Normalizer.normalize(value, Normalizer.Form.NFC).toUpperCase().codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint))
                .forEach(codePoint -> counts.merge(codePoint, 1, Integer::sum));
        return counts;
    }

    private static void nextReference(
            JsonNode node, String path, List<Reference> ignored, boolean ignoredFlag, List<Issue> issues) {
        identifier(node, "nextNodeId", path + ".nextNodeId", issues);
    }

    private static void assetReference(
            JsonNode node, String field, String path, List<Reference> references, List<Issue> issues) {
        String id = identifier(node, field, path + "." + field, issues);
        if (id != null) references.add(new Reference(id, path + "." + field, "asset"));
    }

    private static void textValue(
            JsonNode object, String field, String path, int min, int max,
            List<TextValue> values, List<Issue> issues) {
        values.add(new TextValue(path, text(object, field, path, min, max, issues)));
    }

    private static void exactFields(
            JsonNode object, String path, Set<String> allowed, Set<String> required, List<Issue> issues) {
        if (object == null || !object.isObject()) {
            issues.add(issue("invalid_structure", path, "O objeto obrigatório está ausente ou inválido."));
            return;
        }
        object.fieldNames().forEachRemaining(field -> {
            if (!allowed.contains(field)) issues.add(issue("unknown_field", path + "." + field,
                    "O pacote possui um campo não suportado."));
        });
        required.stream().filter(field -> !object.has(field)).forEach(field -> issues.add(
                issue("required_field_missing", path + "." + field, "O campo é obrigatório.")));
    }

    private static JsonNode array(JsonNode value, String path, int min, int max, List<Issue> issues) {
        if (value == null || !value.isArray()) {
            issues.add(issue("invalid_structure", path, "A lista obrigatória é inválida."));
            return null;
        }
        if (value.size() < min || value.size() > max) {
            issues.add(issue("array_bounds", path, "A quantidade de itens não é permitida."));
        }
        return value;
    }

    private static Set<String> identifiers(JsonNode value, String path, int min, int max, List<Issue> issues) {
        JsonNode array = array(value, path, min, max, issues);
        if (array == null) return Set.of();
        Set<String> result = new LinkedHashSet<>();
        for (int index = 0; index < array.size(); index++) {
            JsonNode item = array.get(index);
            String id = item != null && item.isTextual() ? item.asText() : null;
            if (id == null || !ID.matcher(id).matches()) {
                issues.add(issue("invalid_identifier", path + "[" + index + "]", "O identificador é inválido."));
            } else if (!result.add(id)) {
                issues.add(issue("duplicate_identifier", path, "Identificadores não podem se repetir."));
            }
        }
        return result;
    }

    private static List<String> stringList(
            JsonNode array, String path, int minItems, int maxItems, int minLength, int maxLength,
            boolean unique, List<Issue> issues) {
        if (array == null) return List.of();
        List<String> values = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (array.size() < minItems || array.size() > maxItems) {
            issues.add(issue("array_bounds", path, "A quantidade de itens não é permitida."));
        }
        for (int index = 0; index < array.size(); index++) {
            JsonNode item = array.get(index);
            String text = item != null && item.isTextual() ? item.asText() : null;
            if (text == null || text.length() < minLength || text.length() > maxLength) {
                issues.add(issue("invalid_structure", path + "[" + index + "]", "O texto é inválido."));
            } else if (unique && !seen.add(text)) {
                issues.add(issue("duplicate_identifier", path, "Itens não podem se repetir."));
            } else {
                values.add(text);
            }
        }
        return values;
    }

    private static String identifier(JsonNode object, String field, String path, List<Issue> issues) {
        String value = text(object, field, path, 3, 64, issues);
        if (value != null && !ID.matcher(value).matches()) {
            issues.add(issue("invalid_identifier", path, "O identificador é inválido."));
            return null;
        }
        return value;
    }

    private static String exactText(
            JsonNode object, String field, String path, int min, int max,
            Set<String> values, List<Issue> issues) {
        String value = text(object, field, path, min, max, issues);
        if (value != null && !values.contains(value)) {
            issues.add(issue("invalid_value", path, "O valor não é suportado."));
            return null;
        }
        return value;
    }

    private static void optionalText(
            JsonNode object, String field, String path, int min, int max, List<Issue> issues) {
        if (object != null && object.has(field)) text(object, field, path, min, max, issues);
    }

    private static String text(JsonNode object, String field, String path, int min, int max, List<Issue> issues) {
        JsonNode value = value(object, field);
        if (value == null || !value.isTextual() || value.asText().length() < min || value.asText().length() > max) {
            issues.add(issue("invalid_structure", path, "O texto obrigatório é inválido."));
            return null;
        }
        return value.asText();
    }

    private static int positiveInt(JsonNode object, String field, String path, List<Issue> issues) {
        return boundedInt(value(object, field), path, 1, Integer.MAX_VALUE, issues);
    }

    private static int boundedInt(JsonNode value, String path, int min, int max, List<Issue> issues) {
        if (value == null || !value.canConvertToInt() || !value.isIntegralNumber()
                || value.asInt() < min || value.asInt() > max) {
            issues.add(issue("invalid_structure", path, "O número obrigatório é inválido."));
            return 0;
        }
        return value.asInt();
    }

    private static void optionalBoundedInt(
            JsonNode object, String field, String path, int min, int max, List<Issue> issues) {
        if (object != null && object.has(field)) boundedInt(object.get(field), path, min, max, issues);
    }

    private static void boundedLong(JsonNode value, String path, long min, long max, List<Issue> issues) {
        if (value == null || !value.canConvertToLong() || !value.isIntegralNumber()
                || value.asLong() < min || value.asLong() > max) {
            issues.add(issue("invalid_structure", path, "O número obrigatório é inválido."));
        }
    }

    private static void requiredTrue(JsonNode object, String field, String path, List<Issue> issues) {
        if (object == null || !object.path(field).isBoolean() || !object.path(field).asBoolean()) {
            issues.add(issue("accessibility_required", path, "Este recurso de acessibilidade é obrigatório."));
        }
    }

    private static JsonNode value(JsonNode object, String field) {
        return object == null || !object.isObject() ? null : object.get(field);
    }

    private static String stringValue(JsonNode value) {
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private static Issue issue(String code, String path, String message) {
        return new Issue(code, path, message);
    }

    private static Result result(String storyId, int version, int minAppVersion, List<Issue> issues) {
        return new Result(storyId, version, minAppVersion, issues.stream()
                .sorted(Comparator.comparing(Issue::path).thenComparing(Issue::code)).toList());
    }

    private static final Set<String> ROOT_FIELDS = Set.of(
            "schemaVersion", "packId", "storyId", "version", "minAppVersion", "title", "methodology",
            "objectiveIds", "estimatedMinutes", "startNodeId", "nodes", "assets", "accessibility",
            "provenance", "publishedAt");
    private static final Set<String> ROOT_REQUIRED = Set.of(
            "schemaVersion", "packId", "storyId", "version", "minAppVersion", "title", "methodology",
            "objectiveIds", "startNodeId", "nodes", "assets", "accessibility", "provenance");
    private static final Set<String> BASE_FIELDS = Set.of("id", "type", "objectiveIds", "supports");
    private static final Set<String> BASE_REQUIRED = Set.of("id", "type", "objectiveIds");
    private static final Set<String> COMIC_FIELDS = union(BASE_FIELDS,
            "visualAssetId", "altText", "dialogue", "prompt", "nextNodeId");
    private static final Set<String> COMIC_REQUIRED = Set.of(
            "id", "type", "objectiveIds", "visualAssetId", "altText", "dialogue", "nextNodeId");
    private static final Set<String> PUZZLE_FIELDS = union(BASE_FIELDS,
            "imageAssetId", "grid", "interactionModes", "instruction", "completionSpeech", "nextNodeId");
    private static final Set<String> PUZZLE_REQUIRED = Set.of(
            "id", "type", "objectiveIds", "imageAssetId", "grid", "interactionModes", "instruction",
            "completionSpeech", "nextNodeId");
    private static final Set<String> WORD_FIELDS = union(BASE_FIELDS,
            "imageAssetId", "targetWord", "letterTiles", "syllables", "initialLetterName",
            "initialPhonemeCue", "instruction", "completionSpeech", "nextNodeId");
    private static final Set<String> WORD_REQUIRED = Set.of(
            "id", "type", "objectiveIds", "imageAssetId", "targetWord", "letterTiles", "instruction",
            "completionSpeech", "nextNodeId");
    private static final Set<String> GROUP_FIELDS = union(BASE_FIELDS, "instruction", "nextNodeId");
    private static final Set<String> GROUP_REQUIRED = Set.of("id", "type", "objectiveIds", "instruction", "nextNodeId");
    private static final Set<String> END_FIELDS = union(BASE_FIELDS, "closingSpeech");
    private static final Set<String> END_REQUIRED = Set.of("id", "type", "objectiveIds", "closingSpeech");
    private static final Set<String> SUPPORT_FIELDS = Set.of("level", "spokenHint", "visualTargetId");
    private static final Set<String> SUPPORT_REQUIRED = Set.of("level", "spokenHint");
    private static final Set<String> DIALOGUE_FIELDS = Set.of("speaker", "characterId", "text", "audioAssetId");
    private static final Set<String> DIALOGUE_REQUIRED = Set.of("speaker", "text");
    private static final Set<String> ASSET_FIELDS = Set.of("id", "kind", "required", "variants");
    private static final Set<String> ASSET_REQUIRED = ASSET_FIELDS;
    private static final Set<String> VARIANT_FIELDS = Set.of("role", "path", "mediaType", "width", "height", "bytes", "sha256");
    private static final Set<String> VARIANT_REQUIRED = Set.of("role", "path", "mediaType", "bytes", "sha256");
    private static final Set<String> ACCESSIBILITY_FIELDS = Set.of(
            "minTouchTargetDp", "reducedStimuliSupported", "spokenInstructions", "noRequiredScroll");
    private static final Set<String> PROVENANCE_FIELDS = Set.of(
            "createdBy", "approvedBy", "approvedAt", "sourceRefs", "assetOrigins");
    private static final Set<String> PROVENANCE_REQUIRED = PROVENANCE_FIELDS;
    private static final Set<String> SOURCE_REF_FIELDS = Set.of("sourceId", "sourceVersion");
    private static final Set<String> ORIGIN_FIELDS = Set.of(
            "assetId", "origin", "provider", "model", "generationId", "reviewedByTeacher");
    private static final Set<String> ORIGIN_REQUIRED = Set.of("assetId", "origin", "reviewedByTeacher");

    private static Set<String> union(Set<String> base, String... more) {
        Set<String> result = new HashSet<>(base);
        result.addAll(List.of(more));
        return Set.copyOf(result);
    }
}
