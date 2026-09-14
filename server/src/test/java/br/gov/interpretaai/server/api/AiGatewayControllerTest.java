package br.gov.interpretaai.server.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.gov.interpretaai.server.core.ConversationDeadline;
import br.gov.interpretaai.server.provider.NvidiaWarmupService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AiGatewayControllerTest {
    @Test
    void schedulesWarmupWithoutReturningSecrets() {
        NvidiaWarmupService warmup = mock(NvidiaWarmupService.class);
        when(warmup.activeModelId()).thenReturn("mistralai/mistral-nemotron");
        when(warmup.requestWarmup()).thenReturn(true);
        ConversationDeadline execution = mock(ConversationDeadline.class);
        when(execution.circuitState()).thenReturn("CLOSED");
        AiGatewayController controller = new AiGatewayController(execution, Optional.of(warmup), "nvidia");

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
        ConversationDeadline execution = mock(ConversationDeadline.class);
        when(execution.circuitState()).thenReturn("CLOSED");
        AiGatewayController controller = new AiGatewayController(execution, Optional.of(warmup), "nvidia");

        when(warmup.isWarm()).thenReturn(false, true);

        assertThat(controller.status()).containsEntry("state", "COLD");
        assertThat(controller.status()).containsEntry("state", "HOT");
    }
}
