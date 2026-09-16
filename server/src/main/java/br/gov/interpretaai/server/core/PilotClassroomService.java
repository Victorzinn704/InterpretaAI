package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.PilotAssignmentModels.AssignmentResponse;
import br.gov.interpretaai.server.api.PilotAssignmentModels.AssignmentMember;
import br.gov.interpretaai.server.api.PilotClassroomModels.ClassroomAssignmentResponse;
import br.gov.interpretaai.server.api.PilotClassroomModels.ClassroomResponse;
import br.gov.interpretaai.server.api.PilotClassroomModels.ParticipantRequest;
import br.gov.interpretaai.server.api.PilotClassroomModels.PublishClassroomAssignmentRequest;
import br.gov.interpretaai.server.api.PilotClassroomModels.UpsertClassroomRequest;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;
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
        request.participants().stream().map(ParticipantRequest::deviceId).distinct().forEach(deviceId ->
                classrooms.findParticipantByDevice(deviceId)
                        .filter(location -> !location.classroomId().equals(classroomId))
                        .ifPresent(location -> {
                            throw new ResponseStatusException(HttpStatus.CONFLICT,
                                    "device_already_linked_to_another_classroom");
                        }));
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
        List<ParticipantRequest> selected = requested.isEmpty()
                ? classroom.participants()
                : classroom.participants().stream()
                        .filter(participant -> requested.contains(participant.learnerAlias())).toList();
        if (!requested.isEmpty() && selected.size() != requested.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown_assignment_target");
        }
        Set<String> targetDevices = selected.stream().map(ParticipantRequest::deviceId)
                .collect(java.util.stream.Collectors.toSet());
        Map<String, List<ParticipantRequest>> targets = classroom.participants().stream()
                .filter(participant -> targetDevices.contains(participant.deviceId()))
                .collect(java.util.stream.Collectors.groupingBy(
                        ParticipantRequest::deviceId, LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));
        Instant now = Instant.now();
        List<AssignmentResponse> published = targets.entrySet().stream().map(entry -> assignments.publish(
                entry.getKey(), classroom.classroomLabel(), request.activity(), request.drawingPrompt(),
                entry.getValue().stream().map(participant ->
                        new AssignmentMember(participant.learnerAlias(), participant.avatarId())).toList(),
                now)).toList();
        return new ClassroomAssignmentResponse(classroomId, published.size(), now, published);
    }

    private void ensureUniqueRoster(List<ParticipantRequest> participants) {
        Set<String> aliases = new HashSet<>();
        boolean unique = participants.stream().allMatch(participant -> aliases.add(participant.learnerAlias()));
        if (!unique) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duplicate_roster_member");
        }
        boolean oversizedTabletGroup = participants.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ParticipantRequest::deviceId, java.util.stream.Collectors.counting()))
                .values().stream().anyMatch(count -> count > 4);
        if (oversizedTabletGroup) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tablet_group_too_large");
        }
    }
}
