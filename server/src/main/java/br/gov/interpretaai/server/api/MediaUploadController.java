package br.gov.interpretaai.server.api;

import br.gov.interpretaai.server.api.MediaUploadModels.CreateUploadRequest;
import br.gov.interpretaai.server.api.MediaUploadModels.UploadReceipt;
import br.gov.interpretaai.server.api.MediaUploadModels.UploadSession;
import br.gov.interpretaai.server.identity.AdultIdentity;
import br.gov.interpretaai.server.media.MediaUploadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Validated
@RestController
@RequestMapping("/api/v2/media/uploads")
public class MediaUploadController {
    private static final String ID_PATTERN = "[a-z0-9][a-z0-9_-]{2,63}";

    private final AdultIdentity identity;
    private final MediaUploadService uploads;

    public MediaUploadController(AdultIdentity identity, MediaUploadService uploads) {
        this.identity = identity;
        this.uploads = uploads;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UploadSession create(
            Authentication authentication,
            @RequestHeader("X-School-Id") @Pattern(regexp = ID_PATTERN) String schoolId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateUploadRequest request) {
        var upload = uploads.create(
                identity.subject(authentication), schoolId, idempotencyKey, request);
        String uploadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v2/media/uploads/{mediaId}/content")
                .buildAndExpand(upload.mediaId())
                .toUriString();
        return new UploadSession(
                upload.mediaId(), uploadUrl, upload.expiresAt(), upload.declaredBytes());
    }

    @PutMapping(
            path = "/{mediaId}/content",
            consumes = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, "image/webp"})
    public UploadReceipt upload(
            Authentication authentication,
            @RequestHeader("X-School-Id") @Pattern(regexp = ID_PATTERN) String schoolId,
            @PathVariable @Pattern(regexp = ID_PATTERN) String mediaId,
            @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
            HttpServletRequest request) throws IOException {
        return uploads.receive(
                identity.subject(authentication), schoolId, mediaId, contentType, request.getInputStream());
    }
}
