package br.gov.interpretaai.server.device;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DevicePairingStore {
    public record Pairing(
            String pairingId,
            String schoolId,
            String classroomId,
            String createdByUserId,
            Instant expiresAt) {}

    public record Device(
            String deviceId,
            String schoolId,
            String classroomId,
            String credentialHash,
            int appVersion,
            String status) {}

    private final JdbcTemplate jdbc;

    public DevicePairingStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertPairing(
            String pairingId,
            String codeHash,
            String schoolId,
            String classroomId,
            String userId,
            Instant expiresAt,
            Instant now) {
        jdbc.update("""
                insert into device_pairing_code
                (pairing_id, code_hash, school_id, classroom_id, created_by_user_id,
                 status, expires_at, created_at)
                values (?, ?, ?, ?, ?, 'ACTIVE', ?, ?)
                """, pairingId, codeHash, schoolId, classroomId, userId,
                Timestamp.from(expiresAt), Timestamp.from(now));
    }

    public Optional<Pairing> findActivePairing(String codeHash, Instant now) {
        return jdbc.query("""
                select pairing_id, school_id, classroom_id, created_by_user_id, expires_at
                  from device_pairing_code
                 where code_hash = ? and status = 'ACTIVE' and expires_at >= ?
                """, (result, row) -> new Pairing(
                        result.getString("pairing_id"),
                        result.getString("school_id"),
                        result.getString("classroom_id"),
                        result.getString("created_by_user_id"),
                        result.getTimestamp("expires_at").toInstant()),
                codeHash, Timestamp.from(now)).stream().findFirst();
    }

    public boolean consume(String pairingId, Instant now) {
        return jdbc.update("""
                update device_pairing_code
                   set status = 'CONSUMED', consumed_at = ?
                 where pairing_id = ? and status = 'ACTIVE' and expires_at >= ?
                """, Timestamp.from(now), pairingId, Timestamp.from(now)) == 1;
    }

    public void insertDevice(
            String deviceId,
            Pairing pairing,
            String installationHash,
            String label,
            String credentialHash,
            int appVersion,
            String architecture,
            int widthDp,
            int heightDp,
            Instant now) {
        jdbc.update("""
                insert into institution_device
                (device_id, school_id, classroom_id, installation_hash, label, status,
                 credential_hash, credential_version, app_version, architecture,
                 viewport_width_dp, viewport_height_dp, created_at)
                values (?, ?, ?, ?, ?, 'ACTIVE', ?, 1, ?, ?, ?, ?, ?)
                """, deviceId, pairing.schoolId(), pairing.classroomId(), installationHash,
                label, credentialHash, appVersion, architecture, widthDp, heightDp,
                Timestamp.from(now));
    }

    public Optional<Device> findActiveDevice(String deviceId) {
        return jdbc.query("""
                select device_id, school_id, classroom_id, credential_hash, app_version, status
                  from institution_device where device_id = ? and status = 'ACTIVE'
                """, (result, row) -> new Device(
                        result.getString("device_id"),
                        result.getString("school_id"),
                        result.getString("classroom_id"),
                        result.getString("credential_hash"),
                        result.getInt("app_version"),
                        result.getString("status")), deviceId).stream().findFirst();
    }

    public Optional<Device> findDevice(String deviceId) {
        return jdbc.query("""
                select device_id, school_id, classroom_id, credential_hash, app_version, status
                  from institution_device where device_id = ?
                """, (result, row) -> new Device(
                        result.getString("device_id"),
                        result.getString("school_id"),
                        result.getString("classroom_id"),
                        result.getString("credential_hash"),
                        result.getInt("app_version"),
                        result.getString("status")), deviceId).stream().findFirst();
    }

    public boolean revoke(String deviceId, Instant now) {
        return jdbc.update("""
                update institution_device set status = 'REVOKED', revoked_at = ?
                 where device_id = ? and status = 'ACTIVE'
                """, Timestamp.from(now), deviceId) == 1;
    }
}
