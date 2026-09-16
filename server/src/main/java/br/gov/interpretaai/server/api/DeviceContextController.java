package br.gov.interpretaai.server.api;

import br.gov.interpretaai.server.api.DevicePairingModels.DeviceContext;
import br.gov.interpretaai.server.device.DevicePairingService.DevicePrincipal;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v2/devices")
public class DeviceContextController {
    @GetMapping("/{deviceId}/context")
    public DeviceContext context(
            Authentication authentication,
            @PathVariable @Pattern(regexp = "[a-z0-9][a-z0-9_-]{2,63}") String deviceId) {
        DevicePrincipal principal = (DevicePrincipal) authentication.getPrincipal();
        return new DeviceContext(
                principal.deviceId(), principal.schoolId(), principal.classroomId(), "ACTIVE");
    }
}
