package br.gov.interpretaai.server.story;

import br.gov.interpretaai.server.api.StoryVersionModels.ApprovalRequest;
import br.gov.interpretaai.server.api.StoryVersionModels.StoryVersionState;
import br.gov.interpretaai.server.authoring.AuthoringJobStore;
import br.gov.interpretaai.server.identity.InstitutionAction;
import br.gov.interpretaai.server.identity.InstitutionAuditStore;
import br.gov.interpretaai.server.identity.InstitutionRole;
import br.gov.interpretaai.server.identity.InstitutionalAccessService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Human-only gate between a reviewable draft and any future device delivery. */
@Service
public class StoryVersionService {
    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("[A-Za-z0-9._:-]{16,128}");
    private static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9_-]{2,63}");

    private final StoryVersionStore versions;
    private final AuthoringJobStore authoringJobs;
    private final LearningStoryPackValidator packs;
    private final InstitutionalAccessService access;
    private final InstitutionAuditStore audit;
    private final Clock clock;

    public StoryVersionService(
            StoryVersionStore versions,
            AuthoringJobStore authoringJobs,
            LearningStoryPackValidator packs,
            InstitutionalAccessService access,
            InstitutionAuditStore audit,
            Clock clock) {
        this.versions = versions;
        this.authoringJobs = authoringJobs;
        this.packs = packs;
        this.access = access;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * Internal ingress for a completed authoring worker. There is deliberately no matching HTTP
     * endpoint: an adult browser cannot submit arbitrary JSON as child-facing content.
     */
    @Transactional
    public StoryVersionState materializeValidatedDraft(String authoringJobId, String rawPackJson) {
        var job = authoringJobs.findById(authoringJobId)
                .orElseThrow(() -> new StoryVersionException(
                        404, "authoring_job_not_found", "O trabalho de autoria não existe."));
        if (!Set.of("VALIDATING", "READY_FOR_REVIEW").contains(job.status())) {
            throw new StoryVersionException(
                    409,
                    "authoring_job_not_ready",
                    "O rascunho ainda não chegou à etapa de validação.");
        }
        var result = packs.validate(rawPackJson);
        if (!result.valid()) {
            throw new StoryVersionException(
                    422,
                    "story_contract_invalid",
                    "O rascunho não atende ao contrato executável da história.");
        }
        String hash = sha256(rawPackJson);
        Instant now = clock.instant();
        try {
            versions.insertReviewableDraft(
                    result.storyId(), result.version(), job.schoolId(), job.requestedByUserId(),
                    job.jobId(), rawPackJson, hash, now);
        } catch (DataIntegrityViolationException duplicate) {
            throw new StoryVersionException(
                    409,
                    "story_version_conflict",
                    "Esta versão da história já foi materializada.");
        }
        audit.append(job.requestedByUserId(), job.schoolId(), "STORY_VERSION_CREATED",
                "STORY_VERSION", target(result.storyId(), result.version()), now);
        return versions.findInSchool(result.storyId(), result.version(), job.schoolId())
                .map(StoryVersionService::view)
                .orElseThrow(() -> new IllegalStateException("story_version_materialization_missing"));
    }

    @Transactional
    public StoryVersionState approve(
            String oidcSubject,
            String schoolId,
            String storyId,
            int version,
            String idempotencyKey,
            ApprovalRequest request) {
        validateIdempotencyKey(idempotencyKey);
        if (!request.confirmedWarningIds().isEmpty()) {
            throw new StoryVersionException(
                    422,
                    "story_warning_unknown",
                    "Esta versão não possui avisos pendentes para confirmar.");
        }
        var grant = access.requireSchoolAction(oidcSubject, schoolId, InstitutionAction.CREATE_DRAFT);
        var current = readableBy(grant, storyId, version, schoolId);
        String fingerprint = fingerprint("APPROVE", request.expectedRevision(), request.confirmedWarningIds());
        var repeated = versions.findTransition(
                storyId, version, "APPROVE", grant.userId(), idempotencyKey);
        if (repeated.isPresent()) return repeated(current, repeated.orElseThrow(), fingerprint);
        if (current.revision() != request.expectedRevision()) {
            throw revisionConflict();
        }
        Instant now = clock.instant();
        if (!versions.approve(storyId, version, request.expectedRevision(), grant.userId(), now)) {
            throw invalidTransition("aprovar");
        }
        var approved = versions.findInSchool(storyId, version, schoolId).orElseThrow();
        versions.insertTransition(
                storyId, version, "APPROVE", grant.userId(), idempotencyKey,
                fingerprint, approved.revision(), now);
        audit.append(grant.userId(), schoolId, "STORY_VERSION_APPROVED", "STORY_VERSION",
                target(storyId, version), now);
        return view(approved);
    }

    @Transactional
    public StoryVersionState publish(
            String oidcSubject,
            String schoolId,
            String storyId,
            int version,
            String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        var grant = access.requireSchoolAction(oidcSubject, schoolId, InstitutionAction.CREATE_DRAFT);
        var current = readableBy(grant, storyId, version, schoolId);
        String fingerprint = fingerprint("PUBLISH", 0, List.of());
        var repeated = versions.findTransition(
                storyId, version, "PUBLISH", grant.userId(), idempotencyKey);
        if (repeated.isPresent()) return repeated(current, repeated.orElseThrow(), fingerprint);
        Instant now = clock.instant();
        if (!versions.publish(storyId, version, grant.userId(), now)) {
            throw invalidTransition("publicar");
        }
        var published = versions.findInSchool(storyId, version, schoolId).orElseThrow();
        versions.insertTransition(
                storyId, version, "PUBLISH", grant.userId(), idempotencyKey,
                fingerprint, published.revision(), now);
        audit.append(grant.userId(), schoolId, "STORY_VERSION_PUBLISHED", "STORY_VERSION",
                target(storyId, version), now);
        return view(published);
    }

    private StoryVersionStore.Version readableBy(
            InstitutionalAccessService.Grant grant, String storyId, int version, String schoolId) {
        var current = versions.findInSchool(storyId, version, schoolId)
                .orElseThrow(InstitutionalAccessService.AccessDeniedException::new);
        if (grant.role() == InstitutionRole.TEACHER
                && !current.authorUserId().equals(grant.userId())) {
            throw new InstitutionalAccessService.AccessDeniedException();
        }
        return current;
    }

    private StoryVersionState repeated(
            StoryVersionStore.Version current,
            StoryVersionStore.Transition existing,
            String fingerprint) {
        if (!existing.requestFingerprint().equals(fingerprint)) {
            throw new StoryVersionException(
                    409, "idempotency_conflict", "A chave já pertence a outra solicitação.");
        }
        return view(current);
    }

    private static StoryVersionState view(StoryVersionStore.Version item) {
        return new StoryVersionState(
                item.storyId(), item.version(), item.state(), item.packSha256(), item.updatedAt());
    }

    private static void validateIdempotencyKey(String key) {
        if (key == null || !IDEMPOTENCY_KEY.matcher(key).matches()) {
            throw new StoryVersionException(
                    400,
                    "invalid_idempotency_key",
                    "Use uma chave idempotente de 16 a 128 caracteres.");
        }
    }

    private static String fingerprint(String action, long revision, List<String> warningIds) {
        if (warningIds.stream().anyMatch(value -> value == null || !ID.matcher(value).matches())) {
            throw new StoryVersionException(
                    400, "invalid_warning_id", "Use identificadores de aviso válidos.");
        }
        String canonical = action + "|" + revision + "|" + warningIds.stream().sorted().toList();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static StoryVersionException revisionConflict() {
        return new StoryVersionException(
                409,
                "story_revision_conflict",
                "A versão foi alterada. Atualize a revisão antes de aprovar.");
    }

    private static StoryVersionException invalidTransition(String action) {
        return new StoryVersionException(
                409,
                "story_transition_invalid",
                "Esta versão não pode ser " + action + " neste estado.");
    }

    private static String target(String storyId, int version) {
        return storyId + "@" + version;
    }
}
