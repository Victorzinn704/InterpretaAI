package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.VoiceTurnModels.NextAction;
import br.gov.interpretaai.server.api.VoiceTurnModels.PedagogicalReply;
import br.gov.interpretaai.server.api.VoiceTurnModels.Request;
import br.gov.interpretaai.server.api.VoiceTurnModels.VisualReaction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.text.Normalizer;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Conteúdo pedagógico imutável, carregado uma vez e consultado em O(1) no caminho quente. */
@Component
public class ScenePackCatalog {
    private static final String VERSION_PATTERN = "v[0-9]+";

    private final String version;
    private final Map<String, SceneContext> scenes;

    public ScenePackCatalog(
            ObjectMapper json,
            @Value("${interpretaai.scene-pack.version:v2}") String configuredVersion) {
        if (configuredVersion == null || !configuredVersion.matches(VERSION_PATTERN)) {
            throw new IllegalArgumentException("Versão de ScenePack inválida");
        }
        String resourceName = "scene-packs/" + configuredVersion + ".json";
        try (var input = new ClassPathResource(resourceName).getInputStream()) {
            JsonNode root = json.readTree(input);
            if (!configuredVersion.equals(root.path("version").asText())) {
                throw new IllegalStateException("Versão interna do ScenePack não corresponde ao arquivo");
            }
            Map<String, SceneContext> loaded = new LinkedHashMap<>();
            root.path("scenes").properties().forEach(entry -> {
                JsonNode value = entry.getValue();
                String context = required(value, "context", entry.getKey());
                String objective = required(value, "objective", entry.getKey());
                List<String> acceptedAnswers = strings(value.path("acceptedAnswers"));
                String completionReply = value.path("completionReply").asText("").trim();
                if (acceptedAnswers.isEmpty() != completionReply.isEmpty()) {
                    throw new IllegalStateException("Resposta determinística incompleta: " + entry.getKey());
                }
                loaded.put(entry.getKey(), new SceneContext(
                        context, objective, acceptedAnswers, completionReply));
            });
            if (loaded.isEmpty()) throw new IllegalStateException("ScenePack não pode ser vazio");
            this.version = configuredVersion;
            this.scenes = Map.copyOf(loaded);
        } catch (IOException error) {
            throw new IllegalStateException("ScenePack não encontrado: " + configuredVersion, error);
        }
    }

    public Optional<SceneContext> find(String sceneId) {
        return Optional.ofNullable(scenes.get(sceneId));
    }

    /** Resolve respostas inequívocas sem rede nem inferência; casos ambíguos seguem para o LLM. */
    public Optional<PedagogicalReply> deterministicReply(Request request) {
        SceneContext scene = scenes.get(request.sceneId());
        if (scene == null || scene.acceptedAnswers().isEmpty()) return Optional.empty();
        String spoken = normalized(request.transcript());
        boolean accepted = scene.acceptedAnswers().stream()
                .map(ScenePackCatalog::normalized)
                .anyMatch(answer -> containsAffirmedAnswer(spoken, answer));
        if (!accepted) return Optional.empty();
        return Optional.of(new PedagogicalReply(
                scene.completionReply(), VisualReaction.CELEBRATE,
                NextAction.CONTINUE, "ORAL_EXPRESSION"));
    }

    public String version() {
        return version;
    }

    public int sceneCount() {
        return scenes.size();
    }

    public List<String> preparedCompletionReplies() {
        return scenes.values().stream()
                .map(SceneContext::completionReply)
                .filter(reply -> !reply.isEmpty())
                .distinct()
                .toList();
    }

    private static String required(JsonNode node, String field, String sceneId) {
        String value = node.path(field).asText().trim();
        if (value.isEmpty() || value.length() > 400) {
            throw new IllegalStateException("Campo inválido no ScenePack: " + sceneId + "." + field);
        }
        return value;
    }

    private static List<String> strings(JsonNode node) {
        if (!node.isArray()) return List.of();
        List<String> result = new java.util.ArrayList<>();
        node.forEach(value -> {
            String text = value.asText("").trim();
            if (text.isEmpty() || text.length() > 80) {
                throw new IllegalStateException("Resposta aceita inválida no ScenePack");
            }
            result.add(text);
        });
        return List.copyOf(result);
    }

    private static String normalized(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }

    private static boolean containsAffirmedAnswer(String spoken, String answer) {
        List<String> tokens = List.of(spoken.split(" "));
        List<String> answerTokens = List.of(answer.split(" "));
        for (int start = 0; start <= tokens.size() - answerTokens.size(); start++) {
            if (!tokens.subList(start, start + answerTokens.size()).equals(answerTokens)) continue;
            int contextStart = Math.max(0, start - 4);
            boolean negated = tokens.subList(contextStart, start).stream()
                    .anyMatch(token -> token.equals("nao") || token.equals("nem"));
            if (!negated) return true;
        }
        return false;
    }

    public record SceneContext(
            String context,
            String objective,
            List<String> acceptedAnswers,
            String completionReply) {}
}
