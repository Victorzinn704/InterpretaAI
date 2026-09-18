package br.gov.interpretaai.server.api;

import br.gov.interpretaai.server.classroom.ClassroomSessionModels.DeviceSeat;
import br.gov.interpretaai.server.classroom.ClassroomSessionModels.ImportRosterRequest;
import br.gov.interpretaai.server.classroom.ClassroomSessionModels.JoinSessionRequest;
import br.gov.interpretaai.server.classroom.ClassroomSessionModels.OpenSessionResponse;
import br.gov.interpretaai.server.classroom.ClassroomSessionModels.Roster;
import br.gov.interpretaai.server.classroom.ClassroomSessionModels.SessionLobby;
import br.gov.interpretaai.server.classroom.ClassroomSessionModels.TeacherSessionStatus;
import br.gov.interpretaai.server.classroom.ClassroomSessionService;
import br.gov.interpretaai.server.device.DeviceAuthenticationToken;
import br.gov.interpretaai.server.device.DevicePairingService.DevicePrincipal;
import br.gov.interpretaai.server.identity.AdultIdentity;
import br.gov.interpretaai.server.identity.InstitutionalAccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v2")
public class ClassroomSessionController {
    private static final String ID = "[a-z0-9][a-z0-9_-]{2,63}";
    private final AdultIdentity identity;
    private final ClassroomSessionService sessions;

    public ClassroomSessionController(AdultIdentity identity, ClassroomSessionService sessions) {
        this.identity = identity;
        this.sessions = sessions;
    }

    @PutMapping("/classroom-management/classrooms/{classroomId}/roster")
    public Roster importRoster(
            Authentication authentication,
            @PathVariable @Pattern(regexp = ID) String classroomId,
            @Valid @RequestBody ImportRosterRequest request) {
        return sessions.importRoster(identity.subject(authentication), classroomId, request);
    }

    @GetMapping("/classroom-management/classrooms/{classroomId}/roster")
    public ResponseEntity<Roster> roster(
            Authentication authentication,
            @PathVariable @Pattern(regexp = ID) String classroomId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(sessions.roster(identity.subject(authentication), classroomId));
    }

    @PostMapping("/classroom-management/classrooms/{classroomId}/sessions")
    public ResponseEntity<OpenSessionResponse> open(
            Authentication authentication,
            @PathVariable @Pattern(regexp = ID) String classroomId) {
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore())
                .body(sessions.open(identity.subject(authentication), classroomId));
    }

    @GetMapping("/classroom-management/sessions/{sessionId}")
    public ResponseEntity<TeacherSessionStatus> status(
            Authentication authentication,
            @PathVariable @Pattern(regexp = ID) String sessionId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(sessions.status(identity.subject(authentication), sessionId));
    }

    @PostMapping("/classroom-management/sessions/{sessionId}/close")
    public ResponseEntity<TeacherSessionStatus> close(
            Authentication authentication,
            @PathVariable @Pattern(regexp = ID) String sessionId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(sessions.close(identity.subject(authentication), sessionId));
    }

    @PostMapping("/devices/{deviceId}/classroom-sessions/resolve")
    public ResponseEntity<SessionLobby> resolve(
            Authentication authentication,
            @PathVariable @Pattern(regexp = ID) String deviceId,
            @RequestBody Map<String, String> request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(sessions.resolve(ownDevice(authentication, deviceId), request.get("code")));
    }

    @PostMapping("/devices/{deviceId}/classroom-sessions/join")
    public ResponseEntity<DeviceSeat> join(
            Authentication authentication,
            @PathVariable @Pattern(regexp = ID) String deviceId,
            @Valid @RequestBody JoinSessionRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(sessions.join(ownDevice(authentication, deviceId), request));
    }

    private static DevicePrincipal ownDevice(Authentication authentication, String deviceId) {
        if (!(authentication instanceof DeviceAuthenticationToken token)
                || !token.getPrincipal().deviceId().equals(deviceId)) {
            throw new InstitutionalAccessService.AccessDeniedException();
        }
        return token.getPrincipal();
    }
}
