package br.gov.interpretaai.server.core;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class SessionMemory {
    private static final int MAX_MESSAGES = 6;
    private static final Duration TTL = Duration.ofMinutes(10);
    private final Map<String, Entry> sessions = new ConcurrentHashMap<>();
    private final Clock clock;

    public SessionMemory() { this(Clock.systemUTC()); }
    SessionMemory(Clock clock) { this.clock = clock; }

    public synchronized List<String> appendAndRead(String sessionId, String message) {
        Instant now = clock.instant();
        sessions.entrySet().removeIf(entry ->
                Duration.between(entry.getValue().updatedAt, now).compareTo(TTL) > 0);
        Entry entry = sessions.compute(sessionId, (key, old) ->
                old == null || Duration.between(old.updatedAt, now).compareTo(TTL) > 0
                        ? new Entry(new ArrayDeque<>(), now) : old);
        entry.messages.addLast(message);
        while (entry.messages.size() > MAX_MESSAGES) entry.messages.removeFirst();
        entry.updatedAt = now;
        return new ArrayList<>(entry.messages);
    }

    synchronized int activeSessionCount() {
        return sessions.size();
    }

    private static final class Entry {
        private final ArrayDeque<String> messages;
        private Instant updatedAt;
        private Entry(ArrayDeque<String> messages, Instant updatedAt) {
            this.messages = messages;
            this.updatedAt = updatedAt;
        }
    }
}
