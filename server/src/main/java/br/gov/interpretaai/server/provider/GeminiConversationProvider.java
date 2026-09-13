package br.gov.interpretaai.server.provider;

import br.gov.interpretaai.server.api.VoiceTurnModels.NextAction;
import br.gov.interpretaai.server.api.VoiceTurnModels.PedagogicalReply;
import br.gov.interpretaai.server.api.VoiceTurnModels.Request;
import br.gov.interpretaai.server.api.VoiceTurnModels.VisualReaction;
import br.gov.interpretaai.server.core.ConversationProvider;
import br.gov.interpretaai.server.core.SafeFallbackConversationProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.google.genai.GoogleGenAiChatModel;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GeminiConversationProvider implements ConversationProvider {
    private static final String RULES = """
            Você é LEIA, mediadora brasileira de alfabetização para uma criança que ainda pode não ler.
            Responda em português brasileiro, em no máximo duas frases curtas e com apenas uma pergunta.
            Valorize esforço e contribuição. Nunca dê nota, diagnostique, use culpa, diga 'você errou'
            ou trate uma emoção como absolutamente certa. A criança ajuda a história a avançar.
            Responda somente JSON com replyText, visualReaction (CURIOUS|ENCOURAGE|CELEBRATE),
            nextAction (SPEAK_AGAIN|CONTINUE) e observationCategory (rótulo pedagógico neutro).
            """;

    private final GoogleGenAiChatModel model;
    private final ObjectMapper json;
    private final SafeFallbackConversationProvider fallback = new SafeFallbackConversationProvider();

    public GeminiConversationProvider(
            @Value("${interpretaai.gemini.api-key:}") String apiKey,
            @Value("${interpretaai.gemini.model:gemini-2.5-flash}") String modelName,
            ObjectMapper json) {
        this.json = json;
        this.model = apiKey.isBlank() ? null : GoogleGenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.2)
                .maxOutputTokens(180)
                .timeout(Duration.ofSeconds(5))
                .responseFormat(dev.langchain4j.model.chat.request.ResponseFormat.JSON)
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    @Override
    public PedagogicalReply reply(Request request, List<String> recentMessages) {
        if (model == null) return fallback.reply(request, recentMessages);
        String prompt = RULES + "\nCena: " + request.sceneId() + "\nTurno: " + request.turn()
                + " de 3\nContexto recente:\n" + String.join("\n", recentMessages)
                + "\nIdeia atual da criança: " + request.transcript();
        try {
            JsonNode node = json.readTree(model.chat(prompt));
            return new PedagogicalReply(
                    node.path("replyText").asText(),
                    VisualReaction.valueOf(node.path("visualReaction").asText("ENCOURAGE")),
                    NextAction.valueOf(node.path("nextAction").asText(request.turn() >= 3 ? "CONTINUE" : "SPEAK_AGAIN")),
                    node.path("observationCategory").asText("ORAL_EXPRESSION"));
        } catch (Exception error) {
            throw new IllegalStateException("Gemini indisponível ou resposta inválida", error);
        }
    }
}
