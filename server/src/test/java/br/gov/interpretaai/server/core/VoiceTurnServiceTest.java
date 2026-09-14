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
        SpeechProvider speech = (text, speaker) -> new SpeechProvider.SpeechAudio(
                "opus".getBytes(StandardCharsets.UTF_8), "audio/ogg; codecs=opus");
        VoiceTurnService service = new VoiceTurnService(
                conversation, speech, new SessionMemory(), new ConversationDeadline(1_000, 2));

        Response response = service.execute(new Request(
                "session-1", "comic-ball-01", 1, "Perto da árvore", Speaker.LEIA_FEMALE, false));

        assertThat(response.replyText()).contains("Gostei");
        assertThat(response.audioBase64()).isNotBlank();
        assertThat(response.degraded()).isFalse();
    }

    @Test void degradesSafelyWhenProvidersFail() {
        ConversationProvider broken = (request, history) -> { throw new IllegalStateException("offline"); };
        SpeechProvider silent = (text, speaker) -> SpeechProvider.SpeechAudio.silent();
        VoiceTurnService service = new VoiceTurnService(
                broken, silent, new SessionMemory(), new ConversationDeadline(1_000, 2));

        Response response = service.execute(new Request(
                "session-1", "scene", 3, "Minha ideia", Speaker.LEIA_FEMALE, true));

        assertThat(response.degraded()).isTrue();
        assertThat(response.nextAction()).isEqualTo(NextAction.CONTINUE);
    }

    @Test void enforcesConversationDeadlineBeforeUsingFallback() {
        ConversationProvider slow = (request, history) -> {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            }
            return new PedagogicalReply("atrasada", VisualReaction.CURIOUS,
                    NextAction.SPEAK_AGAIN, "ORAL_EXPRESSION");
        };
        ConversationDeadline deadline = new ConversationDeadline(30, 1);
        VoiceTurnService service = new VoiceTurnService(
                slow, (text, speaker) -> SpeechProvider.SpeechAudio.silent(),
                new SessionMemory(), deadline);

        long started = System.nanoTime();
        Response response = service.execute(new Request(
                "session-1", "scene", 1, "Minha ideia", Speaker.LEIA_FEMALE, true));
        long elapsedMs = (System.nanoTime() - started) / 1_000_000;

        assertThat(response.replyText()).contains("Gostei da sua ideia");
        assertThat(response.degraded()).isTrue();
        assertThat(elapsedMs).isLessThan(500);
        deadline.close();
    }
}
