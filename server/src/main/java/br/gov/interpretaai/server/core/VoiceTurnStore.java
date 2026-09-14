package br.gov.interpretaai.server.core;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class VoiceTurnStore {
    public record StoredTurn(String fingerprint, String status, String responseJson, Instant updatedAt) {}

    private final JdbcTemplate jdbc;
    private final Duration ttl;

    public VoiceTurnStore(JdbcTemplate jdbc,
            @Value("${interpretaai.idempotency.ttl-minutes:10}") long ttlMinutes) {
        this.jdbc = jdbc;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    @Transactional
    public boolean claim(String key, String fingerprint, Instant now, Duration processingTimeout) {
        try {
            jdbc.update("""
                    insert into voice_turn_idempotency
                    (idempotency_key, request_fingerprint, status, created_at, updated_at)
                    values (?, ?, 'PROCESSING', ?, ?)
                    """, key, fingerprint, Timestamp.from(now), Timestamp.from(now));
            return true;
        } catch (DuplicateKeyException duplicate) {
            Instant staleBefore = now.minus(processingTimeout);
            return jdbc.update("""
                    update voice_turn_idempotency
                       set status = 'PROCESSING', response_json = null, updated_at = ?
                     where idempotency_key = ? and request_fingerprint = ?
                       and (status = 'FAILED' or (status = 'PROCESSING' and updated_at < ?))
                    """, Timestamp.from(now), key, fingerprint, Timestamp.from(staleBefore)) == 1;
        }
    }

    public Optional<StoredTurn> find(String key) {
        return jdbc.query("""
                select request_fingerprint, status, response_json, updated_at
                  from voice_turn_idempotency where idempotency_key = ?
                """, (result, row) -> new StoredTurn(
                        result.getString("request_fingerprint"),
                        result.getString("status"),
                        result.getString("response_json"),
                        result.getTimestamp("updated_at").toInstant()), key).stream().findFirst();
    }

    @Transactional
    public void complete(String key, String responseJson, Instant now) {
        int changed = jdbc.update("""
                update voice_turn_idempotency
                   set status = 'COMPLETED', response_json = ?, updated_at = ?
                 where idempotency_key = ? and status = 'PROCESSING'
                """, responseJson, Timestamp.from(now), key);
        if (changed != 1) throw new IllegalStateException("Turno idempotente perdeu a posse");
        jdbc.update("""
                insert into operational_outbox
                (event_type, status, attempts, available_at, created_at)
                values ('VOICE_TURN_COMPLETED', 'PENDING', 0, ?, ?)
                """, Timestamp.from(now), Timestamp.from(now));
    }

    public void fail(String key, String fingerprint, Instant now) {
        jdbc.update("""
                update voice_turn_idempotency set status = 'FAILED', updated_at = ?
                 where idempotency_key = ? and request_fingerprint = ? and status = 'PROCESSING'
                """, Timestamp.from(now), key, fingerprint);
    }

    @Scheduled(fixedDelayString = "${interpretaai.idempotency.cleanup-ms:60000}")
    public void cleanup() {
        jdbc.update("delete from voice_turn_idempotency where updated_at < ?",
                Timestamp.from(Instant.now().minus(ttl)));
    }
}
