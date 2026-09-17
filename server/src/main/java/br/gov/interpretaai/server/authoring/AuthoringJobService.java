package br.gov.interpretaai.server.authoring;

import br.gov.interpretaai.server.api.AuthoringJobModels.AuthoringJob;
import br.gov.interpretaai.server.api.AuthoringJobModels.CreateAuthoringJobRequest;
import br.gov.interpretaai.server.api.AuthoringJobModels.Failure;
import br.gov.interpretaai.server.api.AuthoringJobModels.Progress;
import br.gov.interpretaai.server.api.AuthoringJobModels.Source;
import br.gov.interpretaai.server.api.AuthoringJobModels.SourceType;
import br.gov.interpretaai.server.authoring.AuthoringPlanContract.DraftPlan;
import br.gov.interpretaai.server.identity.InstitutionAction;
import br.gov.interpretaai.server.identity.InstitutionRole;
import br.gov.interpretaai.server.identity.InstitutionalAccessService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class AuthoringJobService {
    public record JobView(AuthoringJob body, long revision) {}

    private static final Pattern IDEMPOTENCY_KEY =
            Pattern.compile("[A-Za-z0-9._:-]{16,128}");

    private final AuthoringJobStore jobs;
    private final AuthoringJobPersistence persistence;
    private final AuthoringPlanQueueStore plans;
    private final InstitutionalAccessService access;
    private final ObjectMapper mapper;
    private final Clock clock;

    public AuthoringJobService(
            AuthoringJobStore jobs,
            AuthoringJobPersistence persistence,
            AuthoringPlanQueueStore plans,
            InstitutionalAccessService access,
            ObjectMapper mapper,
            Clock clock) {
        this.jobs = jobs;
        this.persistence = persistence;
        this.plans = plans;
        this.access = access;
        this.mapper = mapper;
        this.clock = clock;
    }

    public JobView create(
            String oidcSubject,
            String schoolId,
            String idempotencyKey,
            CreateAuthoringJobRequest request) {
        validateKey(idempotencyKey);
        validateRequest(request);
        var grant = access.requireSchoolAction(
                oidcSubject, schoolId, InstitutionAction.CREATE_DRAFT);
        String requestJson = requestJson(request);
        String fingerprint = sha256(requestJson);
        var existing = jobs.findByIdempotency(grant.userId(), schoolId, idempotencyKey);
        if (existing.isPresent()) return view(matching(existing.orElseThrow(), fingerprint));

        Source source = request.source();
        String sourceRef = sourceRef(source);
        if (source.type() == SourceType.TEACHER_UPLOAD
                && !jobs.sanitizedMediaIsReadyForOwner(sourceRef, schoolId, grant.userId())) {
            throw new AuthoringJobException(
                    422, "authoring_media_not_ready",
                    "A imagem ainda não está pronta para criar a história.");
        }
        if (source.type() == SourceType.APPROVED_LIBRARY) {
            throw new AuthoringJobException(
                    422, "authoring_library_source_unavailable",
                    "A biblioteca aprovada ainda não está disponível neste ambiente.");
        }

        Instant now = clock.instant();
        String jobId = "job_" + UUID.randomUUID().toString().replace("-", "");
        var job = new AuthoringJobStore.Job(
                jobId, schoolId, grant.userId(), idempotencyKey, fingerprint, requestJson,
                source.type().name(), sourceRef, "QUEUED", "QUEUED", 0, 5, 1,
                null, null, now, now);
        try {
            persistence.create(job);
        } catch (DataIntegrityViolationException race) {
            return view(matching(jobs.findByIdempotency(
                    grant.userId(), schoolId, idempotencyKey).orElseThrow(() -> race), fingerprint));
        }
        return view(job);
    }

    public JobView get(String oidcSubject, String schoolId, String jobId) {
        var grant = access.requireSchoolAction(
                oidcSubject, schoolId, InstitutionAction.CREATE_DRAFT);
        var job = jobs.findInSchool(jobId, schoolId)
                .orElseThrow(InstitutionalAccessService.AccessDeniedException::new);
        if (grant.role() == InstitutionRole.TEACHER
                && !job.requestedByUserId().equals(grant.userId())) {
            throw new InstitutionalAccessService.AccessDeniedException();
        }
        return view(job);
    }

    public record PlanView(DraftPlan body, String sha256) {}

    public PlanView getPlan(String oidcSubject, String schoolId, String jobId) {
        get(oidcSubject, schoolId, jobId);
        var stored = plans.findDeliveredInSchool(jobId, schoolId)
                .orElseThrow(() -> new AuthoringJobException(409, "authoring_plan_not_ready",
                        "O plano ainda não está disponível para revisão."));
        try {
            return new PlanView(mapper.readValue(stored.planJson(), DraftPlan.class), stored.sha256());
        } catch (JsonProcessingException invalid) {
            throw new IllegalStateException("stored_authoring_plan_invalid", invalid);
        }
    }

    private void validateKey(String key) {
        if (key == null || !IDEMPOTENCY_KEY.matcher(key).matches()) {
            throw new AuthoringJobException(
                    400, "invalid_idempotency_key",
                    "Use uma chave idempotente de 16 a 128 caracteres.");
        }
    }

    private void validateRequest(CreateAuthoringJobRequest request) {
        if (new HashSet<>(request.objectiveIds()).size() != request.objectiveIds().size()
                || new HashSet<>(request.requestedComponents()).size()
                        != request.requestedComponents().size()) {
            throw new AuthoringJobException(
                    400, "authoring_request_has_duplicates",
                    "Objetivos e componentes não podem se repetir.");
        }
        Source source = request.source();
        boolean valid = switch (source.type()) {
            case TEACHER_UPLOAD -> present(source.mediaId())
                    && source.assetId() == null && source.theme() == null;
            case APPROVED_LIBRARY -> present(source.assetId())
                    && source.mediaId() == null && source.theme() == null;
            case THEME -> present(source.theme())
                    && source.mediaId() == null && source.assetId() == null;
        };
        if (!valid || (source.theme() != null && source.theme().chars().anyMatch(c -> c < 32))) {
            throw new AuthoringJobException(
                    400, "invalid_authoring_source", "Escolha uma fonte válida para a história.");
        }
    }

    private String sourceRef(Source source) {
        return switch (source.type()) {
            case TEACHER_UPLOAD -> source.mediaId();
            case APPROVED_LIBRARY -> source.assetId();
            case THEME -> source.theme().trim();
        };
    }

    private String requestJson(CreateAuthoringJobRequest request) {
        try {
            return mapper.writeValueAsString(request);
        } catch (JsonProcessingException impossible) {
            throw new IllegalStateException("authoring_request_serialization_failed", impossible);
        }
    }

    private AuthoringJobStore.Job matching(AuthoringJobStore.Job job, String fingerprint) {
        if (!job.requestFingerprint().equals(fingerprint)) {
            throw new AuthoringJobException(
                    409, "idempotency_conflict", "A chave já pertence a outra solicitação.");
        }
        return job;
    }

    private JobView view(AuthoringJobStore.Job job) {
        return new JobView(new AuthoringJob(
                job.jobId(), job.status(),
                new Progress(job.progressStep(), job.completedSteps(), job.totalSteps()),
                failure(job),
                job.createdAt(), job.updatedAt()), job.revision());
    }

    private Failure failure(AuthoringJobStore.Job job) {
        if (job.failureSafeMessage() == null) return null;
        String publicCode = switch (job.failureCode() == null ? "" : job.failureCode()) {
            case "guidance_not_approved", "plan_invalid_response", "authoring_payload_invalid" ->
                    job.failureCode();
            default -> "authoring_preparation_failed";
        };
        return new Failure(
                publicCode,
                "FAILED_RETRYABLE".equals(job.status()),
                job.failureSafeMessage());
    }

    private static boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
