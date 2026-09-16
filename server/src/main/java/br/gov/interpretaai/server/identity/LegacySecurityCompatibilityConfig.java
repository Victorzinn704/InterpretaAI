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
    @Bean
    @Order(3)
    SecurityFilterChain legacyPermitAll(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .build();
    }
}
