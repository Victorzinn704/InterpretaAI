package br.gov.interpretaai.server.api;

import br.gov.interpretaai.server.api.DeliveryModels.Assignment;
import br.gov.interpretaai.server.api.DeliveryModels.CreateAssignmentRequest;
import br.gov.interpretaai.server.api.DeliveryModels.DeviceManifest;
import br.gov.interpretaai.server.api.DeliveryModels.ManifestItem;
import br.gov.interpretaai.server.delivery.DeliveryService;
import br.gov.interpretaai.server.device.DeviceAuthenticationToken;
import br.gov.interpretaai.server.device.DevicePairingService.DevicePrincipal;
import br.gov.interpretaai.server.identity.AdultIdentity;
import br.gov.interpretaai.server.identity.InstitutionalAccessService;
import br.gov.interpretaai.server.media.PrivateObjectStore;
import java.io.IOException;
import java.io.InputStream;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Validated
@RestController
@RequestMapping("/api/v2")
public class DeliveryController {
    private static final String ID = "[a-z0-9][a-z0-9_-]{2,63}";

    private final AdultIdentity identity;
    private final DeliveryService delivery;
    private final PrivateObjectStore objects;

    public DeliveryController(
            AdultIdentity identity, DeliveryService delivery, PrivateObjectStore objects) {
        this.identity = identity;
        this.delivery = delivery;
        this.objects = objects;
    }

    @PostMapping("/assignments")
    public ResponseEntity<Assignment> createAssignment(
            Authentication authentication,
            @RequestHeader("X-School-Id") @Pattern(regexp = ID) String schoolId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreateAssignmentRequest request) {
        Assignment created = delivery.create(identity.subject(authentication), schoolId, idempotencyKey, request);
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping("/devices/{deviceId}/manifest")
    public DeviceManifest manifest(
            Authentication authentication,
            @PathVariable @Pattern(regexp = ID) String deviceId,
            @RequestParam(required = false) @Size(max = 256) String after) {
        DevicePrincipal device = ownDevice(authentication, deviceId);
        var page = delivery.manifest(device, after);
        var items = page.items().stream().map(item -> new ManifestItem(
                item.assignmentId(), item.storyId(), item.storyVersion(), item.minAppVersion(),
                item.packSha256(), item.packJson().getBytes(StandardCharsets.UTF_8).length,
                item.priority(), packUrl(deviceId, item.assignmentId()), item.expiresAt())).toList();
        return new DeviceManifest(items, page.nextCursor(), page.serverTime());
    }

    @GetMapping(value = "/devices/{deviceId}/assignments/{assignmentId}/pack", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> pack(
            Authentication authentication,
            @PathVariable @Pattern(regexp = ID) String deviceId,
            @PathVariable @Pattern(regexp = ID) String assignmentId) {
        DevicePrincipal device = ownDevice(authentication, deviceId);
        var pack = delivery.pack(device, assignmentId);
        return ResponseEntity.ok()
                .eTag("\"" + pack.packSha256() + "\"")
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.APPLICATION_JSON)
                .body(pack.packJson());
    }

    @GetMapping("/devices/{deviceId}/assignments/{assignmentId}/assets/{assetId}/{role}")
    public ResponseEntity<StreamingResponseBody> asset(
            Authentication authentication,
            @PathVariable @Pattern(regexp = ID) String deviceId,
            @PathVariable @Pattern(regexp = ID) String assignmentId,
            @PathVariable @Pattern(regexp = ID) String assetId,
            @PathVariable @Pattern(regexp = "PHONE|TABLET|THUMBNAIL|AUDIO") String role) {
        DevicePrincipal device = ownDevice(authentication, deviceId);
        var asset = delivery.asset(device, assignmentId, assetId, role);
        InputStream input;
        try {
            input = objects.open(asset.objectKey());
        } catch (IOException unavailable) {
            throw new br.gov.interpretaai.server.delivery.DeliveryException(
                    503, "delivery_asset_unavailable", "O recurso está sendo verificado antes do envio.");
        }
        StreamingResponseBody body = output -> {
            try (input) {
                input.transferTo(output);
            }
        };
        return ResponseEntity.ok()
                .eTag("\"" + asset.sha256() + "\"")
                .cacheControl(CacheControl.noStore())
                .contentLength(asset.bytes())
                .contentType(MediaType.parseMediaType(asset.mediaType()))
                .body(body);
    }

    private static DevicePrincipal ownDevice(Authentication authentication, String deviceId) {
        if (!(authentication instanceof DeviceAuthenticationToken token)
                || !token.getPrincipal().deviceId().equals(deviceId)) {
            throw new InstitutionalAccessService.AccessDeniedException();
        }
        return token.getPrincipal();
    }

    private static String packUrl(String deviceId, String assignmentId) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v2/devices/{deviceId}/assignments/{assignmentId}/pack")
                .buildAndExpand(deviceId, assignmentId).toUriString();
    }
}
