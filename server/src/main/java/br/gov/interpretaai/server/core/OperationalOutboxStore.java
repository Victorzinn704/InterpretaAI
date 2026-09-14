package br.gov.interpretaai.server.core;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class OperationalOutboxStore {
    public record Event(long id, String type, int attempts) {}

    private final JdbcTemplate jdbc;

    public OperationalOutboxStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public Optional<Event> claimNext(Instant now, Duration lease) {
        var candidates = jdbc.query("""
                select id, event_type, attempts from operational_outbox
                 where ((status = 'PENDING' and available_at <= ?)
                    or (status = 'PROCESSING' and available_at <= ?))
                 order by id limit 1
                """, (result, row) -> new Event(
                        result.getLong("id"), result.getString("event_type"),
                        result.getInt("attempts")), Timestamp.from(now), Timestamp.from(now));
        if (candidates.isEmpty()) return Optional.empty();
        Event event = candidates.get(0);
        int changed = jdbc.update("""
                update operational_outbox set status = 'PROCESSING', available_at = ?
                 where id = ? and available_at <= ? and status in ('PENDING', 'PROCESSING')
                """, Timestamp.from(now.plus(lease)), event.id(), Timestamp.from(now));
        return changed == 1 ? Optional.of(event) : Optional.empty();
    }

    public void delivered(long id) {
        jdbc.update("update operational_outbox set status = 'DELIVERED' where id = ?", id);
    }

    public void retry(Event event, Instant now) {
        int attempts = event.attempts() + 1;
        if (attempts >= 5) {
            jdbc.update("""
                    update operational_outbox set status = 'DEAD', attempts = ? where id = ?
                    """, attempts, event.id());
            return;
        }
        long delaySeconds = Math.min(60, 1L << Math.min(attempts, 6));
        jdbc.update("""
                update operational_outbox
                   set status = 'PENDING', attempts = ?, available_at = ? where id = ?
                """, attempts, Timestamp.from(now.plusSeconds(delaySeconds)), event.id());
    }

    @Scheduled(fixedDelayString = "${interpretaai.outbox.cleanup-ms:3600000}")
    public void cleanup() {
        Instant now = Instant.now();
        jdbc.update("""
                delete from operational_outbox
                 where (status = 'DELIVERED' and created_at < ?)
                    or (status = 'DEAD' and created_at < ?)
                """, Timestamp.from(now.minus(Duration.ofDays(1))),
                Timestamp.from(now.minus(Duration.ofDays(7))));
    }
}
