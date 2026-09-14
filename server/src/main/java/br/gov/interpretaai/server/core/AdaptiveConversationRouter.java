package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.VoiceTurnModels.PedagogicalReply;
import br.gov.interpretaai.server.api.VoiceTurnModels.Request;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/** Escolhe um único provedor saudável; failover só ocorre depois de falha rápida. */
@Component
@Primary
public class AdaptiveConversationRouter implements ConversationProvider {
    private static final Logger log = LoggerFactory.getLogger(AdaptiveConversationRouter.class);

    private final Map<String, RoutableConversationProvider> providers;
    private final List<String> routeOrder;
    private final boolean adaptive;
    private final long totalDeadlineMs;
    private final long fastFailoverMs;
    private final long failureCooldownMs;
    private final ConversationDeadline execution;
    private final MeterRegistry meters;
    private final Map<String, RouteHealth> health = new ConcurrentHashMap<>();

    public AdaptiveConversationRouter(
            List<RoutableConversationProvider> providerList,
            ConversationDeadline execution,
            MeterRegistry meters,
            @Value("${interpretaai.conversation.provider:ollama}") String selectedProvider,
            @Value("${interpretaai.conversation.route:ollama}") String configuredRoute,
            @Value("${interpretaai.conversation.routing-deadline-ms:4000}") long totalDeadlineMs,
            @Value("${interpretaai.conversation.fast-failover-ms:350}") long fastFailoverMs,
            @Value("${interpretaai.conversation.failure-cooldown-ms:5000}") long failureCooldownMs) {
        this.execution = execution;
        this.meters = meters;
        this.totalDeadlineMs = totalDeadlineMs;
        this.fastFailoverMs = fastFailoverMs;
        this.failureCooldownMs = failureCooldownMs;
        this.providers = new LinkedHashMap<>();
        providerList.forEach(provider -> {
            if (providers.put(provider.providerId(), provider) != null) {
                throw new IllegalStateException("Provedor duplicado: " + provider.providerId());
            }
        });
        String normalizedProvider = selectedProvider.trim().toLowerCase(Locale.ROOT);
        this.adaptive = "adaptive".equals(normalizedProvider);
        this.routeOrder = adaptive
                ? Arrays.stream(configuredRoute.split(",")).map(String::trim)
                        .map(value -> value.toLowerCase(Locale.ROOT))
                        .filter(value -> !value.isBlank()).distinct().toList()
                : List.of(normalizedProvider);
        if (totalDeadlineMs < 1 || fastFailoverMs < 0 || fastFailoverMs > totalDeadlineMs
                || failureCooldownMs < 0) {
            throw new IllegalArgumentException("orçamento total e failover rápido são inválidos");
        }
        if (routeOrder.isEmpty() || routeOrder.stream().anyMatch(id -> !providers.containsKey(id))) {
            throw new IllegalArgumentException("Rota de conversa contém provedor desconhecido: " + routeOrder);
        }
    }

    @Override
    public PedagogicalReply reply(Request request, List<String> recentMessages) {
        long routeStarted = System.nanoTime();
        RuntimeException lastError = new IllegalStateException("Nenhum provedor disponível");
        for (String providerId : candidates()) {
            RoutableConversationProvider provider = providers.get(providerId);
            RouteHealth route = health.computeIfAbsent(providerId, ignored -> new RouteHealth());
            if (!provider.available() || route.coolingDown()
                    || "OPEN".equals(execution.circuitState(providerId))) continue;
            long elapsedMs = elapsedMs(routeStarted);
            long remainingMs = totalDeadlineMs - elapsedMs;
            if (remainingMs <= 0) break;
            long attemptStarted = System.nanoTime();
            try {
                PedagogicalReply reply = execution.call(providerId, remainingMs,
                        () -> provider.reply(request, recentMessages));
                record(providerId, attemptStarted, true);
                log.info("conversation_route provider={} outcome=success duration_ms={}",
                        providerId, elapsedMs(attemptStarted));
                return reply;
            } catch (RuntimeException error) {
                lastError = error;
                record(providerId, attemptStarted, false);
                log.warn("conversation_route provider={} outcome=failure duration_ms={}",
                        providerId, elapsedMs(attemptStarted));
                if (!(error instanceof CallNotPermittedException)
                        && elapsedMs(routeStarted) > fastFailoverMs) break;
            }
        }
        throw lastError;
    }

    public Map<String, RouteSnapshot> snapshots() {
        Map<String, RouteSnapshot> result = new LinkedHashMap<>();
        routeOrder.forEach(id -> {
            RouteHealth route = health.computeIfAbsent(id, ignored -> new RouteHealth());
            boolean available = providers.get(id).available();
            String circuit = execution.circuitState(id);
            result.put(id, new RouteSnapshot(
                    available, available && !route.coolingDown() && !"OPEN".equals(circuit), circuit,
                    route.samples.get(), route.failures.get(), route.ewmaMillis()));
        });
        return result;
    }

    public List<String> configuredRoute() {
        return routeOrder;
    }

    private List<String> candidates() {
        if (!adaptive) return routeOrder;
        List<String> candidates = new ArrayList<>(routeOrder);
        candidates.sort(Comparator
                .comparingInt((String id) -> health.computeIfAbsent(id, ignored -> new RouteHealth()).samples.get() == 0 ? 0 : 1)
                .thenComparingLong(id -> health.get(id).scoreMicros())
                .thenComparingInt(routeOrder::indexOf));
        return candidates;
    }

    private void record(String providerId, long started, boolean success) {
        long nanos = System.nanoTime() - started;
        RouteHealth route = health.computeIfAbsent(providerId, ignored -> new RouteHealth());
        route.record(TimeUnit.NANOSECONDS.toMicros(nanos), success, failureCooldownMs);
        Timer.builder("interpretaai.conversation.provider")
                .tags("provider", providerId, "outcome", success ? "success" : "failure")
                .serviceLevelObjectives(Duration.ofMillis(500), Duration.ofMillis(1500), Duration.ofMillis(4000))
                .register(meters).record(nanos, TimeUnit.NANOSECONDS);
    }

    private long elapsedMs(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }

    public record RouteSnapshot(
            boolean available, boolean eligible, String circuit,
            int samples, int failures, long ewmaMs) {}

    private static final class RouteHealth {
        private final AtomicInteger samples = new AtomicInteger();
        private final AtomicInteger failures = new AtomicInteger();
        private final AtomicInteger consecutiveFailures = new AtomicInteger();
        private final AtomicLong ewmaMicros = new AtomicLong();
        private final AtomicLong cooldownUntilNanos = new AtomicLong();

        void record(long durationMicros, boolean success, long cooldownMs) {
            samples.incrementAndGet();
            if (success) {
                consecutiveFailures.set(0);
                cooldownUntilNanos.set(0);
            } else {
                failures.incrementAndGet();
                consecutiveFailures.incrementAndGet();
                cooldownUntilNanos.set(System.nanoTime()
                        + TimeUnit.MILLISECONDS.toNanos(cooldownMs));
            }
            ewmaMicros.updateAndGet(previous -> previous == 0
                    ? durationMicros : Math.round(previous * 0.75 + durationMicros * 0.25));
        }

        long scoreMicros() {
            return (ewmaMicros.get() == 0 ? 1_500_000 : ewmaMicros.get())
                    + consecutiveFailures.get() * 500_000L;
        }

        long ewmaMillis() {
            return TimeUnit.MICROSECONDS.toMillis(ewmaMicros.get());
        }

        boolean coolingDown() {
            return System.nanoTime() < cooldownUntilNanos.get();
        }
    }
}
