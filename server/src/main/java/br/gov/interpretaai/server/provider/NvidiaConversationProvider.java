package br.gov.interpretaai.server.provider;

import br.gov.interpretaai.server.api.VoiceTurnModels.PedagogicalReply;
import br.gov.interpretaai.server.api.VoiceTurnModels.Request;
import br.gov.interpretaai.server.core.ConversationPromptFactory;
import br.gov.interpretaai.server.core.PedagogicalReplyContract;
import br.gov.interpretaai.server.core.WarmableConversationProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NvidiaConversationProvider implements WarmableConversationProvider {
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
            @Value("${interpretaai.conversation.provider-timeout-ms:3500}") long providerTimeoutMs,
            ObjectMapper json,
            ConversationPromptFactory prompts) {
        if (providerTimeoutMs < 1) {
            throw new IllegalArgumentException("Timeout do provedor deve ser positivo");
        }
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
                .timeout(Duration.ofMillis(providerTimeoutMs))
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
            String content = model.chat(prompts.create(request, recentMessages));
            PedagogicalReply reply = PedagogicalReplyContract.decode(json, content, request.turn());
            warmUntilEpochMs = System.currentTimeMillis() + WARM_TTL_MS;
            return reply;
        } catch (Exception error) {
            markCold();
            throw new IllegalStateException("NVIDIA NIM indisponível ou resposta inválida", error);
        }
    }

}
