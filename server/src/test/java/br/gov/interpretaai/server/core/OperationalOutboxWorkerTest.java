package br.gov.interpretaai.server.core;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class OperationalOutboxWorkerTest {
    @Test void retriesWithoutBlockingTheOriginalVoiceTurn() {
        OperationalOutboxStore store = mock(OperationalOutboxStore.class);
        OperationalEventSink sink = mock(OperationalEventSink.class);
        var event = new OperationalOutboxStore.Event(7, "VOICE_TURN_COMPLETED", 1);
        AtomicInteger claims = new AtomicInteger();
        when(store.claimNext(org.mockito.ArgumentMatchers.any(Instant.class),
                org.mockito.ArgumentMatchers.eq(Duration.ofSeconds(30))))
                .thenAnswer(ignored -> claims.getAndIncrement() == 0
                        ? Optional.of(event) : Optional.empty());
        org.mockito.Mockito.doThrow(new IllegalStateException("metrics offline"))
                .when(sink).deliver(event.type());

        new OperationalOutboxWorker(store, sink).drain();

        verify(store).retry(org.mockito.ArgumentMatchers.eq(event),
                org.mockito.ArgumentMatchers.any(Instant.class));
    }
}
