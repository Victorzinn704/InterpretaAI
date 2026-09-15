package br.gov.interpretaai.server.api;

import br.gov.interpretaai.server.api.PilotAssignmentModels.AssignmentResponse;
import br.gov.interpretaai.server.api.PilotAssignmentModels.PublishAssignmentRequest;
import br.gov.interpretaai.server.core.PilotAssignmentStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/pilot/assignments")
public class PilotAssignmentController {
    private final PilotAssignmentStore store;
    private final boolean enabled;
    private final String teacherToken;
    private final String deviceToken;

    public PilotAssignmentController(
            PilotAssignmentStore store,
            @Value("${interpretaai.pilot-sync.enabled:false}") boolean enabled,
            @Value("${interpretaai.pilot-sync.teacher-token:}") String teacherToken,
            @Value("${interpretaai.pilot-sync.device-token:}") String deviceToken) {
        this.store = store;
        this.enabled = enabled;
        this.teacherToken = teacherToken;
        this.deviceToken = deviceToken;
    }

    @PutMapping("/{deviceId}")
    public AssignmentResponse publish(
            @PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{6,64}") String deviceId,
            @RequestHeader(value = "X-Teacher-Token", required = false) String providedToken,
            @Valid @RequestBody PublishAssignmentRequest request) {
        authorize(providedToken, teacherToken);
        return store.publish(deviceId, request, Instant.now());
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<AssignmentResponse> latest(
            @PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{6,64}") String deviceId,
            @RequestHeader(value = "X-Device-Token", required = false) String providedToken,
            @RequestParam(defaultValue = "0") @Min(0) long afterVersion) {
        authorize(providedToken, deviceToken);
        return store.find(deviceId).filter(item -> item.version() > afterVersion).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    private void authorize(String provided, String configured) {
        if (!enabled || configured.length() < 16) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "pilot_sync_disabled");
        }
        byte[] expected = configured.getBytes(StandardCharsets.UTF_8);
        byte[] actual = provided == null ? new byte[0] : provided.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_pilot_token");
        }
    }
}
