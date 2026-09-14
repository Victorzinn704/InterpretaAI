package br.gov.interpretaai.server.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
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
                loaded.put(entry.getKey(), new SceneContext(context, objective));
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

    public String version() {
        return version;
    }

    public int sceneCount() {
        return scenes.size();
    }

    private static String required(JsonNode node, String field, String sceneId) {
        String value = node.path(field).asText().trim();
        if (value.isEmpty() || value.length() > 400) {
            throw new IllegalStateException("Campo inválido no ScenePack: " + sceneId + "." + field);
        }
        return value;
    }

    public record SceneContext(String context, String objective) {}
}
