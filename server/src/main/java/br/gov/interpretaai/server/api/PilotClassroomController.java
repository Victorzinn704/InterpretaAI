package br.gov.interpretaai.server.api;

import br.gov.interpretaai.server.api.PilotClassroomModels.ClassroomAssignmentResponse;
import br.gov.interpretaai.server.api.PilotClassroomModels.ClassroomResponse;
import br.gov.interpretaai.server.api.PilotClassroomModels.PublishClassroomAssignmentRequest;
import br.gov.interpretaai.server.api.PilotClassroomModels.UpsertClassroomRequest;
import br.gov.interpretaai.server.core.PilotAccess;
import br.gov.interpretaai.server.core.PilotClassroomService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pilot/classrooms")
public class PilotClassroomController {
    private final PilotClassroomService service;
    private final PilotAccess access;

    public PilotClassroomController(PilotClassroomService service, PilotAccess access) {
        this.service = service;
        this.access = access;
    }

    @PutMapping("/{classroomId}")
    public ClassroomResponse save(
            @PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{3,64}") String classroomId,
            @RequestHeader(value = "X-Teacher-Token", required = false) String token,
            @Valid @RequestBody UpsertClassroomRequest request) {
        access.authorizeTeacher(token);
        return service.save(classroomId, request);
    }

    @GetMapping("/{classroomId}")
    public ClassroomResponse find(
            @PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{3,64}") String classroomId,
            @RequestHeader(value = "X-Teacher-Token", required = false) String token) {
        access.authorizeTeacher(token);
        return service.find(classroomId);
    }

    @PostMapping("/{classroomId}/assignments")
    public ClassroomAssignmentResponse publish(
            @PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{3,64}") String classroomId,
            @RequestHeader(value = "X-Teacher-Token", required = false) String token,
            @Valid @RequestBody PublishClassroomAssignmentRequest request) {
        access.authorizeTeacher(token);
        return service.publish(classroomId, request);
    }
}
