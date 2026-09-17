package br.gov.interpretaai.server.studio;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/** Teacher-only browser session; never shares the device's bearer-token security chain. */
@Configuration
public class StudioSecurityConfig {
    @Bean
    @Order(2)
    @ConditionalOnProperty(prefix = "interpretaai.studio", name = "enabled", havingValue = "true")
    SecurityFilterChain enabledStudioSecurity(
            HttpSecurity http,
            @Value("${interpretaai.identity.oidc-enabled:false}") boolean oidcEnabled) throws Exception {
        if (!oidcEnabled) {
            throw new IllegalStateException("Studio requires institutional OIDC");
        }
        return http.securityMatcher(
                        "/studio", "/studio/**", "/login", "/oauth2/authorization/**",
                        "/login/oauth2/code/**")
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/oauth2/authorization/**", "/login/oauth2/code/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(login -> login.defaultSuccessUrl("/studio/", false))
                .logout(logout -> logout.logoutUrl("/studio/logout").logoutSuccessUrl("/login"))
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .build();
    }

    @Bean
    @Order(2)
    @ConditionalOnProperty(prefix = "interpretaai.studio", name = "enabled",
            havingValue = "false", matchIfMissing = true)
    SecurityFilterChain disabledStudioSecurity(HttpSecurity http) throws Exception {
        return http.securityMatcher("/studio", "/studio/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().denyAll())
                .csrf(Customizer.withDefaults())
                .build();
    }
}
