package br.gov.interpretaai.server.core;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Impõe prazo total e concorrência limitada às chamadas de conversa remota. */
@Component
public class ConversationDeadline {
    private final long timeoutMs;
    private final ThreadPoolExecutor executor;
    private final CircuitBreaker circuit;

    @Autowired
    public ConversationDeadline(
            @Value("${interpretaai.conversation.deadline-ms:4000}") long timeoutMs,
            @Value("${interpretaai.conversation.max-concurrent:2}") int maxConcurrent,
            @Value("${interpretaai.conversation.circuit.minimum-calls:4}") int minimumCalls,
            @Value("${interpretaai.conversation.circuit.window-size:8}") int windowSize,
            @Value("${interpretaai.conversation.circuit.failure-rate:50}") float failureRate,
            @Value("${interpretaai.conversation.circuit.slow-rate:50}") float slowRate,
            @Value("${interpretaai.conversation.circuit.slow-ms:2500}") long slowMs,
            @Value("${interpretaai.conversation.circuit.open-seconds:20}") long openSeconds) {
        if (timeoutMs < 1 || maxConcurrent < 1) {
            throw new IllegalArgumentException("deadline e concorrência devem ser positivos");
        }
        this.timeoutMs = timeoutMs;
        this.circuit = CircuitBreaker.of("conversation", CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(windowSize)
                .minimumNumberOfCalls(minimumCalls)
                .failureRateThreshold(failureRate)
                .slowCallRateThreshold(slowRate)
                .slowCallDurationThreshold(Duration.ofMillis(slowMs))
                .waitDurationInOpenState(Duration.ofSeconds(openSeconds))
                .permittedNumberOfCallsInHalfOpenState(1)
                .build());
        AtomicInteger threadNumber = new AtomicInteger();
        this.executor = new ThreadPoolExecutor(
                maxConcurrent,
                maxConcurrent,
                0,
                TimeUnit.MILLISECONDS,
                new SynchronousQueue<>(),
                runnable -> {
                    Thread thread = new Thread(runnable,
                            "leia-conversation-" + threadNumber.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy());
    }

    ConversationDeadline(long timeoutMs, int maxConcurrent) {
        this(timeoutMs, maxConcurrent, 4, 8, 50, 50, 2_500, 20);
    }

    public <T> T call(Supplier<T> operation) {
        if (!circuit.tryAcquirePermission()) {
            throw CallNotPermittedException.createCallNotPermittedException(circuit);
        }
        long started = System.nanoTime();
        Future<T> future;
        try {
            future = executor.submit(operation::get);
        } catch (RejectedExecutionException error) {
            circuit.onError(System.nanoTime() - started, TimeUnit.NANOSECONDS, error);
            throw new IllegalStateException("Capacidade de conversa temporariamente esgotada", error);
        }
        try {
            T response = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            circuit.onSuccess(System.nanoTime() - started, TimeUnit.NANOSECONDS);
            return response;
        } catch (TimeoutException error) {
            future.cancel(true);
            circuit.onError(System.nanoTime() - started, TimeUnit.NANOSECONDS, error);
            throw new IllegalStateException("Prazo da conversa excedido", error);
        } catch (InterruptedException error) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            circuit.onError(System.nanoTime() - started, TimeUnit.NANOSECONDS, error);
            throw new IllegalStateException("Conversa interrompida", error);
        } catch (ExecutionException error) {
            Throwable cause = error.getCause();
            circuit.onError(System.nanoTime() - started, TimeUnit.NANOSECONDS, cause);
            if (cause instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Falha na conversa", cause);
        }
    }

    public String circuitState() {
        return circuit.getState().name();
    }

    @PreDestroy
    void close() {
        executor.shutdownNow();
    }
}
