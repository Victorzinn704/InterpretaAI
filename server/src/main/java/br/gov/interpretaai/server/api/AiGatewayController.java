package br.gov.interpretaai.server.api;

import br.gov.interpretaai.server.provider.NvidiaWarmupService;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gateway")
@ConditionalOnProperty(name = "interpretaai.conversation.provider", havingValue = "nvidia")
public class AiGatewayController {
    private final NvidiaWarmupService warmup;

    public AiGatewayController(NvidiaWarmupService warmup) {
        this.warmup = warmup;
    }

    @PostMapping("/warmup")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> warmup() {
        boolean accepted = warmup.requestWarmup();
        return status(accepted ? "WARMING" : warmup.isWarm() ? "HOT" : "COOLDOWN");
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return status(warmup.isWarm() ? "HOT" : "COLD");
    }

    private Map<String, Object> status(String state) {
        return Map.of("state", state, "model", warmup.activeModelId());
    }
}
