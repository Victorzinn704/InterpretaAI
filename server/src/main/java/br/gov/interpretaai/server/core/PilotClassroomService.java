package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.PilotAssignmentModels.AssignmentResponse;
import br.gov.interpretaai.server.api.PilotAssignmentModels.PublishAssignmentRequest;
import br.gov.interpretaai.server.api.PilotClassroomModels.ClassroomAssignmentResponse;
import br.gov.interpretaai.server.api.PilotClassroomModels.ClassroomResponse;
import br.gov.interpretaai.server.api.PilotClassroomModels.ParticipantRequest;
import br.gov.interpretaai.server.api.PilotClassroomModels.PublishClassroomAssignmentRequest;
import br.gov.interpretaai.server.api.PilotClassroomModels.UpsertClassroomRequest;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PilotClassroomService {
    private final PilotClassroomStore classrooms;
    private final PilotAssignmentStore assignments;

    public PilotClassroomService(PilotClassroomStore classrooms, PilotAssignmentStore assignments) {
        this.classrooms = classrooms;
        this.assignments = assignments;
    }

    @Transactional
    public ClassroomResponse save(String classroomId, UpsertClassroomRequest request) {
        ensureUniqueRoster(request.participants());
        try {
            return classrooms.upsert(classroomId, request, Instant.now());
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "device_already_linked_to_another_classroom", exception);
        }
    }

    @Transactional(readOnly = true)
    public ClassroomResponse find(String classroomId) {
        return classrooms.find(classroomId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "classroom_not_found"));
    }

    @Transactional
    public ClassroomAssignmentResponse publish(
            String classroomId, PublishClassroomAssignmentRequest request) {
        ClassroomResponse classroom = find(classroomId);
        Set<String> requested = new HashSet<>(request.effectiveLearnerAliases());
        if (requested.size() != request.effectiveLearnerAliases().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duplicate_assignment_target");
        }
        List<ParticipantRequest> targets = requested.isEmpty()
                ? classroom.participants()
                : classroom.participants().stream()
                        .filter(participant -> requested.contains(participant.learnerAlias())).toList();
        if (!requested.isEmpty() && targets.size() != requested.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown_assignment_target");
        }
        Instant now = Instant.now();
        List<AssignmentResponse> published = targets.stream().map(participant -> assignments.publish(
                participant.deviceId(),
                new PublishAssignmentRequest(classroom.classroomLabel(), participant.avatarId(),
                        participant.learnerAlias(), request.activity(), request.drawingPrompt()),
                now)).toList();
        return new ClassroomAssignmentResponse(classroomId, published.size(), now, published);
    }

    private void ensureUniqueRoster(List<ParticipantRequest> participants) {
        Set<String> aliases = new HashSet<>();
        Set<String> devices = new HashSet<>();
        boolean unique = participants.stream().allMatch(participant ->
                aliases.add(participant.learnerAlias()) && devices.add(participant.deviceId()));
        if (!unique) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duplicate_roster_member");
        }
    }
}
