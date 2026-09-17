package br.gov.interpretaai.server.story;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.gov.interpretaai.server.api.StoryVersionModels.ApprovalRequest;
import br.gov.interpretaai.server.api.StoryVersionModels.AssetConfirmation;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
        "interpretaai.conversation.provider=gemini",
        "interpretaai.gemini.api-key=",
        "interpretaai.speech.provider=kokoro",
        "interpretaai.kokoro.base-url=http://127.0.0.1:1",
        "spring.datasource.url=jdbc:h2:mem:story-materialization;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
class StoryVersionMaterializationTest {
    private static final Instant NOW = Instant.parse("2026-09-17T13:00:00Z");

    @Autowired StoryVersionService stories;
    @Autowired LearningStoryPackValidator validator;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void seedReadyAuthoringJob() {
        jdbc.update("delete from story_version_asset");
        jdbc.update("delete from story_version_transition");
        jdbc.update("delete from story_version");
        jdbc.update("delete from authoring_plan_queue");
        jdbc.update("delete from authoring_job_queue");
        jdbc.update("delete from authoring_job");
        jdbc.update("delete from media_sanitization_job");
        jdbc.update("delete from media_upload_session");
        jdbc.update("delete from institution_audit_event");
        jdbc.update("delete from institution_teacher_classroom");
        jdbc.update("delete from institution_school_membership");
        jdbc.update("delete from institution_classroom");
        jdbc.update("delete from institution_adult_user");
        jdbc.update("delete from institution_school");
        jdbc.update("delete from institution_tenant");
        jdbc.update("""
                insert into institution_tenant(tenant_id, name, status, created_at)
                values ('tenant_rio', 'Rede Rio', 'ACTIVE', ?)
                """, Timestamp.from(NOW));
        jdbc.update("""
                insert into institution_school(school_id, tenant_id, name, status, created_at)
                values ('school_centro', 'tenant_rio', 'Centro', 'ACTIVE', ?)
                """, Timestamp.from(NOW));
        jdbc.update("""
                insert into institution_adult_user(user_id, oidc_subject, status, created_at)
                values ('user_author', 'oidc|author', 'ACTIVE', ?)
                """, Timestamp.from(NOW));
        jdbc.update("""
                insert into institution_school_membership
                (user_id, school_id, role, status, created_at, updated_at)
                values ('user_author', 'school_centro', 'TEACHER', 'ACTIVE', ?, ?)
                """, Timestamp.from(NOW), Timestamp.from(NOW));
        jdbc.update("""
                insert into authoring_job
                (job_id, school_id, requested_by_user_id, idempotency_key, request_fingerprint,
                 request_json, source_type, source_ref, status, progress_step, completed_steps,
                 total_steps, revision, created_at, updated_at)
                values ('job_story_001', 'school_centro', 'user_author',
                        'materialization-job-key-0001', ?, '{}', 'THEME', 'maçã',
                        'VALIDATING', 'VALIDATING', 4, 5, 1, ?, ?)
                """, "c".repeat(64), Timestamp.from(NOW), Timestamp.from(NOW));
    }

    @Test
    void admitsOnlyAValidatedPackAndFreezesItsExactBytesAndHash() throws Exception {
        String pack = example();

        var state = stories.materializeValidatedDraft("job_story_001", pack);

        assertThat(state.storyId()).isEqualTo("historia_handoff_001");
        assertThat(state.version()).isEqualTo(1);
        assertThat(state.state()).isEqualTo("DRAFT");
        assertThat(state.packSha256()).isEqualTo(sha256(pack));
        assertThat(jdbc.queryForMap("""
                select pack_json, pack_sha256, state, authoring_job_id
                  from story_version where story_id = 'historia_handoff_001' and version = 1
                """)).containsEntry("PACK_JSON", pack)
                .containsEntry("PACK_SHA256", sha256(pack))
                .containsEntry("STATE", "DRAFT")
                .containsEntry("AUTHORING_JOB_ID", "job_story_001");
        assertThat(jdbc.queryForObject("""
                select count(*) from institution_audit_event
                 where action = 'STORY_VERSION_CREATED' and target_id = 'historia_handoff_001@1'
                """, Integer.class)).isEqualTo(1);

        assertThatThrownBy(() -> stories.materializeValidatedDraft("job_story_001", pack))
                .isInstanceOf(StoryVersionException.class)
                .extracting("code").isEqualTo("story_version_conflict");
    }

    @Test
    void rejectsInvalidDataBeforeItCanBecomeADraft() throws Exception {
        String invalid = example().replace("\"noRequiredScroll\":true", "\"noRequiredScroll\":false");

        assertThatThrownBy(() -> stories.materializeValidatedDraft("job_story_001", invalid))
                .isInstanceOf(StoryVersionException.class)
                .extracting("code").isEqualTo("story_contract_invalid");
        assertThat(jdbc.queryForObject("select count(*) from story_version", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from institution_audit_event", Integer.class)).isZero();
    }

    @Test
    void rejectsPrematureApprovalAndPublicationClaimsInADraft() {
        String premature = example().replace("\"sourceRefs\":[]",
                "\"approvedBy\":\"user_author\",\"approvedAt\":\"2026-09-17T13:00:00Z\",\"sourceRefs\":[]")
                .replace("\"assetOrigins\":[]}}",
                        "\"assetOrigins\":[]},\"publishedAt\":\"2026-09-17T13:00:00Z\"}");

        assertThatThrownBy(() -> stories.materializeValidatedDraft("job_story_001", premature))
                .isInstanceOf(StoryVersionException.class)
                .extracting("code").isEqualTo("story_contract_invalid");
        assertThat(jdbc.queryForObject("select count(*) from story_version", Integer.class)).isZero();
    }

    @Test
    void draftAndChildDeliveryHaveDifferentProvenanceGates() {
        String draft = assetPack("a".repeat(64));

        assertThat(validator.validateDraft(draft).valid()).isTrue();
        assertThat(validator.validate(draft).valid()).isFalse();
        assertThat(validator.validateDraft(
                draft.replace("\"reviewedByTeacher\":false", "\"reviewedByTeacher\":true"))
                .issues()).extracting(LearningStoryPackValidator.Issue::code)
                .contains("premature_asset_review");
    }

    @Test
    void assistedDraftCannotHideTheAbsenceOfPedagogicalSources() {
        String ungrounded = example().replace("\"createdBy\":\"TEACHER\"",
                "\"createdBy\":\"ASSISTED\"");

        assertThat(validator.validateDraft(ungrounded).issues())
                .extracting(LearningStoryPackValidator.Issue::code)
                .contains("assisted_source_missing");
        assertThatThrownBy(() -> stories.materializeValidatedDraft("job_story_001", ungrounded))
                .isInstanceOf(StoryVersionException.class)
                .extracting("code").isEqualTo("story_contract_invalid");
    }

    @Test
    void refusesToMaterializeBeforeTheAuthoringWorkflowReachesValidation() throws Exception {
        jdbc.update("update authoring_job set status = 'RETRIEVING_GUIDANCE' where job_id = 'job_story_001'");

        assertThatThrownBy(() -> stories.materializeValidatedDraft("job_story_001", example()))
                .isInstanceOf(StoryVersionException.class)
                .extracting("code").isEqualTo("authoring_job_not_ready");
        assertThat(jdbc.queryForObject("select count(*) from story_version", Integer.class)).isZero();
    }

    @Test
    void rejectsAnyDeclaredImageThatHasNoImmutableSanitizedBinding() {
        assertThatThrownBy(() -> stories.materializeValidatedDraft("job_story_001", assetPack("a".repeat(64))))
                .isInstanceOf(StoryVersionException.class)
                .extracting("code").isEqualTo("story_asset_binding_invalid");
        assertThat(jdbc.queryForObject("select count(*) from story_version", Integer.class)).isZero();
    }

    @Test
    void freezesOnlyABindingWhoseReadyDerivativeMatchesTheDeclaredMetadata() {
        String hash = "a".repeat(64);
        insertReadyMedia("media_apple_001", hash, 42L);

        stories.materializeValidatedDraft("job_story_001", assetPack(hash), List.of(
                new StoryVersionAssetStore.Binding("maca_objeto", "PHONE", "media_apple_001")
        ));

        assertThat(jdbc.queryForMap("""
                select media_id, object_key, media_type, bytes, sha256 from story_version_asset
                 where story_id = 'historia_asset_001' and story_version = 1
                """)).containsEntry("MEDIA_ID", "media_apple_001")
                .containsEntry("OBJECT_KEY", "sanitized/school_centro/media_apple_001/apple.png")
                .containsEntry("MEDIA_TYPE", "image/png")
                .containsEntry("BYTES", 42L)
                .containsEntry("SHA256", hash);
    }

    @Test
    void approvalRequiresEveryBoundImageAndFreezesRealReviewMetadata() throws Exception {
        String hash = "a".repeat(64);
        insertReadyMedia("media_apple_001", hash, 42L);
        String draft = assetPack(hash);
        stories.materializeValidatedDraft("job_story_001", draft, List.of(
                new StoryVersionAssetStore.Binding("maca_objeto", "PHONE", "media_apple_001")));

        var review = stories.review("oidc|author", "school_centro", "historia_asset_001", 1);
        assertThat(review.state()).isEqualTo("DRAFT");
        assertThat(review.packSha256()).isEqualTo(sha256(draft));
        assertThat(review.assets()).extracting(asset -> asset.assetId()).containsExactly("maca_objeto");
        assertThat(stories.reviewAsset("oidc|author", "school_centro", "historia_asset_001", 1,
                "maca_objeto", "PHONE").mediaId()).isEqualTo("media_apple_001");
        assertThatThrownBy(() -> stories.approve("oidc|author", "school_centro", "historia_asset_001", 1,
                "approve-without-image-0001", new ApprovalRequest(1L, sha256(draft), List.of(), List.of())))
                .isInstanceOf(StoryVersionException.class)
                .extracting("code").isEqualTo("story_assets_not_confirmed");
        assertThatThrownBy(() -> stories.approve("oidc|author", "school_centro", "historia_asset_001", 1,
                "approve-wrong-image-00001", new ApprovalRequest(1L, sha256(draft),
                        List.of(new AssetConfirmation("maca_objeto", "PHONE", "b".repeat(64))), List.of())))
                .isInstanceOf(StoryVersionException.class)
                .extracting("code").isEqualTo("story_assets_not_confirmed");

        var approved = stories.approve("oidc|author", "school_centro", "historia_asset_001", 1,
                "approve-with-image-00001", new ApprovalRequest(
                        1L, sha256(draft), List.of(new AssetConfirmation("maca_objeto", "PHONE", hash)), List.of()));
        var frozen = jdbc.queryForMap("""
                select pack_json, pack_sha256 from story_version
                 where story_id = 'historia_asset_001' and version = 1
                """);
        assertThat(approved.state()).isEqualTo("APPROVED");
        assertThat(frozen.get("PACK_JSON").toString())
                .contains("\"approvedBy\":\"user_author\"")
                .contains("\"reviewedByTeacher\":true");
        assertThat(frozen.get("PACK_SHA256")).isEqualTo(sha256(frozen.get("PACK_JSON").toString()));
        assertThat(frozen.get("PACK_SHA256")).isNotEqualTo(sha256(draft));
        assertThat(validator.validate(frozen.get("PACK_JSON").toString()).valid()).isTrue();
    }

    @Test
    void phoneApprovalCannotSilentlyApproveADifferentTabletImage() throws Exception {
        String phoneHash = "a".repeat(64);
        String tabletHash = "b".repeat(64);
        insertReadyMedia("media_phone_001", phoneHash, 42L);
        insertReadyMedia("media_tablet_001", tabletHash, 43L);
        ObjectNode pack = (ObjectNode) mapper.readTree(assetPack(phoneHash));
        ArrayNode variants = (ArrayNode) pack.path("assets").get(0).path("variants");
        variants.add(mapper.createObjectNode()
                .put("role", "TABLET").put("path", "images/maca_tablet.png")
                .put("mediaType", "image/png").put("width", 100).put("height", 100)
                .put("bytes", 43).put("sha256", tabletHash));
        String draft = mapper.writeValueAsString(pack);
        stories.materializeValidatedDraft("job_story_001", draft, List.of(
                new StoryVersionAssetStore.Binding("maca_objeto", "PHONE", "media_phone_001"),
                new StoryVersionAssetStore.Binding("maca_objeto", "TABLET", "media_tablet_001")));

        assertThatThrownBy(() -> stories.approve("oidc|author", "school_centro", "historia_asset_001", 1,
                "approve-phone-only-00001", new ApprovalRequest(1L, sha256(draft),
                        List.of(new AssetConfirmation("maca_objeto", "PHONE", phoneHash)), List.of())))
                .isInstanceOf(StoryVersionException.class)
                .extracting("code").isEqualTo("story_assets_not_confirmed");
        assertThat(stories.review("oidc|author", "school_centro", "historia_asset_001", 1)
                .assets()).extracting(asset -> asset.role()).containsExactly("PHONE", "TABLET");
    }

    @Test
    void rejectsABindingWhenDerivativeMetadataDiffersFromThePack() {
        insertReadyMedia("media_apple_001", "a".repeat(64), 42L);

        assertThatThrownBy(() -> stories.materializeValidatedDraft(
                "job_story_001", assetPack("b".repeat(64)), List.of(
                        new StoryVersionAssetStore.Binding("maca_objeto", "PHONE", "media_apple_001")
                )))
                .isInstanceOf(StoryVersionException.class)
                .extracting("code").isEqualTo("story_asset_binding_invalid");
        assertThat(jdbc.queryForObject("select count(*) from story_version", Integer.class)).isZero();
    }

    @Test
    void refusesABindingWhoseDerivativeIsNotReady() {
        assertThatThrownBy(() -> stories.materializeValidatedDraft(
                "job_story_001", assetPack("a".repeat(64)), List.of(
                        new StoryVersionAssetStore.Binding("maca_objeto", "PHONE", "media_missing_001")
                )))
                .isInstanceOf(StoryVersionException.class)
                .extracting("code").isEqualTo("story_asset_not_ready");
        assertThat(jdbc.queryForObject("select count(*) from story_version", Integer.class)).isZero();
    }

    private void insertReadyMedia(String mediaId, String hash, long bytes) {
        jdbc.update("""
                insert into media_upload_session
                (media_id, school_id, owner_user_id, idempotency_key, request_fingerprint,
                 original_file_name, media_type, declared_bytes, status, object_key,
                 actual_bytes, sha256, expires_at, created_at, updated_at)
                values (?, 'school_centro', 'user_author', ?, ?, 'apple.png', 'image/png', ?,
                        'UPLOADED', ?, ?, ?, ?, ?, ?)
                """, mediaId, "asset-binding-key-" + mediaId, "c".repeat(64), bytes,
                "raw/school_centro/" + mediaId + "/source.png", bytes, hash,
                Timestamp.from(NOW.plusSeconds(900)), Timestamp.from(NOW), Timestamp.from(NOW));
        jdbc.update("""
                insert into media_sanitization_job
                (media_id, status, attempts, available_at, sanitized_object_key, sanitized_sha256,
                 sanitized_bytes, width, height, output_media_type, created_at, updated_at)
                values (?, 'READY', 1, ?, ?, ?, ?, 100, 100, 'image/png', ?, ?)
                """, mediaId, Timestamp.from(NOW),
                "sanitized/school_centro/" + mediaId + "/apple.png", hash, bytes,
                Timestamp.from(NOW), Timestamp.from(NOW));
    }

    private String example() {
        return """
                {"schemaVersion":"1.0","packId":"pack_handoff_001","storyId":"historia_handoff_001",
                 "version":1,"minAppVersion":1,"title":"Conversa da turma","methodology":"LEIA",
                 "objectiveIds":["explicar_ideia"],"estimatedMinutes":3,"startNodeId":"conversa_001",
                 "nodes":[
                   {"id":"conversa_001","type":"GROUP_HANDOFF","objectiveIds":["explicar_ideia"],
                    "supports":[],"instruction":"Conte para sua dupla como pensou.","nextNodeId":"fim_001"},
                   {"id":"fim_001","type":"END","objectiveIds":["explicar_ideia"],"supports":[],
                    "closingSpeech":"Vocês terminaram a conversa."}],
                 "assets":[],"accessibility":{"minTouchTargetDp":48,"reducedStimuliSupported":true,
                 "spokenInstructions":true,"noRequiredScroll":true},"provenance":{"createdBy":"TEACHER",
                 "sourceRefs":[],"assetOrigins":[]}}
                """.replace("\n", "").replace("  ", "");
    }

    private String assetPack(String hash) {
        return """
                {"schemaVersion":"1.0","packId":"pack_asset_001","storyId":"historia_asset_001",
                 "version":1,"minAppVersion":1,"title":"A maçã da história","methodology":"LEIA",
                 "objectiveIds":["reconhecer_maca"],"estimatedMinutes":3,"startNodeId":"cena_asset_001",
                 "nodes":[
                   {"id":"cena_asset_001","type":"COMIC","objectiveIds":["reconhecer_maca"],
                    "visualAssetId":"maca_objeto","altText":"Uma maçã na cesta.",
                    "dialogue":[{"speaker":"LEIA_TEACHER","text":"Que fruta está na cesta?"}],
                    "supports":[],"nextNodeId":"fim_asset_001"},
                   {"id":"fim_asset_001","type":"END","objectiveIds":["reconhecer_maca"],"supports":[],
                    "closingSpeech":"Você encontrou a maçã."}],
                 "assets":[{"id":"maca_objeto","kind":"OBJECT_IMAGE","required":true,
                    "variants":[{"role":"PHONE","path":"images/maca.png","mediaType":"image/png",
                    "width":100,"height":100,"bytes":42,"sha256":"%s"}]}],
                 "accessibility":{"minTouchTargetDp":48,"reducedStimuliSupported":true,
                 "spokenInstructions":true,"noRequiredScroll":true},"provenance":{"createdBy":"TEACHER",
                 "sourceRefs":[],
                 "assetOrigins":[{"assetId":"maca_objeto","origin":"TEACHER_UPLOAD",
                 "reviewedByTeacher":false}]}}
                """.formatted(hash).replace("\n", "").replace("  ", "");
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
