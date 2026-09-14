package br.gov.interpretaai.server.provider;

import br.gov.interpretaai.server.api.VoiceTurnModels.NextAction;
import br.gov.interpretaai.server.api.VoiceTurnModels.PedagogicalReply;
import br.gov.interpretaai.server.api.VoiceTurnModels.Request;
import br.gov.interpretaai.server.api.VoiceTurnModels.VisualReaction;
import br.gov.interpretaai.server.core.ConversationPromptFactory;
import br.gov.interpretaai.server.core.RoutableConversationProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.ollama.OllamaChatModel;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OllamaConversationProvider implements RoutableConversationProvider {
    private final OllamaChatModel model;
    private final ObjectMapper json;
    private final ConversationPromptFactory prompts;

    public OllamaConversationProvider(
            @Value("${interpretaai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${interpretaai.ollama.model:qwen2.5:1.5b}") String modelName,
            ObjectMapper json,
            ConversationPromptFactory prompts) {
        this.json = json;
        this.prompts = prompts;
        this.model = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.2)
                .timeout(Duration.ofSeconds(12))
                .maxRetries(0)
                .responseFormat(ResponseFormat.JSON)
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    @Override public String providerId() { return "ollama"; }
    @Override public boolean available() { return true; }

    @Override
    public PedagogicalReply reply(Request request, List<String> recentMessages) {
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
            throw new IllegalStateException("Ollama indisponível ou resposta inválida", error);
        }
    }

    private String safeObservation(String value) {
        String normalized = value == null ? "" : value.toLowerCase();
        if (normalized.contains("context") || normalized.contains("espa")) return "CONTEXT_REASONING";
        if (normalized.contains("particip") || normalized.contains("coop")) return "PARTICIPATION";
        return "ORAL_EXPRESSION";
    }
}
