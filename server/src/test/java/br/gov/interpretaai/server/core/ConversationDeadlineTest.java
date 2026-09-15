package br.gov.interpretaai.server.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ConversationDeadlineTest {
    @Test void rejectsImmediatelyInsteadOfQueueingBehindAnOccupiedProviderSlot() throws Exception {
        ConversationDeadline execution = new ConversationDeadline(500, 1);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger queuedOperationCalls = new AtomicInteger();
        CompletableFuture<String> occupied = CompletableFuture.supplyAsync(() -> execution.call("hot", 500, () -> {
            entered.countDown();
            try {
                if (!release.await(1, TimeUnit.SECONDS)) throw new IllegalStateException("teste bloqueado");
                return "pronto";
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(error);
            }
        }));

        assertThat(entered.await(1, TimeUnit.SECONDS)).isTrue();
        long started = System.nanoTime();
        assertThatThrownBy(() -> execution.call("other", 500, () -> {
            queuedOperationCalls.incrementAndGet();
            return "não executar";
        })).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Capacidade");
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        assertThat(elapsedMs).isLessThan(200);
        assertThat(queuedOperationCalls).hasValue(0);
        release.countDown();
        assertThat(occupied.get(1, TimeUnit.SECONDS)).isEqualTo("pronto");
        execution.close();
    }

    @Test void cancelsAProviderCallAtTheDeclaredDeadline() throws Exception {
        ConversationDeadline execution = new ConversationDeadline(40, 1);
        CountDownLatch interrupted = new CountDownLatch(1);
        long started = System.nanoTime();

        assertThatThrownBy(() -> execution.call("slow", 40, () -> {
            try {
                Thread.sleep(5_000);
                return "atrasado";
            } catch (InterruptedException error) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
                throw new IllegalStateException(error);
            }
        })).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Prazo");

        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)).isLessThan(400);
        assertThat(interrupted.await(1, TimeUnit.SECONDS)).isTrue();
        execution.close();
    }

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
        int providerCallsBeforeProbe = calls.get();
        assertThatThrownBy(() -> execution.call(() -> {
            calls.incrementAndGet();
            return "não executar";
        })).isInstanceOf(CallNotPermittedException.class);
        assertThat(calls).hasValue(providerCallsBeforeProbe);
        assertThat(providerCallsBeforeProbe).isBetween(1, 4);
        execution.close();
    }
}
