package br.gov.interpretaai.server.provider;

import br.gov.interpretaai.server.api.VoiceTurnModels.NextAction;
import br.gov.interpretaai.server.api.VoiceTurnModels.PedagogicalReply;
import br.gov.interpretaai.server.api.VoiceTurnModels.Request;
import br.gov.interpretaai.server.api.VoiceTurnModels.VisualReaction;
import br.gov.interpretaai.server.core.ConversationPromptFactory;
import br.gov.interpretaai.server.core.RoutableConversationProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.google.genai.GoogleGenAiChatModel;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GeminiConversationProvider implements RoutableConversationProvider {
    private final GoogleGenAiChatModel model;
    private final ObjectMapper json;
    private final ConversationPromptFactory prompts;

    public GeminiConversationProvider(
            @Value("${interpretaai.gemini.api-key:}") String apiKey,
            @Value("${interpretaai.gemini.model:gemini-3.8-flash}") String modelName,
            @Value("${interpretaai.gemini.thinking-level:LOW}") String thinkingLevel,
            @Value("${interpretaai.conversation.provider-timeout-ms:3500}") long providerTimeoutMs,
            ObjectMapper json,
            ConversationPromptFactory prompts) {
        if (providerTimeoutMs < 1) {
            throw new IllegalArgumentException("Timeout do provedor deve ser positivo");
        }
        this.json = json;
        this.prompts = prompts;
        this.model = apiKey.isBlank() ? null : GoogleGenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.2)
                .maxOutputTokens(256)
                .thinkingLevel(thinkingLevel)
                .timeout(Duration.ofMillis(providerTimeoutMs))
                .maxRetries(0)
                .responseFormat(dev.langchain4j.model.chat.request.ResponseFormat.JSON)
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    @Override public String providerId() { return "gemini"; }
    @Override public boolean available() { return model != null; }

    @Override
    public PedagogicalReply reply(Request request, List<String> recentMessages) {
        if (model == null) throw new IllegalStateException("Gemini sem credencial");
        try {
            JsonNode node = json.readTree(model.chat(prompts.create(request, recentMessages)));
            NextAction nextAction = request.turn() >= 3
                    ? NextAction.CONTINUE
                    : NextAction.valueOf(node.path("nextAction").asText("SPEAK_AGAIN"));
            return new PedagogicalReply(
                    node.path("replyText").asText(),
                    VisualReaction.valueOf(node.path("visualReaction").asText("ENCOURAGE")),
                    nextAction,
                    node.path("observationCategory").asText("ORAL_EXPRESSION"));
        } catch (Exception error) {
            throw new IllegalStateException("Gemini indisponível ou resposta inválida", error);
        }
    }
}
