package br.gov.interpretaai.server.api;

import br.gov.interpretaai.server.api.PilotLearningModels.ClassroomSummary;
import br.gov.interpretaai.server.api.PilotLearningModels.LearningEventBatchRequest;
import br.gov.interpretaai.server.api.PilotLearningModels.LearningEventBatchResponse;
import br.gov.interpretaai.server.api.PilotLearningModels.SecretariatSummary;
import br.gov.interpretaai.server.core.PilotAccess;
import br.gov.interpretaai.server.core.PilotLearningService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pilot")
public class PilotLearningController {
    private final PilotLearningService service;
    private final PilotAccess access;

    public PilotLearningController(PilotLearningService service, PilotAccess access) {
        this.service = service;
        this.access = access;
    }

    @PostMapping("/learning-events:batch")
    public LearningEventBatchResponse ingest(
            @RequestHeader(value = "X-Device-Token", required = false) String token,
            @Valid @RequestBody LearningEventBatchRequest request) {
        access.authorizeDevice(token);
        return service.ingest(request);
    }

    @GetMapping("/classrooms/{classroomId}/summary")
    public ClassroomSummary classroomSummary(
            @PathVariable @Pattern(regexp = "[a-zA-Z0-9_-]{3,64}") String classroomId,
            @RequestHeader(value = "X-Teacher-Token", required = false) String token) {
        access.authorizeTeacher(token);
        return service.classroomSummary(classroomId);
    }

    @GetMapping("/secretariat/summary")
    public SecretariatSummary secretariatSummary(
            @RequestHeader(value = "X-Secretary-Token", required = false) String token) {
        access.authorizeSecretary(token);
        return service.secretariatSummary();
    }
}
