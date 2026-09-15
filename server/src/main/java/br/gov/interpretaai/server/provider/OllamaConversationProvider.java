package br.gov.interpretaai.server.provider;

import br.gov.interpretaai.server.api.VoiceTurnModels.PedagogicalReply;
import br.gov.interpretaai.server.api.VoiceTurnModels.Request;
import br.gov.interpretaai.server.core.ConversationPromptFactory;
import br.gov.interpretaai.server.core.PedagogicalReplyContract;
import br.gov.interpretaai.server.core.WarmableConversationProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.ollama.OllamaChatModel;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OllamaConversationProvider implements WarmableConversationProvider {
    private static final long WARM_TTL_MS = 150_000;
    private final OllamaChatModel model;
    private final OllamaChatModel warmupModel;
    private final String modelName;
    private final boolean warmupEnabled;
    private final ObjectMapper json;
    private final ConversationPromptFactory prompts;
    private volatile long warmUntilEpochMs;

    public OllamaConversationProvider(
            @Value("${interpretaai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${interpretaai.ollama.model:qwen2.5:1.5b}") String modelName,
            @Value("${interpretaai.ollama.warmup-enabled:true}") boolean warmupEnabled,
            @Value("${interpretaai.conversation.provider-timeout-ms:3500}") long providerTimeoutMs,
            ObjectMapper json,
            ConversationPromptFactory prompts) {
        this.json = json;
        this.prompts = prompts;
        this.modelName = modelName;
        this.warmupEnabled = warmupEnabled;
        this.model = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.2)
                .numPredict(120)
                .timeout(Duration.ofMillis(providerTimeoutMs))
                .maxRetries(0)
                .logRequests(false)
                .logResponses(false)
                .build();
        this.warmupModel = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.1)
                .numPredict(8)
                .timeout(Duration.ofSeconds(30))
                .maxRetries(0)
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    @Override public String providerId() { return "ollama"; }
    @Override public boolean available() { return isWarm(); }
    @Override public String activeModelId() { return modelName; }

    @Override
    public boolean warmUp() {
        try {
            warmupModel.chat("Responda apenas: pronto");
            warmUntilEpochMs = System.currentTimeMillis() + WARM_TTL_MS;
            return true;
        } catch (RuntimeException ignored) {
            warmUntilEpochMs = 0;
            return false;
        }
    }

    @Override
    public boolean isWarm() {
        return !warmupEnabled || System.currentTimeMillis() < warmUntilEpochMs;
    }

    @Override
    public PedagogicalReply reply(Request request, List<String> recentMessages) {
        try {
            var chatRequest = PedagogicalReplyContract.request(prompts.create(request, recentMessages));
            String content = model.chat(chatRequest).aiMessage().text();
            warmUntilEpochMs = System.currentTimeMillis() + WARM_TTL_MS;
            return PedagogicalReplyContract.decode(json, content, request.turn());
        } catch (Exception error) {
            warmUntilEpochMs = 0;
            throw new IllegalStateException("Ollama indisponível ou resposta inválida", error);
        }
    }
}
