package br.gov.interpretaai.server.api;

import br.gov.interpretaai.server.core.ConversationDeadline;
import br.gov.interpretaai.server.provider.NvidiaWarmupService;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gateway")
public class AiGatewayController {
    private final ConversationDeadline execution;
    private final Optional<NvidiaWarmupService> warmup;
    private final String provider;

    public AiGatewayController(
            ConversationDeadline execution,
            Optional<NvidiaWarmupService> warmup,
            @Value("${interpretaai.conversation.provider:ollama}") String provider) {
        this.execution = execution;
        this.warmup = warmup;
        this.provider = provider;
    }

    @PostMapping("/warmup")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> warmup() {
        boolean accepted = warmup.map(NvidiaWarmupService::requestWarmup).orElse(false);
        return status(accepted ? "WARMING" : available() ? "HOT" : "COOLDOWN");
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return status(available() ? "HOT" : "COLD");
    }

    private Map<String, Object> status(String state) {
        return Map.of(
                "state", state,
                "circuit", execution.circuitState(),
                "provider", provider,
                "model", warmup.map(NvidiaWarmupService::activeModelId).orElse(provider));
    }

    private boolean available() {
        boolean circuitAllows = !"OPEN".equals(execution.circuitState());
        return circuitAllows && warmup.map(NvidiaWarmupService::isWarm).orElse(true);
    }
}
