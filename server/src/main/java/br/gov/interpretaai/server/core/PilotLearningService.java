package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.PilotLearningModels.ClassroomSummary;
import br.gov.interpretaai.server.api.PilotLearningModels.LearningEventBatchRequest;
import br.gov.interpretaai.server.api.PilotLearningModels.LearningEventBatchResponse;
import br.gov.interpretaai.server.api.PilotLearningModels.SecretariatSummary;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PilotLearningService {
    private static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(5);
    private static final Duration MAX_EVENT_AGE = Duration.ofDays(30);

    private final PilotClassroomStore classrooms;
    private final PilotLearningStore learning;
    private final Clock clock;

    @Autowired
    public PilotLearningService(PilotClassroomStore classrooms, PilotLearningStore learning) {
        this(classrooms, learning, Clock.systemUTC());
    }

    PilotLearningService(PilotClassroomStore classrooms, PilotLearningStore learning, Clock clock) {
        this.classrooms = classrooms;
        this.learning = learning;
        this.clock = clock;
    }

    @Transactional
    public LearningEventBatchResponse ingest(LearningEventBatchRequest request) {
        var participant = classrooms.findParticipantByDevice(request.deviceId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "device_not_registered_in_classroom"));
        ensureUniqueEventIds(request);
        Instant now = clock.instant();
        int accepted = 0;
        for (var event : request.events()) {
            if (event.occurredAt().isAfter(now.plus(MAX_CLOCK_SKEW))
                    || event.occurredAt().isBefore(now.minus(MAX_EVENT_AGE))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "event_time_out_of_range");
            }
            accepted += learning.insert(request.deviceId(), participant, event, now);
        }
        return new LearningEventBatchResponse(accepted, request.events().size() - accepted);
    }

    @Transactional
    public ClassroomSummary classroomSummary(String classroomId) {
        classrooms.find(classroomId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "classroom_not_found"));
        learning.audit("TEACHER", classroomId, clock.instant());
        return learning.summary(classroomId);
    }

    @Transactional
    public SecretariatSummary secretariatSummary() {
        List<ClassroomSummary> summaries = learning.summaries();
        learning.audit("SECRETARY", "PILOT_NETWORK", clock.instant());
        return new SecretariatSummary(
                summaries.size(),
                summaries.stream().mapToInt(ClassroomSummary::participants).sum(),
                summaries.stream().mapToLong(ClassroomSummary::sessions).sum(),
                summaries.stream().mapToLong(ClassroomSummary::participations).sum(),
                summaries.stream().mapToLong(ClassroomSummary::completedStages).sum(),
                summaries.stream().mapToLong(ClassroomSummary::helpRequests).sum(),
                summaries,
                clock.instant());
    }

    private void ensureUniqueEventIds(LearningEventBatchRequest request) {
        Set<String> ids = new HashSet<>();
        if (!request.events().stream().allMatch(event -> ids.add(event.eventId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "duplicate_event_in_batch");
        }
    }
}
