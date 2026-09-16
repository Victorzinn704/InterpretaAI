package br.gov.interpretaai.server.provider;

import br.gov.interpretaai.server.core.AdaptiveConversationRouter;
import br.gov.interpretaai.server.core.ScenePackCatalog;
import br.gov.interpretaai.server.core.SpeechSynthesisService;
import br.gov.interpretaai.server.core.WarmableConversationProvider;
import br.gov.interpretaai.server.api.VoiceTurnModels.Speaker;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Aquece somente provedores presentes na rota, sempre com conteúdo sintético. */
@Component
@ConditionalOnExpression("'${interpretaai.conversation.provider:ollama}' == 'ollama' || "
        + "'${interpretaai.conversation.provider:ollama}' == 'nvidia' || "
        + "'${interpretaai.conversation.provider:ollama}' == 'gemini' || "
        + "('${interpretaai.conversation.provider:ollama}' == 'adaptive' && ("
        + "'${interpretaai.conversation.route:ollama}'.contains('ollama') || "
        + "'${interpretaai.conversation.route:ollama}'.contains('nvidia') || "
        + "'${interpretaai.conversation.route:ollama}'.contains('gemini')))")
public class ProviderWarmupService {
    private static final Logger log = LoggerFactory.getLogger(ProviderWarmupService.class);
    private static final long ON_DEMAND_COOLDOWN_MS = 30_000;
    private static final long FAILURE_COOLDOWN_MS = 2_000;

    private final Map<String, WarmableConversationProvider> providers;
    private final TaskScheduler scheduler;
    private final SpeechSynthesisService speech;
    private final ScenePackCatalog scenes;
    private final AtomicBoolean warming = new AtomicBoolean(false);
    private final AtomicLong nextOnDemandAt = new AtomicLong(0);

    public ProviderWarmupService(
            List<WarmableConversationProvider> providerList,
            AdaptiveConversationRouter router,
            TaskScheduler scheduler,
            SpeechSynthesisService speech,
            ScenePackCatalog scenes) {
        Map<String, WarmableConversationProvider> configured = new LinkedHashMap<>();
        Map<String, WarmableConversationProvider> available = new LinkedHashMap<>();
        providerList.forEach(provider -> available.put(provider.providerId(), provider));
        router.configuredRoute().forEach(providerId -> {
            WarmableConversationProvider provider = available.get(providerId);
            if (provider != null) configured.put(providerId, provider);
        });
        this.providers = Collections.unmodifiableMap(configured);
        this.scheduler = scheduler;
        this.speech = speech;
        this.scenes = scenes;
    }

    @Scheduled(
            initialDelayString = "${interpretaai.remote-warmup.initial-delay-ms:250}",
            fixedDelayString = "${interpretaai.remote-warmup.interval-ms:120000}")
    public void keepWarm() {
        if (System.currentTimeMillis() < nextOnDemandAt.get()) return;
        if (!warming.compareAndSet(false, true)) return;
        // A sondagem agendada renova inclusive provedores ainda marcados como quentes.
        // Caso contrario, TTL=150 s + intervalo=120 s criaria uma janela fria entre ciclos.
        runWarmup(true);
    }

    private void runWarmup(boolean refreshHealthyProviders) {
        try {
            // A fala preparada protege a experiência infantil mesmo quando uma sonda remota demora.
            // Por isso ela aquece antes dos modelos opcionais, nunca depois deles.
            warmPreparedSpeech();
            boolean anyReady = false;
            for (WarmableConversationProvider provider : providers.values()) {
                if (!refreshHealthyProviders && provider.isWarm()) {
                    anyReady = true;
                    continue;
                }
                long started = System.nanoTime();
                boolean ready = provider.warmUp();
                anyReady |= ready;
                long durationMs = (System.nanoTime() - started) / 1_000_000;
                log.info("provider_warmup provider={} model={} ready={} duration_ms={}",
                        provider.providerId(), provider.activeModelId(), ready, durationMs);
            }
            nextOnDemandAt.set(System.currentTimeMillis()
                    + (anyReady ? ON_DEMAND_COOLDOWN_MS : FAILURE_COOLDOWN_MS));
        } finally {
            warming.set(false);
        }
    }

    private void warmPreparedSpeech() {
        int speechReady = 0;
        long speechStarted = System.nanoTime();
        List<String> preparedReplies = scenes.preparedCompletionReplies();
        for (String reply : preparedReplies) {
            try {
                speech.synthesize(reply, Speaker.LEIA_FEMALE);
                speechReady++;
            } catch (RuntimeException ignored) {
                // Voz local do Android permanece como fallback; nunca registra o texto.
            }
        }
        log.info("speech_warmup ready={} total={} duration_ms={}", speechReady,
                preparedReplies.size(),
                (System.nanoTime() - speechStarted) / 1_000_000);
    }

    public boolean requestWarmup() {
        if (providers.isEmpty()
                || !warming.compareAndSet(false, true)) return false;
        long now = System.currentTimeMillis();
        long next = nextOnDemandAt.get();
        if (now < next || !nextOnDemandAt.compareAndSet(next, now + ON_DEMAND_COOLDOWN_MS)) {
            warming.set(false);
            return false;
        }
        try {
            scheduler.schedule(() -> runWarmup(false), Instant.now());
        } catch (RuntimeException error) {
            warming.set(false);
            throw error;
        }
        return true;
    }

    public String activeModelId(String providerId) {
        WarmableConversationProvider provider = providers.get(providerId);
        return provider == null ? providerId : provider.activeModelId();
    }
}
