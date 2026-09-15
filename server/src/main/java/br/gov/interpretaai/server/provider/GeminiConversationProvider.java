package br.gov.interpretaai.server.provider;

import br.gov.interpretaai.server.api.VoiceTurnModels.PedagogicalReply;
import br.gov.interpretaai.server.api.VoiceTurnModels.Request;
import br.gov.interpretaai.server.core.ConversationPromptFactory;
import br.gov.interpretaai.server.core.PedagogicalReplyContract;
import br.gov.interpretaai.server.core.WarmableConversationProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.google.genai.GoogleGenAiChatModel;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GeminiConversationProvider implements WarmableConversationProvider {
    private static final long WARM_TTL_MS = 150_000;
    private final GoogleGenAiChatModel model;
    private final GoogleGenAiChatModel warmupModel;
    private final String modelName;
    private final boolean warmupEnabled;
    private final ObjectMapper json;
    private final ConversationPromptFactory prompts;
    private volatile long warmUntilEpochMs;

    public GeminiConversationProvider(
            @Value("${interpretaai.gemini.api-key:}") String apiKey,
            @Value("${interpretaai.gemini.model:gemini-3.8-flash}") String modelName,
            @Value("${interpretaai.gemini.thinking-level:LOW}") String thinkingLevel,
            @Value("${interpretaai.gemini.warmup-enabled:true}") boolean warmupEnabled,
            @Value("${interpretaai.conversation.provider-timeout-ms:3500}") long providerTimeoutMs,
            ObjectMapper json,
            ConversationPromptFactory prompts) {
        if (providerTimeoutMs < 1) {
            throw new IllegalArgumentException("Timeout do provedor deve ser positivo");
        }
        this.json = json;
        this.prompts = prompts;
        this.modelName = modelName;
        this.warmupEnabled = warmupEnabled;
        this.model = apiKey.isBlank() ? null : GoogleGenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .maxOutputTokens(256)
                .thinkingLevel(thinkingLevel)
                .timeout(Duration.ofMillis(providerTimeoutMs))
                .maxRetries(0)
                .logRequests(false)
                .logResponses(false)
                .build();
        this.warmupModel = apiKey.isBlank() ? null : GoogleGenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .maxOutputTokens(32)
                .thinkingLevel(thinkingLevel)
                .timeout(Duration.ofSeconds(10))
                .maxRetries(0)
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    @Override public String providerId() { return "gemini"; }
    @Override public boolean available() { return isWarm(); }

    @Override
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

    @Override
    public boolean isWarm() {
        return model != null && (!warmupEnabled || System.currentTimeMillis() < warmUntilEpochMs);
    }

    @Override public String activeModelId() { return modelName; }

    private void markCold() { warmUntilEpochMs = 0; }

    @Override
    public PedagogicalReply reply(Request request, List<String> recentMessages) {
        if (model == null) throw new IllegalStateException("Gemini sem credencial");
        try {
            var chatRequest = PedagogicalReplyContract.request(prompts.create(request, recentMessages));
            String content = model.chat(chatRequest).aiMessage().text();
            PedagogicalReply reply = PedagogicalReplyContract.decode(json, content, request.turn());
            warmUntilEpochMs = System.currentTimeMillis() + WARM_TTL_MS;
            return reply;
        } catch (Exception error) {
            markCold();
            throw new IllegalStateException("Gemini indisponível ou resposta inválida", error);
        }
    }
}
