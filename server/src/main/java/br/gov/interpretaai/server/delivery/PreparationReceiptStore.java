package br.gov.interpretaai.server.delivery;

import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PreparationReceiptStore {
    public record Counts(int pairedCompatibleDevices, int recentlyConfirmedDevices,
                         Instant lastConfirmationAt) {}

    private final JdbcTemplate jdbc;

    public PreparationReceiptStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Caller holds a row lock on story_assignment, so insert/update is race-free. */
    public void confirm(String assignmentId, String deviceId, String sha256, Instant now) {
        int updated = jdbc.update("""
                update story_pack_preparation_receipt
                   set last_confirmed_at = ?
                 where assignment_id = ? and device_id = ? and pack_sha256 = ?
                """, Timestamp.from(now), assignmentId, deviceId, sha256);
        if (updated == 1) return;
        String existing = jdbc.query("""
                select pack_sha256 from story_pack_preparation_receipt
                 where assignment_id = ? and device_id = ?
                """, (result, row) -> result.getString(1), assignmentId, deviceId)
                .stream().findFirst().orElse(null);
        if (existing != null) {
            throw new DeliveryException(409, "preparation_hash_conflict",
                    "A confirmação pertence a outra versão do pacote.");
        }
        jdbc.update("""
                insert into story_pack_preparation_receipt
                (assignment_id, device_id, pack_sha256, first_confirmed_at, last_confirmed_at)
                values (?, ?, ?, ?, ?)
                """, assignmentId, deviceId, sha256, Timestamp.from(now), Timestamp.from(now));
    }

    public Counts counts(
            String assignmentId, String schoolId, String classroomId,
            String sha256, int minAppVersion, Instant freshSince) {
        return jdbc.queryForObject("""
                select count(d.device_id) as paired,
                       count(case when r.pack_sha256 = ? and r.last_confirmed_at >= ?
                                  then 1 end) as confirmed,
                       max(case when r.pack_sha256 = ? and r.last_confirmed_at >= ?
                                then r.last_confirmed_at end) as latest
                  from institution_device d
                  left join story_pack_preparation_receipt r
                    on r.device_id = d.device_id and r.assignment_id = ?
                 where d.school_id = ? and d.classroom_id = ? and d.status = 'ACTIVE'
                   and d.app_version >= ?
                """, (result, row) -> new Counts(
                result.getInt("paired"), result.getInt("confirmed"),
                result.getTimestamp("latest") == null ? null
                        : result.getTimestamp("latest").toInstant()),
                sha256, Timestamp.from(freshSince), sha256, Timestamp.from(freshSince),
                assignmentId, schoolId, classroomId, minAppVersion);
    }
}
