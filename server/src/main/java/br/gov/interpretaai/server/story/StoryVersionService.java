package br.gov.interpretaai.server.story;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import br.gov.interpretaai.server.api.StoryVersionModels.ApprovalRequest;
import br.gov.interpretaai.server.api.StoryVersionModels.AssetConfirmation;
import br.gov.interpretaai.server.api.StoryVersionModels.ReviewAsset;
import br.gov.interpretaai.server.api.StoryVersionModels.ReviewBundle;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Human-only gate between a reviewable draft and any future device delivery. */
@Service
public class StoryVersionService {
    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("[A-Za-z0-9._:-]{16,128}");
    private static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9_-]{2,63}");
    private static final Pattern ROLE = Pattern.compile("PHONE|TABLET|THUMBNAIL|AUDIO");

    private final StoryVersionStore versions;
    private final StoryVersionAssetStore assets;
    private final AuthoringJobStore authoringJobs;
    private final LearningStoryPackValidator packs;
    private final ObjectMapper mapper;
    private final InstitutionalAccessService access;
    private final InstitutionAuditStore audit;
    private final Clock clock;

    public StoryVersionService(
            StoryVersionStore versions,
            StoryVersionAssetStore assets,
            AuthoringJobStore authoringJobs,
            LearningStoryPackValidator packs,
            ObjectMapper mapper,
            InstitutionalAccessService access,
            InstitutionAuditStore audit,
            Clock clock) {
        this.versions = versions;
        this.assets = assets;
        this.authoringJobs = authoringJobs;
        this.packs = packs;
        this.mapper = mapper;
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
        return materializeValidatedDraft(authoringJobId, rawPackJson, List.of());
    }

    /**
     * Internal ingress after a worker resolves every declared asset to a sanitized private
     * derivative. The binding type is deliberately not exposed in an adult HTTP request.
     */
    @Transactional
    public StoryVersionState materializeValidatedDraft(
            String authoringJobId,
            String rawPackJson,
            List<StoryVersionAssetStore.Binding> assetBindings) {
        var job = authoringJobs.findById(authoringJobId)
                .orElseThrow(() -> new StoryVersionException(
                        404, "authoring_job_not_found", "O trabalho de autoria não existe."));
        if (!Set.of("VALIDATING", "READY_FOR_REVIEW").contains(job.status())) {
            throw new StoryVersionException(
                    409,
                    "authoring_job_not_ready",
                    "O rascunho ainda não chegou à etapa de validação.");
        }
        var result = packs.validateDraft(rawPackJson);
        if (!result.valid()) {
            throw new StoryVersionException(
                    422,
                    "story_contract_invalid",
                    "O rascunho não atende ao contrato executável da história.");
        }
        List<StoryVersionAssetStore.BoundAsset> boundAssets = resolveAssetBindings(
                rawPackJson, job.schoolId(), assetBindings);
        String hash = sha256(rawPackJson);
        Instant now = clock.instant();
        try {
            versions.insertReviewableDraft(
                    result.storyId(), result.version(), job.schoolId(), job.requestedByUserId(),
                    job.jobId(), rawPackJson, hash, result.minAppVersion(), now);
            assets.insertAll(result.storyId(), result.version(), boundAssets, now);
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

    private List<StoryVersionAssetStore.BoundAsset> resolveAssetBindings(
            String rawPackJson,
            String schoolId,
            List<StoryVersionAssetStore.Binding> supplied) {
        Map<AssetKey, ExpectedAsset> expected = declaredAssetVariants(rawPackJson);
        Map<AssetKey, StoryVersionAssetStore.Binding> bindings = new LinkedHashMap<>();
        for (StoryVersionAssetStore.Binding binding : supplied == null
                ? List.<StoryVersionAssetStore.Binding>of() : supplied) {
            if (binding == null || !ID.matcher(binding.assetId()).matches()
                    || !ROLE.matcher(binding.role()).matches()
                    || !ID.matcher(binding.mediaId()).matches()) {
                throw invalidAssetBinding();
            }
            var key = new AssetKey(binding.assetId(), binding.role());
            if (bindings.putIfAbsent(key, binding) != null) throw invalidAssetBinding();
        }
        if (!bindings.keySet().equals(expected.keySet())) throw invalidAssetBinding();

        return expected.entrySet().stream().map(entry -> {
            ExpectedAsset declared = entry.getValue();
            var source = assets.findReadyMedia(bindings.get(entry.getKey()).mediaId(), schoolId)
                    .orElseThrow(StoryVersionService::assetNotReady);
            if (!declared.mediaType().equals(source.mediaType())
                    || declared.bytes() != source.bytes()
                    || !declared.sha256().equals(source.sha256())) {
                throw invalidAssetBinding();
            }
            return new StoryVersionAssetStore.BoundAsset(
                    entry.getKey().assetId(), entry.getKey().role(), source.mediaId(),
                    source.objectKey(), source.mediaType(), source.bytes(), source.sha256());
        }).toList();
    }

    private Map<AssetKey, ExpectedAsset> declaredAssetVariants(String rawPackJson) {
        try {
            JsonNode assetsNode = mapper.readTree(rawPackJson).path("assets");
            Map<AssetKey, ExpectedAsset> declared = new LinkedHashMap<>();
            for (JsonNode asset : assetsNode) {
                String assetId = asset.path("id").asText();
                for (JsonNode variant : asset.path("variants")) {
                    var key = new AssetKey(assetId, variant.path("role").asText());
                    var value = new ExpectedAsset(
                            variant.path("mediaType").asText(), variant.path("bytes").asLong(),
                            variant.path("sha256").asText());
                    if (declared.putIfAbsent(key, value) != null) throw invalidAssetBinding();
                }
            }
            return declared;
        } catch (StoryVersionException error) {
            throw error;
        } catch (Exception impossibleAfterValidation) {
            throw invalidAssetBinding();
        }
    }

    public ReviewBundle review(String oidcSubject, String schoolId, String storyId, int version) {
        var grant = access.requireSchoolAction(oidcSubject, schoolId, InstitutionAction.CREATE_DRAFT);
        var current = readableBy(grant, storyId, version, schoolId);
        var previews = assets.findForVersion(storyId, version).stream()
                .map(asset -> new ReviewAsset(asset.assetId(), asset.role(), asset.sha256(),
                        "/api/v2/stories/" + storyId + "/versions/" + version
                                + "/review/assets/" + asset.assetId() + "/" + asset.role()))
                .toList();
        return new ReviewBundle(storyId, version, current.revision(), current.state(),
                current.packSha256(), current.packJson(), previews);
    }

    public StoryVersionAssetStore.BoundAsset reviewAsset(
            String oidcSubject, String schoolId, String storyId, int version,
            String assetId, String role) {
        var grant = access.requireSchoolAction(oidcSubject, schoolId, InstitutionAction.CREATE_DRAFT);
        readableBy(grant, storyId, version, schoolId);
        return assets.findForVersion(storyId, version).stream()
                .filter(item -> item.assetId().equals(assetId) && item.role().equals(role))
                .findFirst().orElseThrow(() -> new StoryVersionException(
                        404, "story_asset_not_found", "A imagem não pertence a esta versão."));
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
        if (request.expectedPackSha256() == null || !request.expectedPackSha256().matches("[a-f0-9]{64}")
                || request.confirmedAssets() == null || request.confirmedAssets().stream()
                        .anyMatch(item -> item == null || item.assetId() == null
                                || !ID.matcher(item.assetId()).matches()
                                || item.role() == null || !ROLE.matcher(item.role()).matches()
                                || item.sha256() == null || !item.sha256().matches("[a-f0-9]{64}"))) {
            throw new StoryVersionException(400, "story_review_invalid", "A confirmação da revisão é inválida.");
        }
        String fingerprint = fingerprint("APPROVE", request.expectedRevision(),
                request.confirmedWarningIds(), request.expectedPackSha256(), request.confirmedAssets());
        var repeated = versions.findTransition(
                storyId, version, "APPROVE", grant.userId(), idempotencyKey);
        if (repeated.isPresent()) return repeated(current, repeated.orElseThrow(), fingerprint);
        if (current.revision() != request.expectedRevision()) {
            throw revisionConflict();
        }
        if (!current.packSha256().equals(request.expectedPackSha256())
                || !current.packSha256().equals(sha256(current.packJson()))) {
            throw new StoryVersionException(409, "story_review_stale",
                    "O rascunho mudou. Reabra a revisão antes de aprovar.");
        }
        if (!packs.validateDraft(current.packJson()).valid()) {
            throw new StoryVersionException(422, "story_contract_invalid",
                    "O rascunho não atende ao contrato executável da história.");
        }
        Set<AssetConfirmation> declaredVariants = declaredAssetVariants(current.packJson()).entrySet().stream()
                .map(entry -> new AssetConfirmation(
                        entry.getKey().assetId(), entry.getKey().role(), entry.getValue().sha256()))
                .collect(java.util.stream.Collectors.toSet());
        Set<AssetConfirmation> boundVariants = assets.findForVersion(storyId, version).stream()
                .map(item -> new AssetConfirmation(item.assetId(), item.role(), item.sha256()))
                .collect(java.util.stream.Collectors.toSet());
        List<AssetConfirmation> confirmed = request.confirmedAssets();
        if (!boundVariants.equals(declaredVariants) || confirmed.size() != declaredVariants.size()
                || !new HashSet<>(confirmed).equals(declaredVariants)) {
            throw new StoryVersionException(422, "story_assets_not_confirmed",
                    "Confirme cada variante de imagem desta versão antes de aprovar.");
        }
        Instant now = clock.instant();
        String deliveryJson = approvedSnapshot(current.packJson(), grant.userId(), now);
        if (!packs.validate(deliveryJson).valid()) {
            throw new StoryVersionException(422, "story_contract_invalid",
                    "O pacote aprovado não atende ao contrato executável da história.");
        }
        if (!versions.approve(storyId, version, request.expectedRevision(), current.packSha256(),
                deliveryJson, sha256(deliveryJson), grant.userId(), now)) {
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

    private static String fingerprint(
            String action, long revision, List<String> warningIds,
            String packSha256, List<AssetConfirmation> confirmedAssets) {
        return sha256(action + "|" + revision + "|" + warningIds.stream().sorted().toList()
                + "|" + packSha256 + "|" + confirmedAssets.stream()
                        .map(item -> item.assetId() + ":" + item.role() + ":" + item.sha256())
                        .sorted().toList());
    }

    private String approvedSnapshot(String draftJson, String actorUserId, Instant at) {
        try {
            ObjectNode root = (ObjectNode) mapper.readTree(draftJson);
            ObjectNode provenance = (ObjectNode) root.get("provenance");
            provenance.put("approvedBy", actorUserId);
            provenance.put("approvedAt", at.toString());
            for (JsonNode origin : provenance.path("assetOrigins")) {
                ((ObjectNode) origin).put("reviewedByTeacher", true);
            }
            return mapper.writeValueAsString(root);
        } catch (Exception impossibleAfterValidation) {
            throw new IllegalStateException("validated_draft_unreadable", impossibleAfterValidation);
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

    private static StoryVersionException invalidAssetBinding() {
        return new StoryVersionException(
                422,
                "story_asset_binding_invalid",
                "Cada recurso da história precisa apontar para uma mídia sanitizada idêntica.");
    }

    private static StoryVersionException assetNotReady() {
        return new StoryVersionException(
                409,
                "story_asset_not_ready",
                "Uma mídia da história ainda não está pronta para revisão.");
    }

    private record AssetKey(String assetId, String role) {}

    private record ExpectedAsset(String mediaType, long bytes, String sha256) {}

    private static String target(String storyId, int version) {
        return storyId + "@" + version;
    }
}
