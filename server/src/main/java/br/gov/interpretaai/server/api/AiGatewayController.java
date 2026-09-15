package br.gov.interpretaai.server.api;

import br.gov.interpretaai.server.core.ConversationDeadline;
import br.gov.interpretaai.server.core.AdaptiveConversationRouter;
import br.gov.interpretaai.server.core.ScenePackCatalog;
import br.gov.interpretaai.server.provider.ProviderWarmupService;
import java.util.List;
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
    private final AdaptiveConversationRouter router;
    private final Optional<ProviderWarmupService> warmup;
    private final String provider;
    private final ScenePackCatalog scenePack;

    public AiGatewayController(
            ConversationDeadline execution,
            AdaptiveConversationRouter router,
            Optional<ProviderWarmupService> warmup,
            @Value("${interpretaai.conversation.provider:ollama}") String provider,
            ScenePackCatalog scenePack) {
        this.execution = execution;
        this.router = router;
        this.warmup = warmup;
        this.provider = provider;
        this.scenePack = scenePack;
    }

    @PostMapping("/warmup")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, Object> warmup() {
        boolean accepted = warmup.map(ProviderWarmupService::requestWarmup).orElse(false);
        return status(accepted ? "WARMING" : available() ? "HOT" : "COOLDOWN");
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return status(available() ? "HOT" : "COLD");
    }

    private Map<String, Object> status(String state) {
        return Map.of(
                "state", state,
                "circuits", execution.circuitStates(),
                "provider", provider,
                "model", List.of("nvidia", "gemini").contains(provider.toLowerCase())
                        ? warmup.map(service -> service.activeModelId(provider)).orElse(provider)
                        : provider,
                "scenePack", Map.of("version", scenePack.version(), "scenes", scenePack.sceneCount()),
                "routes", router.snapshots());
    }

    private boolean available() {
        return router.snapshots().values().stream()
                .anyMatch(AdaptiveConversationRouter.RouteSnapshot::eligible);
    }
}
