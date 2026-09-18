package br.gov.interpretaai.server.identity;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.annotation.Order;

@Configuration
@ConditionalOnProperty(
        prefix = "interpretaai.identity",
        name = "oidc-enabled",
        havingValue = "false",
        matchIfMissing = true)
public class LegacySecurityCompatibilityConfig {
    /** Adult v2 never inherits the legacy v1 permit-all mode. Device routes match order 1 first. */
    @Bean
    @Order(2)
    SecurityFilterChain disabledAdultApiSecurity(HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/v2/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().denyAll())
                .csrf(csrf -> csrf.disable())
                .build();
    }

    @Bean
    @Order(4)
    SecurityFilterChain legacyPermitAll(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .build();
    }
}
