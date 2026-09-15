package br.gov.interpretaai.server.core;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class VoiceTurnRateLimiterTest {
    @Test void limitsOneSessionAndResetsOnTheNextWindow() {
        AtomicLong clock = new AtomicLong(10_000);
        VoiceTurnRateLimiter limiter = new VoiceTurnRateLimiter(true, 3, 60, clock::get);

        for (int call = 0; call < 3; call++) limiter.check("session-a");
        assertThatThrownBy(() -> limiter.check("session-a"))
                .isInstanceOf(VoiceTurnRateLimiter.RateLimitExceededException.class);
        assertThatCode(() -> limiter.check("session-b")).doesNotThrowAnyException();

        clock.addAndGet(60_000);
        assertThatCode(() -> limiter.check("session-a")).doesNotThrowAnyException();
    }

    @Test void disabledLimiterNeverConsumesTheSessionBudget() {
        VoiceTurnRateLimiter limiter = new VoiceTurnRateLimiter(false, 1, 60, () -> 0);
        for (int call = 0; call < 20; call++) limiter.check("session-a");
    }
}
