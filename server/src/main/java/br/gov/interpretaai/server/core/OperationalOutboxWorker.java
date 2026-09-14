package br.gov.interpretaai.server.core;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OperationalOutboxWorker {
    private static final Logger log = LoggerFactory.getLogger(OperationalOutboxWorker.class);
    private static final int BATCH_SIZE = 8;
    private static final Duration LEASE = Duration.ofSeconds(30);

    private final OperationalOutboxStore store;
    private final OperationalEventSink sink;

    public OperationalOutboxWorker(OperationalOutboxStore store, OperationalEventSink sink) {
        this.store = store;
        this.sink = sink;
    }

    @Scheduled(fixedDelayString = "${interpretaai.outbox.poll-ms:500}")
    public void drain() {
        for (int index = 0; index < BATCH_SIZE; index++) {
            var event = store.claimNext(Instant.now(), LEASE);
            if (event.isEmpty()) return;
            try {
                sink.deliver(event.get().type());
                store.delivered(event.get().id());
            } catch (RuntimeException error) {
                store.retry(event.get(), Instant.now());
                log.warn("outbox_delivery_failed event={} attempt={}",
                        event.get().type(), event.get().attempts() + 1);
            }
        }
    }
}
