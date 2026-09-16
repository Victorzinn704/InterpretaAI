package br.gov.interpretaai.server.device;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
public class DeviceSecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain deviceApiSecurity(
            HttpSecurity http, DevicePairingService pairing, ObjectMapper mapper) throws Exception {
        var authentication = new DeviceAuthenticationFilter(pairing, mapper);
        return http.securityMatcher("/api/v2/devices/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .addFilterBefore(authentication, AnonymousAuthenticationFilter.class)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .build();
    }
}
