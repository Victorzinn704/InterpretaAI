package br.gov.interpretaai.server.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gov.interpretaai.server.core.ConversationDeadline;
import br.gov.interpretaai.server.core.AdaptiveConversationRouter;
import br.gov.interpretaai.server.core.AdaptiveConversationRouter.RouteSnapshot;
import br.gov.interpretaai.server.core.ScenePackCatalog;
import br.gov.interpretaai.server.provider.ProviderWarmupService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AiGatewayControllerTest {
    @Test
    void schedulesWarmupWithoutReturningSecrets() {
        ProviderWarmupService warmup = mock(ProviderWarmupService.class);
        when(warmup.activeModelId("nvidia")).thenReturn("mistralai/mistral-nemotron");
        when(warmup.requestWarmup()).thenReturn(true);
        ConversationDeadline execution = mock(ConversationDeadline.class);
        AdaptiveConversationRouter router = mock(AdaptiveConversationRouter.class);
        ScenePackCatalog scenePack = mock(ScenePackCatalog.class);
        when(scenePack.version()).thenReturn("v2");
        when(scenePack.sceneCount()).thenReturn(7);
        when(router.snapshots()).thenReturn(java.util.Map.of("nvidia",
                new RouteSnapshot(true, true, "CLOSED", 0, 0, 0)));
        when(execution.circuitState()).thenReturn("CLOSED");
        AiGatewayController controller = new AiGatewayController(
                execution, router, Optional.of(warmup), "nvidia", scenePack);

        var response = controller.warmup();

        verify(warmup).requestWarmup();
        assertThat(response).containsEntry("state", "WARMING")
                .containsEntry("model", "mistralai/mistral-nemotron")
                .containsEntry("scenePack", java.util.Map.of("version", "v2", "scenes", 7))
                .doesNotContainKeys("apiKey", "token");
    }

    @Test
    void reportsHotAndColdStates() {
        ProviderWarmupService warmup = mock(ProviderWarmupService.class);
        when(warmup.activeModelId("nvidia")).thenReturn("mistralai/mistral-nemotron");
        ConversationDeadline execution = mock(ConversationDeadline.class);
        AdaptiveConversationRouter router = mock(AdaptiveConversationRouter.class);
        ScenePackCatalog scenePack = mock(ScenePackCatalog.class);
        when(scenePack.version()).thenReturn("v2");
        when(scenePack.sceneCount()).thenReturn(7);
        java.util.concurrent.atomic.AtomicInteger snapshots = new java.util.concurrent.atomic.AtomicInteger();
        when(router.snapshots()).thenAnswer(ignored -> snapshots.getAndIncrement() == 0
                ? java.util.Map.of("nvidia", new RouteSnapshot(false, false, "CLOSED", 0, 0, 0))
                : java.util.Map.of("nvidia", new RouteSnapshot(true, true, "CLOSED", 1, 0, 20)));
        when(execution.circuitState()).thenReturn("CLOSED");
        AiGatewayController controller = new AiGatewayController(
                execution, router, Optional.of(warmup), "nvidia", scenePack);

        assertThat(controller.status()).containsEntry("state", "COLD");
        assertThat(controller.status()).containsEntry("state", "HOT");
    }
}
