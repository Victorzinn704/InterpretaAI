package br.gov.interpretaai.server.authoring;

import br.gov.interpretaai.server.api.AuthoringJobModels.CreateAuthoringJobRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Produces a persisted teacher draft only; it cannot generate media or publish to tablets. */
@Component
@ConditionalOnProperty(prefix = "interpretaai.authoring", name = "plan-worker-enabled",
        havingValue = "true")
public class AuthoringPlanWorker {
    private static final Logger log = LoggerFactory.getLogger(AuthoringPlanWorker.class);
    private static final Duration LEASE = Duration.ofSeconds(90);

    private final AuthoringPlanQueueStore queue;
    private final AuthoringPlanService planner;
    private final ObjectMapper mapper;
    private final Clock clock;

    public AuthoringPlanWorker(AuthoringPlanQueueStore queue, AuthoringPlanService planner,
            ObjectMapper mapper, Clock clock) {
        this.queue = queue;
        this.planner = planner;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${interpretaai.authoring.plan-worker-poll-ms:500}")
    public void drain() {
        for (int index = 0; index < 2; index++) {
            if (!processOne()) return;
        }
    }

    public boolean processOne() {
        var claim = queue.claimNext(clock.instant(), LEASE);
        if (claim.isEmpty()) return false;
        var job = claim.orElseThrow();
        try {
            var input = mapper.readValue(job.requestJson(), CreateAuthoringJobRequest.class);
            var plan = planner.plan(input, job.schoolId(), job.userId());
            queue.complete(job, mapper.writeValueAsString(plan), clock.instant());
        } catch (AuthoringPlanService.NoApprovedGuidance missing) {
            queue.pauseForGuidance(job, clock.instant());
            log.info("authoring_guidance_not_approved job={}", job.jobId());
        } catch (AuthoringPlanContract.Rejected invalid) {
            queue.retryOrFail(job, "plan_invalid_response", clock.instant());
            log.warn("authoring_plan_invalid_response job={} attempt={} code={}",
                    job.jobId(), job.attempts(), invalid.code());
        } catch (JsonProcessingException invalidPayload) {
            queue.retryOrFail(job, "authoring_payload_invalid", clock.instant());
            log.warn("authoring_plan_payload_invalid job={} attempt={}",
                    job.jobId(), job.attempts());
        } catch (RuntimeException failure) {
            if ("authoring_plan_lease_lost".equals(failure.getMessage())) {
                log.warn("authoring_plan_lease_lost job={} attempt={}", job.jobId(), job.attempts());
                return true;
            }
            queue.retryOrFail(job, "authoring_plan_unavailable", clock.instant());
            log.warn("authoring_plan_unavailable job={} attempt={}", job.jobId(), job.attempts());
        }
        return true;
    }
}
