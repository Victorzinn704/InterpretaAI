package br.gov.interpretaai.server.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ConversationDeadlineTest {
    @Test void opensCircuitAndRejectsWithoutCallingUnhealthyProvider() {
        ConversationDeadline execution = new ConversationDeadline(100, 1);
        AtomicInteger calls = new AtomicInteger();

        for (int attempt = 0; attempt < 4; attempt++) {
            assertThatThrownBy(() -> execution.call(() -> {
                calls.incrementAndGet();
                throw new IllegalStateException("provider offline");
            })).isInstanceOf(IllegalStateException.class);
        }

        assertThat(execution.circuitState()).isEqualTo("OPEN");
        assertThatThrownBy(() -> execution.call(() -> {
            calls.incrementAndGet();
            return "não executar";
        })).isInstanceOf(CallNotPermittedException.class);
        assertThat(calls).hasValue(4);
        execution.close();
    }
}
