package br.gov.interpretaai.server.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class RequiredAudienceValidatorTest {
    private final RequiredAudienceValidator validator =
            new RequiredAudienceValidator("interpretaai-api");

    @Test
    void acceptsOnlyTheConfiguredAudience() {
        assertThat(validator.validate(jwt(List.of("interpretaai-api"))).hasErrors()).isFalse();
        assertThat(validator.validate(jwt(List.of("another-api"))).hasErrors()).isTrue();
        assertThat(validator.validate(jwt(List.of())).hasErrors()).isTrue();
    }

    private Jwt jwt(List<String> audience) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("oidc|teacher")
                .audience(audience)
                .issuedAt(Instant.parse("2026-09-16T12:00:00Z"))
                .expiresAt(Instant.parse("2026-09-16T13:00:00Z"))
                .build();
    }
}
