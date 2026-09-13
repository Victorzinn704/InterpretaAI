package br.gov.interpretaai.server.core;

import static br.gov.interpretaai.server.api.VoiceTurnModels.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class VoiceTurnServiceTest {
    @Test void returnsStructuredAudioResponseWithoutPersistingPayloadInLogs() {
        ConversationProvider conversation = (request, history) -> new PedagogicalReply(
                "Gostei da sua ideia! Onde a bola pode estar?",
                VisualReaction.CURIOUS, NextAction.SPEAK_AGAIN, "CONTEXT_REASONING");
        SpeechProvider speech = (text, speaker) -> "opus".getBytes(StandardCharsets.UTF_8);
        VoiceTurnService service = new VoiceTurnService(conversation, speech, new SessionMemory());

        Response response = service.execute(new Request(
                "session-1", "comic-ball-01", 1, "Perto da árvore", Speaker.LEIA_FEMALE, false));

        assertThat(response.replyText()).contains("Gostei");
        assertThat(response.audioBase64()).isNotBlank();
        assertThat(response.degraded()).isFalse();
    }

    @Test void degradesSafelyWhenProvidersFail() {
        ConversationProvider broken = (request, history) -> { throw new IllegalStateException("offline"); };
        SpeechProvider silent = (text, speaker) -> new byte[0];
        VoiceTurnService service = new VoiceTurnService(broken, silent, new SessionMemory());

        Response response = service.execute(new Request(
                "session-1", "scene", 3, "Minha ideia", Speaker.LEIA_FEMALE, true));

        assertThat(response.degraded()).isTrue();
        assertThat(response.nextAction()).isEqualTo(NextAction.CONTINUE);
    }
}
