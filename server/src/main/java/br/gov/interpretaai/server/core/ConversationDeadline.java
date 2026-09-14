package br.gov.interpretaai.server.core;

import jakarta.annotation.PreDestroy;
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

    @Autowired
    public ConversationDeadline(
            @Value("${interpretaai.conversation.deadline-ms:4000}") long timeoutMs,
            @Value("${interpretaai.conversation.max-concurrent:2}") int maxConcurrent) {
        if (timeoutMs < 1 || maxConcurrent < 1) {
            throw new IllegalArgumentException("deadline e concorrência devem ser positivos");
        }
        this.timeoutMs = timeoutMs;
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

    public <T> T call(Supplier<T> operation) {
        Future<T> future;
        try {
            future = executor.submit(operation::get);
        } catch (RejectedExecutionException error) {
            throw new IllegalStateException("Capacidade de conversa temporariamente esgotada", error);
        }
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException error) {
            future.cancel(true);
            throw new IllegalStateException("Prazo da conversa excedido", error);
        } catch (InterruptedException error) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Conversa interrompida", error);
        } catch (ExecutionException error) {
            if (error.getCause() instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Falha na conversa", error.getCause());
        }
    }

    @PreDestroy
    void close() {
        executor.shutdownNow();
    }
}
