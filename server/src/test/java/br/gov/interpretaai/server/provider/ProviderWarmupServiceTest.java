package br.gov.interpretaai.server.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gov.interpretaai.server.api.VoiceTurnModels.PedagogicalReply;
import br.gov.interpretaai.server.api.VoiceTurnModels.Request;
import br.gov.interpretaai.server.core.AdaptiveConversationRouter;
import br.gov.interpretaai.server.core.ScenePackCatalog;
import br.gov.interpretaai.server.core.SpeechSynthesisService;
import br.gov.interpretaai.server.core.WarmableConversationProvider;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

class ProviderWarmupServiceTest {
    @Test void warmsOnlyColdRemoteProvidersPresentInTheConfiguredRoute() {
        FakeWarmable gemini = new FakeWarmable("gemini", false);
        FakeWarmable nvidia = new FakeWarmable("nvidia", false);
        AdaptiveConversationRouter router = mock(AdaptiveConversationRouter.class);
        when(router.configuredRoute()).thenReturn(List.of("gemini"));
        ProviderWarmupService service = new ProviderWarmupService(
                List.of(gemini, nvidia), router, mock(TaskScheduler.class),
                mock(SpeechSynthesisService.class), mock(ScenePackCatalog.class));

        service.keepWarm();
        service.keepWarm();

        assertThat(gemini.warmups).hasValue(1);
        assertThat(nvidia.warmups).hasValue(0);
        assertThat(service.activeModelId("gemini")).isEqualTo("model-gemini");
    }

    @Test void coalescesRepeatedOnDemandWarmupRequests() {
        FakeWarmable gemini = new FakeWarmable("gemini", false);
        AdaptiveConversationRouter router = mock(AdaptiveConversationRouter.class);
        when(router.configuredRoute()).thenReturn(List.of("gemini"));
        TaskScheduler scheduler = mock(TaskScheduler.class);
        when(scheduler.schedule(any(Runnable.class), any(Instant.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        });
        ProviderWarmupService service = service(List.of(gemini), router, scheduler);

        assertThat(service.requestWarmup()).isTrue();
        assertThat(service.requestWarmup()).isFalse();
        service.keepWarm();
        assertThat(gemini.warmups).hasValue(1);
        verify(scheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test void scheduledProbeStartsCooldownForAnImmediateOnDemandRequest() {
        FakeWarmable gemini = new FakeWarmable("gemini", false);
        gemini.succeeds = false;
        AdaptiveConversationRouter router = mock(AdaptiveConversationRouter.class);
        when(router.configuredRoute()).thenReturn(List.of("gemini"));
        TaskScheduler scheduler = mock(TaskScheduler.class);
        ProviderWarmupService service = service(List.of(gemini), router, scheduler);

        service.keepWarm();

        assertThat(gemini.warmups).hasValue(1);
        assertThat(service.requestWarmup()).isFalse();
    }

    @Test void preloadsPreparedSpeechWithoutLoggingItsText() {
        FakeWarmable ollama = new FakeWarmable("ollama", true);
        AdaptiveConversationRouter router = mock(AdaptiveConversationRouter.class);
        when(router.configuredRoute()).thenReturn(List.of("ollama"));
        SpeechSynthesisService speech = mock(SpeechSynthesisService.class);
        ScenePackCatalog scenes = mock(ScenePackCatalog.class);
        when(scenes.preparedCompletionReplies()).thenReturn(List.of("Resposta aprovada"));
        ProviderWarmupService service = new ProviderWarmupService(
                List.of(ollama), router, mock(TaskScheduler.class), speech, scenes);

        service.keepWarm();

        verify(speech).synthesize("Resposta aprovada",
                br.gov.interpretaai.server.api.VoiceTurnModels.Speaker.LEIA_FEMALE);
    }

    private ProviderWarmupService service(
            List<WarmableConversationProvider> providers,
            AdaptiveConversationRouter router,
            TaskScheduler scheduler) {
        ScenePackCatalog scenes = mock(ScenePackCatalog.class);
        when(scenes.preparedCompletionReplies()).thenReturn(List.of());
        return new ProviderWarmupService(providers, router, scheduler,
                mock(SpeechSynthesisService.class), scenes);
    }

    private static final class FakeWarmable implements WarmableConversationProvider {
        private final String id;
        private volatile boolean warm;
        private volatile boolean succeeds = true;
        private final AtomicInteger warmups = new AtomicInteger();

        private FakeWarmable(String id, boolean warm) {
            this.id = id;
            this.warm = warm;
        }

        @Override public String providerId() { return id; }
        @Override public boolean available() { return warm; }
        @Override public boolean isWarm() { return warm; }
        @Override public String activeModelId() { return "model-" + id; }
        @Override public boolean warmUp() {
            warmups.incrementAndGet();
            warm = succeeds;
            return succeeds;
        }
        @Override public PedagogicalReply reply(Request request, List<String> recentMessages) {
            throw new UnsupportedOperationException();
        }
    }
}
