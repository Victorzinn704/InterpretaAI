package br.gov.interpretaai.server.core;

import java.time.Clock;
import java.time.Duration;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SessionMemory {
    private static final int MAX_MESSAGES = 6;
    private final Cache<String, Entry> sessions;

    public SessionMemory() { this(Clock.systemUTC(), 2_000, Duration.ofMinutes(10)); }

    @Autowired
    public SessionMemory(
            @Value("${interpretaai.memory.max-sessions:2000}") long maxSessions,
            @Value("${interpretaai.memory.ttl-minutes:10}") long ttlMinutes) {
        this(Clock.systemUTC(), maxSessions, Duration.ofMinutes(ttlMinutes));
    }

    SessionMemory(Clock clock) { this(clock, 2_000, Duration.ofMinutes(10)); }

    SessionMemory(Clock clock, long maxSessions, Duration ttl) {
        if (maxSessions < 1 || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("Limites da memória de sessão devem ser positivos");
        }
        this.sessions = Caffeine.newBuilder()
                .maximumSize(maxSessions)
                .expireAfterAccess(ttl)
                .ticker(() -> TimeUnit.MILLISECONDS.toNanos(clock.millis()))
                .build();
    }

    public List<String> appendAndRead(String sessionId, String message) {
        Entry entry = sessions.get(sessionId, ignored -> new Entry());
        synchronized (entry) {
            entry.messages.addLast(message);
            while (entry.messages.size() > MAX_MESSAGES) entry.messages.removeFirst();
            return new ArrayList<>(entry.messages);
        }
    }

    int activeSessionCount() {
        sessions.cleanUp();
        return Math.toIntExact(sessions.estimatedSize());
    }

    private static final class Entry {
        private final ArrayDeque<String> messages = new ArrayDeque<>();
    }
}
