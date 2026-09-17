package br.gov.interpretaai.server.api;

import br.gov.interpretaai.server.api.StoryVersionModels.ApprovalRequest;
import br.gov.interpretaai.server.api.StoryVersionModels.StoryVersionState;
import br.gov.interpretaai.server.identity.AdultIdentity;
import br.gov.interpretaai.server.story.StoryVersionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Routes deliberately exclude draft ingress: generated content is admitted by the worker boundary. */
@Validated
@RestController
@RequestMapping("/api/v2/stories/{storyId}/versions/{version}")
public class StoryVersionController {
    private static final String ID = "[a-z0-9][a-z0-9_-]{2,63}";

    private final AdultIdentity identity;
    private final StoryVersionService stories;

    public StoryVersionController(AdultIdentity identity, StoryVersionService stories) {
        this.identity = identity;
        this.stories = stories;
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
