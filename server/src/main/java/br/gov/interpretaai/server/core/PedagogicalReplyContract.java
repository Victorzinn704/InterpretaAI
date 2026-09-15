package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.VoiceTurnModels.NextAction;
import br.gov.interpretaai.server.api.VoiceTurnModels.PedagogicalReply;
import br.gov.interpretaai.server.api.VoiceTurnModels.VisualReaction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import java.util.List;

/** Contrato pequeno e idêntico entre os modelos; a UI nunca recebe JSON livre. */
public final class PedagogicalReplyContract {
    private static final List<String> OBSERVATIONS = List.of(
            "ORAL_EXPRESSION", "CONTEXT_REASONING", "PARTICIPATION");

    public static final ResponseFormat RESPONSE_FORMAT = ResponseFormat.builder()
            .type(ResponseFormatType.JSON)
            .jsonSchema(JsonSchema.builder()
                    .name("PedagogicalReply")
                    .rootElement(JsonObjectSchema.builder()
                            .addStringProperty("replyText", "Até duas frases curtas em português brasileiro")
                            .addEnumProperty("visualReaction", List.of("CURIOUS", "ENCOURAGE", "CELEBRATE"))
                            .addEnumProperty("nextAction",
                                    List.of("SPEAK_AGAIN", "CONTINUE"),
                                    "SPEAK_AGAIN somente quando replyText termina com uma pergunta; "
                                            + "CONTINUE quando o objetivo foi cumprido e replyText não tem pergunta")
                            .addEnumProperty("observationCategory", OBSERVATIONS)
                            .required("replyText", "visualReaction", "nextAction", "observationCategory")
                            .additionalProperties(false)
                            .build())
                    .build())
            .build();

    private PedagogicalReplyContract() {}

    public static ChatRequest request(String prompt) {
        return ChatRequest.builder()
                .messages(UserMessage.from(prompt))
                .responseFormat(RESPONSE_FORMAT)
                .build();
    }

    public static PedagogicalReply decode(ObjectMapper json, String content, int turn) throws Exception {
        JsonNode node = json.readTree(content);
        String replyText = requiredText(node, "replyText");
        VisualReaction reaction = VisualReaction.valueOf(requiredText(node, "visualReaction"));
        NextAction proposed = NextAction.valueOf(requiredText(node, "nextAction"));
        String observation = requiredText(node, "observationCategory");
        if (!OBSERVATIONS.contains(observation)) {
            throw new IllegalArgumentException("Categoria pedagógica fora do contrato");
        }
        return new PedagogicalReply(
                replyText,
                reaction,
                turn >= 3 ? NextAction.CONTINUE : proposed,
                observation);
    }

    private static String requiredText(JsonNode node, String field) {
        String value = node.path(field).asText("").trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Campo ausente: " + field);
        return value;
    }
}
