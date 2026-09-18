package br.gov.interpretaai.server.classroom;

import br.gov.interpretaai.server.classroom.ClassroomSessionModels.DeviceSeat;
import br.gov.interpretaai.server.classroom.ClassroomSessionModels.ImportRosterRequest;
import br.gov.interpretaai.server.classroom.ClassroomSessionModels.JoinSessionRequest;
import br.gov.interpretaai.server.classroom.ClassroomSessionModels.LearnerSeat;
import br.gov.interpretaai.server.classroom.ClassroomSessionModels.OpenSessionResponse;
import br.gov.interpretaai.server.classroom.ClassroomSessionModels.Roster;
import br.gov.interpretaai.server.classroom.ClassroomSessionModels.SessionChoice;
import br.gov.interpretaai.server.classroom.ClassroomSessionModels.SessionLobby;
import br.gov.interpretaai.server.classroom.ClassroomSessionModels.TeacherSessionStatus;
import br.gov.interpretaai.server.device.DevicePairingService.DevicePrincipal;
import br.gov.interpretaai.server.identity.InstitutionAction;
import br.gov.interpretaai.server.identity.InstitutionAuditStore;
import br.gov.interpretaai.server.identity.InstitutionalAccessService;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassroomSessionService {
    private static final Pattern SPACES = Pattern.compile("\\s+");
    private static final List<String> AVATARS = List.of("sol", "pipa", "estrela", "foguete");
    private final ClassroomSessionStore store;
    private final ClassroomSessionCode codes;
    private final InstitutionalAccessService access;
    private final InstitutionAuditStore audit;
    private final Clock clock;

    public ClassroomSessionService(
            ClassroomSessionStore store, ClassroomSessionCode codes,
            InstitutionalAccessService access, InstitutionAuditStore audit, Clock clock) {
        this.store = store;
        this.codes = codes;
        this.access = access;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public Roster importRoster(String subject, String classroomId, ImportRosterRequest request) {
        var grant = access.requireClassroomAction(
                subject, classroomId, InstitutionAction.MANAGE_CLASSROOM_SESSION);
        Instant now = clock.instant();
        store.expireSessions(classroomId, now);
        if (store.hasActiveSession(classroomId, now)) {
            throw error(409, "classroom_session_active",
                    "Encerre a aula atual antes de substituir a lista de alunos.");
        }
        List<String> names = request.names().stream().map(ClassroomSessionService::cleanName).toList();
        if (names.stream().anyMatch(String::isBlank)) {
            throw error(400, "learner_name_invalid", "A lista contém um nome vazio.");
        }
        var normalized = new HashSet<String>();
        if (!names.stream().allMatch(name -> normalized.add(normalizeForComparison(name)))) {
            throw error(400, "learner_name_duplicate",
                    "A lista contém nomes repetidos. Diferencie os alunos antes de importar.");
        }
        List<LearnerSeat> learners = java.util.stream.IntStream.range(0, names.size())
                .mapToObj(index -> new LearnerSeat(
                        "learner_" + compactUuid(), names.get(index),
                        AVATARS.get(index % AVATARS.size()) + "-" + String.format("%03d", index + 1),
                        index + 1))
                .toList();
        store.replaceRoster(classroomId, learners, now);
        audit.append(grant.userId(), grant.schoolId(), "CLASSROOM_ROSTER_IMPORTED",
                "CLASSROOM", classroomId, now);
        return new Roster(classroomId, learners);
    }

    @Transactional(readOnly = true)
    public Roster roster(String subject, String classroomId) {
        access.requireClassroomAction(subject, classroomId, InstitutionAction.MANAGE_CLASSROOM_SESSION);
        return new Roster(classroomId, store.roster(classroomId));
    }

    @Transactional
    public OpenSessionResponse open(String subject, String classroomId) {
        var grant = access.requireClassroomAction(
                subject, classroomId, InstitutionAction.MANAGE_CLASSROOM_SESSION);
        Instant now = clock.instant();
        store.expireSessions(classroomId, now);
        List<LearnerSeat> roster = store.roster(classroomId);
        if (roster.isEmpty()) {
            throw error(409, "classroom_roster_empty",
                    "Importe a lista de alunos antes de abrir a aula.");
        }
        if (store.hasActiveSession(classroomId, now)) {
            throw error(409, "classroom_session_active", "Esta turma já possui uma aula aberta.");
        }
        String rawCode = codes.generate();
        String sessionId = "session_" + compactUuid();
        Instant expiresAt = now.plus(Duration.ofHours(8));
        int rosterVersion = store.activeRosterVersion(classroomId);
        store.insertSession(sessionId, classroomId, grant.userId(), codes.hash(rawCode),
                rosterVersion, expiresAt, now);
        audit.append(grant.userId(), grant.schoolId(), "CLASSROOM_SESSION_OPENED",
                "CLASSROOM_SESSION", sessionId, now);
        return new OpenSessionResponse(
                sessionId, classroomId, codes.format(rawCode), expiresAt, roster.size());
    }

    @Transactional(readOnly = true)
    public SessionLobby resolve(DevicePrincipal device, String rawCode) {
        var session = activeByCode(rawCode);
        requireSameSchool(device, session.schoolId());
        var connected = store.seats(session.sessionId());
        return new SessionLobby(session.sessionId(), session.classroomId(), session.expiresAt(),
                connected.stream().map(seat -> new SessionChoice(
                        seat.learnerId(), seat.displayName(), seat.seatNumber(), !seat.connected()))
                        .toList());
    }

    @Transactional
    public DeviceSeat join(DevicePrincipal device, JoinSessionRequest request) {
        var session = activeByCode(request.code());
        requireSameSchool(device, session.schoolId());
        if (!store.learnerAvailable(session.sessionId(), request.learnerId())) {
            throw error(409, "learner_seat_unavailable",
                    "Este lugar já foi usado ou não pertence a esta turma.");
        }
        try {
            store.join(session.sessionId(), device.deviceId(), request.learnerId(), clock.instant());
        } catch (DataIntegrityViolationException conflict) {
            throw error(409, "learner_seat_unavailable", "Este lugar acabou de ser ocupado.");
        }
        var active = store.activeForDevice(device.deviceId(), clock.instant()).orElseThrow();
        int seat = store.seats(session.sessionId()).stream()
                .filter(item -> item.learnerAlias().equals(active.learnerAlias()))
                .mapToInt(item -> item.seatNumber()).findFirst().orElseThrow();
        return new DeviceSeat(active.sessionId(), active.classroomId(), active.learnerAlias(), seat, "ACTIVE");
    }

    @Transactional(readOnly = true)
    public TeacherSessionStatus status(String subject, String sessionId) {
        var session = store.find(sessionId).orElseThrow(() ->
                error(404, "classroom_session_not_found", "A aula não foi encontrada."));
        access.requireClassroomAction(
                subject, session.classroomId(), InstitutionAction.MANAGE_CLASSROOM_SESSION);
        var seats = store.seats(sessionId);
        return new TeacherSessionStatus(
                session.sessionId(), session.classroomId(), session.status(), session.expiresAt(),
                (int) seats.stream().filter(item -> item.connected()).count(), seats.size(), seats);
    }

    @Transactional
    public TeacherSessionStatus close(String subject, String sessionId) {
        var session = store.find(sessionId).orElseThrow(() ->
                error(404, "classroom_session_not_found", "A aula não foi encontrada."));
        var grant = access.requireClassroomAction(
                subject, session.classroomId(), InstitutionAction.MANAGE_CLASSROOM_SESSION);
        Instant now = clock.instant();
        if (store.close(sessionId, now)) {
            audit.append(grant.userId(), grant.schoolId(), "CLASSROOM_SESSION_CLOSED",
                    "CLASSROOM_SESSION", sessionId, now);
        }
        return status(subject, sessionId);
    }

    public ClassroomSessionStore.ActiveDeviceSession activeForDevice(String deviceId) {
        return store.activeForDevice(deviceId, clock.instant()).orElse(null);
    }

    private ClassroomSessionStore.SessionRecord activeByCode(String rawCode) {
        String normalized = codes.normalize(rawCode);
        if (!normalized.matches("[23456789A-HJ-NP-Z]{8}")) {
            throw invalidCode();
        }
        return store.findByCode(codes.hash(normalized), clock.instant())
                .orElseThrow(ClassroomSessionService::invalidCode);
    }

    private static void requireSameSchool(DevicePrincipal device, String schoolId) {
        if (!device.schoolId().equals(schoolId)) {
            throw error(403, "classroom_session_school_mismatch",
                    "A aula não pertence à escola deste aparelho.");
        }
    }

    private static String cleanName(String value) {
        return SPACES.matcher(value == null ? "" : value.strip()).replaceAll(" ");
    }

    private static String normalizeForComparison(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private static String compactUuid() { return UUID.randomUUID().toString().replace("-", ""); }
    private static ClassroomSessionException invalidCode() {
        return error(400, "classroom_session_code_invalid", "O código da aula é inválido ou expirou.");
    }
    private static ClassroomSessionException error(int status, String code, String message) {
        return new ClassroomSessionException(status, code, message);
    }
}
