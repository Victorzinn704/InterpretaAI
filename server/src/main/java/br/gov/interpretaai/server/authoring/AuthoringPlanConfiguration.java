package br.gov.interpretaai.server.authoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.ollama.OllamaChatModel;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Explicit teacher-only provider switch; the child conversation provider is not reused. */
@Configuration
public class AuthoringPlanConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "interpretaai.authoring", name = "plan-worker-enabled",
            havingValue = "true")
    AuthoringPlanService authoringPlanService(
            GuidanceSourceCatalog guidance, ObjectMapper mapper,
            @Value("${interpretaai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${interpretaai.authoring.model:}") String modelName) {
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalStateException("authoring_model_required");
        }
        var model = OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.2)
                .think(false)
                .numPredict(600)
                .timeout(Duration.ofSeconds(30))
                .maxRetries(0)
                .logRequests(false)
                .logResponses(false)
                .build();
        return new AuthoringPlanService(guidance, mapper, AuthoringPlanService.langChain4j(model));
    }
}
