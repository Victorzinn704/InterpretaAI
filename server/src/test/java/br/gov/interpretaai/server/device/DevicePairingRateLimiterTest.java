package br.gov.interpretaai.server.device;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DevicePairingRateLimiterTest {
    @Test
    void blocksAttemptsAfterTheConfiguredWindowLimit() {
        var limiter = new DevicePairingRateLimiter(2);
        limiter.check("client-a");
        limiter.check("client-a");

        assertThatThrownBy(() -> limiter.check("client-a"))
                .isInstanceOf(DevicePairingException.class)
                .extracting("code")
                .isEqualTo("pairing_rate_limited");

        limiter.check("client-b");
    }
}
