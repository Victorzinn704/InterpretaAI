package br.gov.interpretaai.server.api;

import br.gov.interpretaai.server.api.PilotAssignmentModels.AssignmentResponse;
import br.gov.interpretaai.server.api.PilotAssignmentModels.PublishAssignmentRequest;
import br.gov.interpretaai.server.core.PilotAccess;
import br.gov.interpretaai.server.core.PilotAssignmentStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pilot/assignments")
public class PilotAssignmentController {
    private final PilotAssignmentStore store;
    private final PilotAccess access;

    public PilotAssignmentController(PilotAssignmentStore store, PilotAccess access) {
        this.store = store;
        this.access = access;
    }

    @PutMapping("/{deviceId}")
    public AssignmentResponse publish(
            @PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{6,64}") String deviceId,
            @RequestHeader(value = "X-Teacher-Token", required = false) String providedToken,
            @Valid @RequestBody PublishAssignmentRequest request) {
        access.authorizeTeacher(providedToken);
        return store.publish(deviceId, request, Instant.now());
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<AssignmentResponse> latest(
            @PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{6,64}") String deviceId,
            @RequestHeader(value = "X-Device-Token", required = false) String providedToken,
            @RequestParam(defaultValue = "0") @Min(0) long afterVersion) {
        access.authorizeDevice(providedToken);
        return store.find(deviceId).filter(item -> item.version() > afterVersion).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

}
