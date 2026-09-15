package br.gov.interpretaai.server.core;

import static br.gov.interpretaai.server.api.VoiceTurnModels.Speaker.DAVI_MALE;
import static br.gov.interpretaai.server.api.VoiceTurnModels.Speaker.LEIA_FEMALE;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SpeechSynthesisServiceTest {
    @Test
    void reusesAudioForTheSameValidatedTextAndVoice() {
        AtomicInteger calls = new AtomicInteger();
        SpeechProvider provider = (text, speaker) -> {
            calls.incrementAndGet();
            return new SpeechProvider.SpeechAudio(text.getBytes(StandardCharsets.UTF_8), "audio/ogg");
        };
        SpeechSynthesisService service = new SpeechSynthesisService(provider);

        var first = service.synthesize("Vamos procurar juntos!", LEIA_FEMALE);
        var second = service.synthesize("Vamos procurar juntos!", LEIA_FEMALE);

        assertThat(second.content()).isEqualTo(first.content());
        assertThat(calls).hasValue(1);
        assertThat(service.estimatedEntries()).isEqualTo(1);
    }

    @Test
    void separatesVoicesAndDoesNotCacheSilentFallback() {
        AtomicInteger calls = new AtomicInteger();
        SpeechProvider provider = (text, speaker) -> {
            calls.incrementAndGet();
            return text.startsWith("silêncio")
                    ? SpeechProvider.SpeechAudio.silent()
                    : new SpeechProvider.SpeechAudio(new byte[] {1}, "audio/ogg");
        };
        SpeechSynthesisService service = new SpeechSynthesisService(provider);

        service.synthesize("Oi", LEIA_FEMALE);
        service.synthesize("Oi", DAVI_MALE);
        service.synthesize("silêncio temporário", LEIA_FEMALE);
        service.synthesize("silêncio temporário", LEIA_FEMALE);

        assertThat(calls).hasValue(4);
        assertThat(service.estimatedEntries()).isEqualTo(2);
    }

    @Test
    void coalescesConcurrentRequestsForTheSameAudio() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        SpeechProvider provider = (text, speaker) -> {
            calls.incrementAndGet();
            entered.countDown();
            try {
                if (!release.await(1, TimeUnit.SECONDS)) throw new IllegalStateException("timeout");
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(error);
            }
            return new SpeechProvider.SpeechAudio(new byte[] {1, 2, 3}, "audio/ogg");
        };
        SpeechSynthesisService service = new SpeechSynthesisService(provider);
        var executor = Executors.newFixedThreadPool(6);
        try {
            List<CompletableFuture<SpeechProvider.SpeechAudio>> callsInFlight =
                    java.util.stream.IntStream.range(0, 6)
                            .mapToObj(ignored -> CompletableFuture.supplyAsync(
                                    () -> service.synthesize("Resposta aprovada", LEIA_FEMALE), executor))
                            .toList();
            assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
            release.countDown();
            CompletableFuture.allOf(callsInFlight.toArray(CompletableFuture[]::new)).join();

            assertThat(calls).hasValue(1);
            assertThat(callsInFlight).allSatisfy(call ->
                    assertThat(call.join().content()).containsExactly(1, 2, 3));
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }
}
