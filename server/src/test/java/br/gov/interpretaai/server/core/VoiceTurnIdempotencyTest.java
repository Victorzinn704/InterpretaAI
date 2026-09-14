package br.gov.interpretaai.server.core;

import static br.gov.interpretaai.server.api.VoiceTurnModels.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class VoiceTurnIdempotencyTest {
    @Test void executesOnlyOnceAndReplaysFromMemory() {
        VoiceTurnStore store = mock(VoiceTurnStore.class);
        when(store.find("turn-0001")).thenReturn(Optional.empty());
        when(store.claim(eq("turn-0001"), any(), any(), any())).thenReturn(true);
        VoiceTurnIdempotency idempotency = service(store);
        AtomicInteger calls = new AtomicInteger();

        Response first = idempotency.execute("turn-0001", request("scene"),
                () -> { calls.incrementAndGet(); return response("primeira"); });
        Response replay = idempotency.execute("turn-0001", request("scene"),
                () -> { calls.incrementAndGet(); return response("segunda"); });

        assertThat(first.replyText()).isEqualTo("primeira");
        assertThat(replay.replyText()).isEqualTo("primeira");
        assertThat(calls).hasValue(1);
        verify(store).complete(eq("turn-0001"), any(), any());
    }

    @Test void rejectsReuseForAnotherStage() {
        VoiceTurnStore store = mock(VoiceTurnStore.class);
        when(store.find("turn-0002")).thenReturn(Optional.empty());
        when(store.claim(eq("turn-0002"), any(), any(), any())).thenReturn(true);
        VoiceTurnIdempotency idempotency = service(store);
        idempotency.execute("turn-0002", request("scene-a"), () -> response("ok"));

        assertThatThrownBy(() -> idempotency.execute(
                "turn-0002", request("scene-b"), () -> response("não executar")))
                .isInstanceOf(VoiceTurnIdempotency.IdempotencyConflictException.class);
    }

    @Test void keepsServingAndCachesWhenDatabaseFallsAfterStartup() {
        VoiceTurnStore store = mock(VoiceTurnStore.class);
        when(store.find("turn-0003"))
                .thenThrow(new DataAccessResourceFailureException("database offline"));
        VoiceTurnIdempotency idempotency = service(store);
        AtomicInteger calls = new AtomicInteger();

        Response first = idempotency.execute("turn-0003", request("scene"),
                () -> { calls.incrementAndGet(); return response("continua local"); });
        Response replay = idempotency.execute("turn-0003", request("scene"),
                () -> { calls.incrementAndGet(); return response("não executar"); });

        assertThat(first.replyText()).isEqualTo("continua local");
        assertThat(replay.replyText()).isEqualTo("continua local");
        assertThat(calls).hasValue(1);
    }

    private VoiceTurnIdempotency service(VoiceTurnStore store) {
        return new VoiceTurnIdempotency(store, new ObjectMapper(), Clock.systemUTC(),
                Duration.ofMinutes(10), Duration.ofSeconds(30));
    }

    private Request request(String scene) {
        return new Request("session-1", scene, 1, "Minha ideia",
                Speaker.LEIA_FEMALE, false);
    }

    private Response response(String text) {
        return new Response(text, Speaker.LEIA_FEMALE, "", "",
                VisualReaction.ENCOURAGE, NextAction.SPEAK_AGAIN,
                "ORAL_EXPRESSION", false);
    }
}
