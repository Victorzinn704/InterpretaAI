package br.gov.interpretaai.server.authoring;

import br.gov.interpretaai.server.api.AuthoringJobModels.CreateAuthoringJobRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * First isolated authoring stage. It validates the persisted request and hands the job to the
 * future guidance stage; no model or provider is invoked here.
 */
@Component
@ConditionalOnProperty(
        prefix = "interpretaai.authoring",
        name = "worker-enabled",
        havingValue = "true")
public class AuthoringJobWorker {
    private static final Logger log = LoggerFactory.getLogger(AuthoringJobWorker.class);
    private static final int BATCH_SIZE = 4;
    private static final Duration LEASE = Duration.ofSeconds(45);

    private final AuthoringJobQueueStore queue;
    private final ObjectMapper mapper;
    private final Clock clock;

    public AuthoringJobWorker(
            AuthoringJobQueueStore queue, ObjectMapper mapper, Clock clock) {
        this.queue = queue;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${interpretaai.authoring.worker-poll-ms:500}")
    public void drain() {
        for (int index = 0; index < BATCH_SIZE; index++) {
            if (!processOne()) return;
        }
    }

    public boolean processOne() {
        Instant now = clock.instant();
        var claimed = queue.claimNext(now, LEASE);
        if (claimed.isEmpty()) return false;
        var job = claimed.orElseThrow();
        try {
            mapper.readValue(job.requestJson(), CreateAuthoringJobRequest.class);
            queue.markDelivered(job, clock.instant());
        } catch (JsonProcessingException invalidPayload) {
            queue.retryOrFail(job, "authoring_payload_invalid", clock.instant());
            log.warn("authoring_payload_invalid job={} attempt={}", job.jobId(), job.attempts());
        } catch (RuntimeException failure) {
            if ("authoring_job_lease_lost".equals(failure.getMessage())) {
                log.warn("authoring_job_lease_lost job={} attempt={}", job.jobId(), job.attempts());
                return true;
            }
            queue.retryOrFail(job, "authoring_preparation_failed", clock.instant());
            log.warn("authoring_preparation_failed job={} attempt={}", job.jobId(), job.attempts());
        }
        return true;
    }
}
