package br.gov.interpretaai.server.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VoiceTurnRateLimiter {
    private final boolean enabled;
    private final int maxRequests;
    private final long windowMillis;
    private final LongSupplier clock;
    private final Cache<String, Window> sessions;

    @Autowired
    public VoiceTurnRateLimiter(
            @Value("${interpretaai.voice-rate-limit.enabled:false}") boolean enabled,
            @Value("${interpretaai.voice-rate-limit.max-requests:8}") int maxRequests,
            @Value("${interpretaai.voice-rate-limit.window-seconds:60}") long windowSeconds) {
        this(enabled, maxRequests, windowSeconds, System::currentTimeMillis);
    }

    VoiceTurnRateLimiter(boolean enabled, int maxRequests, long windowSeconds, LongSupplier clock) {
        if (maxRequests < 1 || windowSeconds < 1) {
            throw new IllegalArgumentException("limite e janela devem ser positivos");
        }
        this.enabled = enabled;
        this.maxRequests = maxRequests;
        this.windowMillis = windowSeconds * 1_000;
        this.clock = clock;
        this.sessions = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterAccess(Duration.ofSeconds(windowSeconds * 2))
                .build();
    }

    public void check(String sessionId) {
        if (!enabled) return;
        long bucket = clock.getAsLong() / windowMillis;
        Window result = sessions.asMap().compute(sessionId, (key, current) ->
                current == null || current.bucket() != bucket
                        ? new Window(bucket, 1)
                        : new Window(bucket, current.count() + 1));
        if (result.count() > maxRequests) throw new RateLimitExceededException();
    }

    private record Window(long bucket, int count) {}

    public static class RateLimitExceededException extends RuntimeException {}
}
