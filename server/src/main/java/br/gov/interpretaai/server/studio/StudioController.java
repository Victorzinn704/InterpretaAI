package br.gov.interpretaai.server.studio;

import br.gov.interpretaai.server.api.StoryVersionModels.ApprovalRequest;
import br.gov.interpretaai.server.api.DeliveryModels.Assignment;
import br.gov.interpretaai.server.api.DeliveryModels.AssignmentTarget;
import br.gov.interpretaai.server.api.DeliveryModels.AssignmentTargetType;
import br.gov.interpretaai.server.api.DeliveryModels.CreateAssignmentRequest;
import br.gov.interpretaai.server.api.StoryVersionModels.ReviewAsset;
import br.gov.interpretaai.server.api.StoryVersionModels.ReviewBundle;
import br.gov.interpretaai.server.api.StoryVersionModels.ReviewListItem;
import br.gov.interpretaai.server.api.StoryVersionModels.StoryVersionState;
import br.gov.interpretaai.server.identity.AdultIdentity;
import br.gov.interpretaai.server.identity.InstitutionalAccessService;
import br.gov.interpretaai.server.identity.InstitutionalAccessStore;
import br.gov.interpretaai.server.delivery.DeliveryService;
import br.gov.interpretaai.server.media.PrivateObjectStore;
import br.gov.interpretaai.server.story.StoryVersionException;
import br.gov.interpretaai.server.story.StoryVersionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.time.Instant;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** Session-backed BFF: OIDC tokens remain on the server, not in the teacher's JavaScript. */
@Validated
@RestController
@RequestMapping("/studio/api")
@ConditionalOnProperty(prefix = "interpretaai.studio", name = "enabled", havingValue = "true")
public class StudioController {
    private static final String ID = "[a-z0-9][a-z0-9_-]{2,63}";

    public record SchoolContext(String schoolId, String name, String role) {}
    public record AdultContext(List<SchoolContext> schools) {}
    public record CsrfValue(String token) {}
    public record AssignToClassroomRequest(
            @NotBlank @Pattern(regexp = ID) String classroomId, @NotNull Instant availableFrom) {}

    private final InstitutionalAccessService access;
    private final StoryVersionService stories;
    private final DeliveryService delivery;
    private final PrivateObjectStore objects;

    public StudioController(
            InstitutionalAccessService access, StoryVersionService stories,
            DeliveryService delivery, PrivateObjectStore objects) {
        this.access = access;
        this.stories = stories;
        this.delivery = delivery;
        this.objects = objects;
    }

    @GetMapping("/me")
    public ResponseEntity<AdultContext> me(Authentication authentication) {
        var schools = access.activeSchoolDisplays(subject(authentication)).stream()
                .map(context -> new SchoolContext(context.schoolId(), context.name(), context.role().name()))
                .toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new AdultContext(schools));
    }

    @GetMapping("/csrf")
    public ResponseEntity<CsrfValue> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new CsrfValue(token.getToken()));
    }

    @GetMapping("/schools/{schoolId}/reviews")
    public ResponseEntity<List<ReviewListItem>> list(
            Authentication authentication,
            @PathVariable @Pattern(regexp = ID) String schoolId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(stories.listReviewable(subject(authentication), schoolId));
    }

    @GetMapping("/schools/{schoolId}/classrooms")
    public ResponseEntity<List<InstitutionalAccessStore.ClassroomDisplay>> classrooms(
            Authentication authentication,
            @PathVariable @Pattern(regexp = ID) String schoolId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(access.activeClassrooms(subject(authentication), schoolId));
    }

    @GetMapping("/schools/{schoolId}/stories/{storyId}/versions/{version}/assignments")
    public ResponseEntity<List<Assignment>> assignments(
            Authentication authentication,
            @PathVariable @Pattern(regexp = ID) String schoolId,
            @PathVariable @Pattern(regexp = ID) String storyId,
            @PathVariable @Min(1) int version) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(delivery.activeForStory(subject(authentication), schoolId, storyId, version));
    }

    @PostMapping("/schools/{schoolId}/stories/{storyId}/versions/{version}/assignments")
    public Assignment assign(
            Authentication authentication,
            @PathVariable @Pattern(regexp = ID) String schoolId,
            @PathVariable @Pattern(regexp = ID) String storyId,
            @PathVariable @Min(1) int version,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AssignToClassroomRequest request) {
        return delivery.create(subject(authentication), schoolId, idempotencyKey,
                new CreateAssignmentRequest(storyId, version,
                        new AssignmentTarget(AssignmentTargetType.CLASSROOM, request.classroomId()),
                        request.availableFrom(), null, 50));
    }

    @GetMapping("/schools/{schoolId}/stories/{storyId}/versions/{version}/review")
    public ResponseEntity<ReviewBundle> review(
            Authentication authentication,
            @PathVariable @Pattern(regexp = ID) String schoolId,
            @PathVariable @Pattern(regexp = ID) String storyId,
            @PathVariable @Min(1) int version) {
        ReviewBundle source = stories.review(subject(authentication), schoolId, storyId, version);
        String base = "/studio/api/schools/" + schoolId + "/stories/" + storyId
                + "/versions/" + version + "/assets/";
        var previews = source.assets().stream().map(asset -> new ReviewAsset(
                asset.assetId(), asset.role(), asset.sha256(),
                base + asset.assetId() + "/" + asset.role())).toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new ReviewBundle(
                storyId, version, source.revision(), source.state(),
                source.packSha256(), source.packJson(), previews));
    }

    @GetMapping("/schools/{schoolId}/stories/{storyId}/versions/{version}/assets/{assetId}/{role}")
    public ResponseEntity<StreamingResponseBody> asset(
            Authentication authentication,
            @PathVariable @Pattern(regexp = ID) String schoolId,
            @PathVariable @Pattern(regexp = ID) String storyId,
            @PathVariable @Min(1) int version,
            @PathVariable @Pattern(regexp = ID) String assetId,
            @PathVariable @Pattern(regexp = "PHONE|TABLET|THUMBNAIL|AUDIO") String role) {
        var asset = stories.reviewAsset(
                subject(authentication), schoolId, storyId, version, assetId, role);
        InputStream input;
        try {
            input = objects.open(asset.objectKey());
        } catch (IOException unavailable) {
            throw new StoryVersionException(
                    503, "story_review_asset_unavailable", "A imagem está indisponível para revisão.");
        }
        StreamingResponseBody body = output -> {
            try (input) {
                input.transferTo(output);
            }
        };
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .eTag("\"" + asset.sha256() + "\"")
                .contentLength(asset.bytes())
                .contentType(MediaType.parseMediaType(asset.mediaType()))
                .body(body);
    }

    @PostMapping("/schools/{schoolId}/stories/{storyId}/versions/{version}/approve")
    public StoryVersionState approve(
            Authentication authentication,
            @PathVariable @Pattern(regexp = ID) String schoolId,
            @PathVariable @Pattern(regexp = ID) String storyId,
            @PathVariable @Min(1) int version,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ApprovalRequest request) {
        return stories.approve(
                subject(authentication), schoolId, storyId, version, idempotencyKey, request);
    }

    @PostMapping("/schools/{schoolId}/stories/{storyId}/versions/{version}/publish")
    public StoryVersionState publish(
            Authentication authentication,
            @PathVariable @Pattern(regexp = ID) String schoolId,
            @PathVariable @Pattern(regexp = ID) String storyId,
            @PathVariable @Min(1) int version,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return stories.publish(subject(authentication), schoolId, storyId, version, idempotencyKey);
    }

    private static String subject(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser user)
                || user.getSubject() == null || user.getSubject().isBlank()) {
            throw new AdultIdentity.UnauthenticatedAdultException();
        }
        return ((OidcUser) authentication.getPrincipal()).getSubject();
    }
}
