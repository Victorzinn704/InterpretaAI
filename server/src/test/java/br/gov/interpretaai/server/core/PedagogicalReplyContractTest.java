package br.gov.interpretaai.server.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gov.interpretaai.server.api.VoiceTurnModels.NextAction;
import br.gov.interpretaai.server.api.VoiceTurnModels.VisualReaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class PedagogicalReplyContractTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test void exposesAClosedNativeJsonSchema() {
        var root = PedagogicalReplyContract.RESPONSE_FORMAT.jsonSchema().rootElement();

        assertThat(root).isInstanceOf(dev.langchain4j.model.chat.request.json.JsonObjectSchema.class);
        var object = (dev.langchain4j.model.chat.request.json.JsonObjectSchema) root;
        assertThat(object.required()).containsExactlyInAnyOrder(
                "replyText", "visualReaction", "nextAction", "observationCategory");
        assertThat(object.additionalProperties()).isFalse();
    }

    @Test void decodesOnlyTheClosedPedagogicalVocabulary() throws Exception {
        var reply = PedagogicalReplyContract.decode(json, """
                {"replyText":"Boa ideia! Onde está a bola?","visualReaction":"CURIOUS",
                 "nextAction":"SPEAK_AGAIN","observationCategory":"CONTEXT_REASONING"}
                """, 1);

        assertThat(reply.visualReaction()).isEqualTo(VisualReaction.CURIOUS);
        assertThat(reply.nextAction()).isEqualTo(NextAction.SPEAK_AGAIN);
        assertThat(reply.observationCategory()).isEqualTo("CONTEXT_REASONING");
    }

    @Test void forcesTheThirdTurnToCloseAndRejectsUnknownCategories() throws Exception {
        var finalReply = PedagogicalReplyContract.decode(json, """
                {"replyText":"Você ajudou!","visualReaction":"CELEBRATE",
                 "nextAction":"SPEAK_AGAIN","observationCategory":"PARTICIPATION"}
                """, 3);
        assertThat(finalReply.nextAction()).isEqualTo(NextAction.CONTINUE);

        assertThatThrownBy(() -> PedagogicalReplyContract.decode(json, """
                {"replyText":"Texto","visualReaction":"ENCOURAGE",
                 "nextAction":"CONTINUE","observationCategory":"DIAGNOSIS"}
                """, 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
