package br.gov.interpretaai.server.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class SessionMemoryTest {

    @Test
    void removesExpiredSessionsWhenServerReceivesNewActivity() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-13T12:00:00Z"));
        SessionMemory memory = new SessionMemory(clock);

        memory.appendAndRead("expired", "primeira mensagem");
        clock.advance(Duration.ofMinutes(11));
        memory.appendAndRead("current", "nova mensagem");

        assertEquals(1, memory.activeSessionCount());
        assertEquals(2, memory.appendAndRead("current", "continuação").size());
    }

    private static final class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
