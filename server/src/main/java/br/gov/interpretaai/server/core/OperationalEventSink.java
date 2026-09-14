package br.gov.interpretaai.server.core;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class OperationalEventSink {
    private final MeterRegistry meters;

    public OperationalEventSink(MeterRegistry meters) {
        this.meters = meters;
    }

    public void deliver(String eventType) {
        meters.counter("interpretaai.outbox.delivered", "event", eventType).increment();
    }
}
