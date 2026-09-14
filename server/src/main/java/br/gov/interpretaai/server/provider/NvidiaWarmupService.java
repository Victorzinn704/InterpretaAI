package br.gov.interpretaai.server.provider;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Mantém apenas a rota de inferência aquecida; nunca envia conteúdo de usuário. */
@Component
@ConditionalOnExpression("'${interpretaai.conversation.provider:ollama}' == 'nvidia' || "
        + "('${interpretaai.conversation.provider:ollama}' == 'adaptive' && "
        + "'${interpretaai.conversation.route:ollama}'.contains('nvidia'))")
public class NvidiaWarmupService {
    private static final Logger log = LoggerFactory.getLogger(NvidiaWarmupService.class);
    private static final long ON_DEMAND_COOLDOWN_MS = 30_000;

    private final NvidiaConversationProvider provider;
    private final TaskScheduler scheduler;
    private final boolean enabled;
    private final AtomicBoolean warming = new AtomicBoolean(false);
    private final AtomicLong nextOnDemandAt = new AtomicLong(0);

    public NvidiaWarmupService(
            NvidiaConversationProvider provider,
            TaskScheduler scheduler,
            @Value("${interpretaai.nvidia.warmup-enabled:true}") boolean enabled) {
        this.provider = provider;
        this.scheduler = scheduler;
        this.enabled = enabled;
    }

    @Scheduled(
            initialDelayString = "${interpretaai.nvidia.warmup-initial-delay-ms:250}",
            fixedDelayString = "${interpretaai.nvidia.warmup-interval-ms:120000}")
    public void keepWarm() {
        if (!enabled || !warming.compareAndSet(false, true)) return;
        long started = System.nanoTime();
        try {
            boolean ready = provider.warmUp();
            long durationMs = (System.nanoTime() - started) / 1_000_000;
            log.info("nvidia_warmup model={} ready={} duration_ms={}",
                    provider.activeModelId(), ready, durationMs);
        } finally {
            warming.set(false);
        }
    }

    public boolean requestWarmup() {
        if (!enabled || provider.isWarm() || warming.get()) return false;
        long now = System.currentTimeMillis();
        long next = nextOnDemandAt.get();
        if (now < next || !nextOnDemandAt.compareAndSet(next, now + ON_DEMAND_COOLDOWN_MS)) {
            return false;
        }
        scheduler.schedule(this::keepWarm, Instant.now());
        return true;
    }

    public boolean isWarm() {
        return provider.isWarm();
    }

    public String activeModelId() {
        return provider.activeModelId();
    }
}
