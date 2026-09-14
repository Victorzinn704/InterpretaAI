package br.gov.interpretaai.server.provider;

import br.gov.interpretaai.server.api.VoiceTurnModels.NextAction;
import br.gov.interpretaai.server.api.VoiceTurnModels.PedagogicalReply;
import br.gov.interpretaai.server.api.VoiceTurnModels.Request;
import br.gov.interpretaai.server.api.VoiceTurnModels.VisualReaction;
import br.gov.interpretaai.server.core.ConversationPromptFactory;
import br.gov.interpretaai.server.core.RoutableConversationProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NvidiaConversationProvider implements RoutableConversationProvider {
    private static final long WARM_TTL_MS = 150_000;
    private final OpenAiChatModel model;
    private final OpenAiChatModel warmupModel;
    private final String modelName;
    private final boolean warmupEnabled;
    private final ObjectMapper json;
    private final ConversationPromptFactory prompts;
    private volatile long warmUntilEpochMs;

    public NvidiaConversationProvider(
            @Value("${interpretaai.nvidia.base-url:https://integrate.api.nvidia.com/v1}") String baseUrl,
            @Value("${interpretaai.nvidia.api-key:}") String apiKey,
            @Value("${interpretaai.nvidia.model:mistralai/mistral-nemotron}") String modelName,
            @Value("${interpretaai.nvidia.warmup-enabled:true}") boolean warmupEnabled,
            ObjectMapper json,
            ConversationPromptFactory prompts) {
        this.json = json;
        this.prompts = prompts;
        NvidiaModelCatalog.fromModelId(modelName);
        this.modelName = modelName;
        this.warmupEnabled = warmupEnabled;
        this.model = apiKey.isBlank() ? null : OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.2)
                .topP(0.7)
                .maxTokens(120)
                .timeout(Duration.ofSeconds(4))
                .maxRetries(0)
                .responseFormat("json_object")
                .logRequests(false)
                .logResponses(false)
                .build();
        this.warmupModel = apiKey.isBlank() ? null : OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.1)
                .maxTokens(8)
                .timeout(Duration.ofSeconds(15))
                .maxRetries(0)
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    @Override public String providerId() { return "nvidia"; }
    @Override public boolean available() { return isWarm(); }

    public boolean warmUp() {
        if (warmupModel == null) return false;
        try {
            warmupModel.chat("Responda apenas: pronto");
            warmUntilEpochMs = System.currentTimeMillis() + WARM_TTL_MS;
            return true;
        } catch (RuntimeException ignored) {
            markCold();
            return false;
        }
    }

    public String activeModelId() {
        return modelName;
    }

    public boolean isWarm() {
        return model != null && (!warmupEnabled || System.currentTimeMillis() < warmUntilEpochMs);
    }

    private void markCold() {
        warmUntilEpochMs = 0;
    }

    @Override
    public PedagogicalReply reply(Request request, List<String> recentMessages) {
        if (!isWarm()) throw new IllegalStateException("NVIDIA NIM ainda não está quente");
        try {
            JsonNode node = json.readTree(model.chat(prompts.create(request, recentMessages)));
            NextAction nextAction = request.turn() >= 3
                    ? NextAction.CONTINUE
                    : NextAction.valueOf(node.path("nextAction").asText("SPEAK_AGAIN"));
            return new PedagogicalReply(
                    node.path("replyText").asText(),
                    VisualReaction.valueOf(node.path("visualReaction").asText("ENCOURAGE")),
                    nextAction,
                    safeObservation(node.path("observationCategory").asText()));
        } catch (Exception error) {
            markCold();
            throw new IllegalStateException("NVIDIA NIM indisponível ou resposta inválida", error);
        }
    }

    private String safeObservation(String value) {
        String normalized = value == null ? "" : value.toLowerCase();
        if (normalized.contains("context") || normalized.contains("espa")) return "CONTEXT_REASONING";
        if (normalized.contains("particip") || normalized.contains("coop")) return "PARTICIPATION";
        return "ORAL_EXPRESSION";
    }
}
