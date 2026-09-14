package br.gov.interpretaai.server.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gov.interpretaai.server.provider.NvidiaWarmupService;
import org.junit.jupiter.api.Test;

class AiGatewayControllerTest {
    @Test
    void schedulesWarmupWithoutReturningSecrets() {
        NvidiaWarmupService warmup = mock(NvidiaWarmupService.class);
        when(warmup.activeModelId()).thenReturn("mistralai/mistral-nemotron");
        when(warmup.requestWarmup()).thenReturn(true);
        AiGatewayController controller = new AiGatewayController(warmup);

        var response = controller.warmup();

        verify(warmup).requestWarmup();
        assertThat(response).containsEntry("state", "WARMING")
                .containsEntry("model", "mistralai/mistral-nemotron")
                .doesNotContainKeys("apiKey", "token");
    }

    @Test
    void reportsHotAndColdStates() {
        NvidiaWarmupService warmup = mock(NvidiaWarmupService.class);
        when(warmup.activeModelId()).thenReturn("mistralai/mistral-nemotron");
        AiGatewayController controller = new AiGatewayController(warmup);

        when(warmup.isWarm()).thenReturn(false, true);

        assertThat(controller.status()).containsEntry("state", "COLD");
        assertThat(controller.status()).containsEntry("state", "HOT");
    }
}
