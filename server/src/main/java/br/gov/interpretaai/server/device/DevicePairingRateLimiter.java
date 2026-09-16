package br.gov.interpretaai.server.device;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DevicePairingRateLimiter {
    private final Cache<String, AtomicInteger> attempts;
    private final int maxAttempts;

    public DevicePairingRateLimiter(
            @Value("${interpretaai.device-pairing.max-attempts-per-minute:10}") int maxAttempts) {
        this.maxAttempts = maxAttempts;
        this.attempts = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(1))
                .build();
    }

    public void check(String clientKey) {
        int current = attempts.asMap()
                .computeIfAbsent(clientKey, ignored -> new AtomicInteger())
                .incrementAndGet();
        if (current > maxAttempts) {
            throw new DevicePairingException(
                    429, "pairing_rate_limited", "Aguarde um minuto antes de tentar novamente.");
        }
    }
}
