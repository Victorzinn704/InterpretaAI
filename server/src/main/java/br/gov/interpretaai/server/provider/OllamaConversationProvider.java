package br.gov.interpretaai.server.provider;

import br.gov.interpretaai.server.api.VoiceTurnModels.NextAction;
import br.gov.interpretaai.server.api.VoiceTurnModels.PedagogicalReply;
import br.gov.interpretaai.server.api.VoiceTurnModels.Request;
import br.gov.interpretaai.server.api.VoiceTurnModels.VisualReaction;
import br.gov.interpretaai.server.core.ConversationProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.ollama.OllamaChatModel;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "interpretaai.conversation.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaConversationProvider implements ConversationProvider {
    private static final String RULES = """
            Você é LEIA, mediadora brasileira de alfabetização para uma criança que ainda pode não ler.
            Responda em português brasileiro, em no máximo duas frases curtas e com apenas uma pergunta.
            Valorize a ação, o esforço e a contribuição; nunca rotule a inteligência com 'esperto'.
            Nunca dê nota, diagnostique, use culpa, diga 'você errou' ou trate uma emoção como
            absolutamente certa. Não peça nome, escola ou dado pessoal. Nos turnos 1 e 2 faça
            exatamente uma pergunta; no turno 3 conclua sem abrir uma nova tarefa.
            A criança ajuda a história a avançar.
            Responda somente JSON com replyText, visualReaction (CURIOUS|ENCOURAGE|CELEBRATE),
            nextAction (SPEAK_AGAIN|CONTINUE) e observationCategory (rótulo pedagógico neutro).
            """;

    private final OllamaChatModel model;
    private final ObjectMapper json;

    public OllamaConversationProvider(
            @Value("${interpretaai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${interpretaai.ollama.model:qwen2.5:1.5b}") String modelName,
            ObjectMapper json) {
        this.json = json;
        this.model = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.2)
                .timeout(Duration.ofSeconds(12))
                .responseFormat(ResponseFormat.JSON)
                .build();
    }

    @Override
    public PedagogicalReply reply(Request request, List<String> recentMessages) {
        String prompt = RULES + "\nCena: " + request.sceneId() + "\nTurno: " + request.turn()
                + " de 3\nContexto recente:\n" + String.join("\n", recentMessages)
                + "\nIdeia atual da criança: " + request.transcript();
        try {
            JsonNode node = json.readTree(model.chat(prompt));
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
