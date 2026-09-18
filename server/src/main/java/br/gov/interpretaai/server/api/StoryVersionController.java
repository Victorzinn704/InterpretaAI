package br.gov.interpretaai.server.api;

import br.gov.interpretaai.server.api.StoryVersionModels.ApprovalRequest;
import br.gov.interpretaai.server.api.StoryVersionModels.ReviewBundle;
import br.gov.interpretaai.server.api.StoryVersionModels.StoryVersionState;
import br.gov.interpretaai.server.identity.AdultIdentity;
import br.gov.interpretaai.server.media.PrivateObjectStore;
import br.gov.interpretaai.server.story.StoryVersionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** Routes deliberately exclude draft ingress: generated content is admitted by the worker boundary. */
@Validated
@RestController
@RequestMapping("/api/v2/stories/{storyId}/versions/{version}")
public class StoryVersionController {
    private static final String ID = "[a-z0-9][a-z0-9_-]{2,63}";

    private final AdultIdentity identity;
    private final StoryVersionService stories;
    private final PrivateObjectStore objects;

    public StoryVersionController(
            AdultIdentity identity, StoryVersionService stories, PrivateObjectStore objects) {
        this.identity = identity;
        this.stories = stories;
        this.objects = objects;
    }

    @GetMapping("/review")
    public ResponseEntity<ReviewBundle> review(
            Authentication authentication,
            @RequestHeader("X-School-Id") @Pattern(regexp = ID) String schoolId,
            @PathVariable @Pattern(regexp = ID) String storyId,
            @PathVariable @Min(1) int version) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(stories.review(identity.subject(authentication), schoolId, storyId, version));
    }

    @GetMapping("/review/assets/{assetId}/{role}")
    public ResponseEntity<StreamingResponseBody> reviewAsset(
            Authentication authentication,
            @RequestHeader("X-School-Id") @Pattern(regexp = ID) String schoolId,
            @PathVariable @Pattern(regexp = ID) String storyId,
            @PathVariable @Min(1) int version,
            @PathVariable @Pattern(regexp = ID) String assetId,
            @PathVariable @Pattern(regexp = "PHONE|TABLET|THUMBNAIL|AUDIO") String role) {
        var asset = stories.reviewAsset(
                identity.subject(authentication), schoolId, storyId, version, assetId, role);
        try (InputStream ignored = objects.open(asset.objectKey())) {
            // Verify availability before committing the HTTP response.
        } catch (IOException unavailable) {
            throw new br.gov.interpretaai.server.story.StoryVersionException(
                    503, "story_review_asset_unavailable", "A imagem está indisponível para revisão.");
        }
        StreamingResponseBody body = output -> {
            try (InputStream input = objects.open(asset.objectKey())) {
                input.transferTo(output);
            }
        };
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .eTag("\"" + asset.sha256() + "\"")
                .contentLength(asset.bytes())
                .contentType(MediaType.parseMediaType(asset.mediaType()))
                .body(body);
    }

    @PostMapping("/approve")
    public ResponseEntity<StoryVersionState> approve(
            Authentication authentication,
            @RequestHeader("X-School-Id") @Pattern(regexp = ID) String schoolId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable @Pattern(regexp = ID) String storyId,
            @PathVariable @Min(1) int version,
            @Valid @RequestBody ApprovalRequest request) {
        return ResponseEntity.ok(stories.approve(
                identity.subject(authentication), schoolId, storyId, version, idempotencyKey, request));
    }

    @PostMapping("/publish")
    public ResponseEntity<StoryVersionState> publish(
            Authentication authentication,
            @RequestHeader("X-School-Id") @Pattern(regexp = ID) String schoolId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @PathVariable @Pattern(regexp = ID) String storyId,
            @PathVariable @Min(1) int version) {
        return ResponseEntity.ok(stories.publish(
                identity.subject(authentication), schoolId, storyId, version, idempotencyKey));
    }
}
