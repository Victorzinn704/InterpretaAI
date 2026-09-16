package br.gov.interpretaai.server.api;

import br.gov.interpretaai.server.api.AuthoringJobModels.AuthoringJob;
import br.gov.interpretaai.server.api.AuthoringJobModels.CreateAuthoringJobRequest;
import br.gov.interpretaai.server.authoring.AuthoringJobService;
import br.gov.interpretaai.server.identity.AdultIdentity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Validated
@RestController
@RequestMapping("/api/v2/authoring/jobs")
public class AuthoringJobController {
    private static final String ID = "[a-z0-9][a-z0-9_-]{2,63}";

    private final AdultIdentity identity;
    private final AuthoringJobService jobs;

    public AuthoringJobController(AdultIdentity identity, AuthoringJobService jobs) {
        this.identity = identity;
        this.jobs = jobs;
    }

    @PostMapping
    public ResponseEntity<AuthoringJob> create(
            Authentication authentication,
            @RequestHeader("X-School-Id") @Pattern(regexp = ID) String schoolId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateAuthoringJobRequest request) {
        var created = jobs.create(
                identity.subject(authentication), schoolId, idempotencyKey, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{jobId}").buildAndExpand(created.body().jobId()).toUri();
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .location(location)
                .eTag(etag(created))
                .body(created.body());
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<AuthoringJob> get(
            Authentication authentication,
            @RequestHeader("X-School-Id") @Pattern(regexp = ID) String schoolId,
            @PathVariable @Pattern(regexp = ID) String jobId,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        var found = jobs.get(identity.subject(authentication), schoolId, jobId);
        String etag = etag(found);
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }
        return ResponseEntity.ok().eTag(etag).body(found.body());
    }

    private static String etag(AuthoringJobService.JobView job) {
        return "\"" + job.body().jobId() + "-" + job.revision() + "\"";
    }
}
