package br.gov.interpretaai.server.identity;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@ConditionalOnProperty(
        prefix = "interpretaai.identity",
        name = "oidc-enabled",
        havingValue = "true")
public class AdultOidcSecurityConfig {
    @Bean
    SecurityFilterChain adultApiSecurity(HttpSecurity http, ObjectMapper mapper) throws Exception {
        return http.securityMatcher("/api/v2/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .oauth2ResourceServer(resource -> resource
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint((request, response, error) ->
                                writeProblem(response, mapper, 401, "adult_authentication_required")))
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler(
                        (request, response, error) ->
                                writeProblem(response, mapper, 403, "institutional_access_denied")))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    JwtDecoder adultJwtDecoder(
            @Value("${interpretaai.identity.issuer-uri}") String issuer,
            @Value("${interpretaai.identity.audience}") String audience) {
        if (issuer.isBlank() || audience.isBlank()) {
            throw new IllegalStateException("OIDC issuer and audience are required when OIDC is enabled");
        }
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);
        if (decoder instanceof org.springframework.security.oauth2.jwt.NimbusJwtDecoder nimbus) {
            var issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
            var audienceValidator = new RequiredAudienceValidator(audience);
            nimbus.setJwtValidator(
                    new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator));
        }
        return decoder;
    }

    private static void writeProblem(
            HttpServletResponse response,
            ObjectMapper mapper,
            int status,
            String code) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String correlationId = UUID.randomUUID().toString();
        response.setHeader("X-Correlation-Id", correlationId);
        mapper.writeValue(response.getOutputStream(), Map.of(
                "type", "https://interpreta.ai/problems/" + code,
                "title", code,
                "status", status,
                "code", code,
                "safeMessage", status == 401
                        ? "Entre novamente para continuar."
                        : "O recurso não está disponível neste contexto institucional.",
                "correlationId", correlationId));
    }
}
