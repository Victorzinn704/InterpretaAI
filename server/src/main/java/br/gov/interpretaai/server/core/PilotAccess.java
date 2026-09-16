package br.gov.interpretaai.server.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PilotAccess {
    private final boolean enabled;
    private final String teacherToken;
    private final String deviceToken;
    private final String secretaryToken;

    public PilotAccess(
            @Value("${interpretaai.pilot-sync.enabled:false}") boolean enabled,
            @Value("${interpretaai.pilot-sync.teacher-token:}") String teacherToken,
            @Value("${interpretaai.pilot-sync.device-token:}") String deviceToken,
            @Value("${interpretaai.pilot-sync.secretary-token:}") String secretaryToken) {
        this.enabled = enabled;
        this.teacherToken = teacherToken;
        this.deviceToken = deviceToken;
        this.secretaryToken = secretaryToken;
    }

    public void authorizeTeacher(String provided) {
        authorize(provided, teacherToken);
    }

    public void authorizeDevice(String provided) {
        authorize(provided, deviceToken);
    }

    public void authorizeSecretary(String provided) {
        authorize(provided, secretaryToken);
    }

    private void authorize(String provided, String configured) {
        if (!enabled || configured.length() < 16) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "pilot_sync_disabled");
        }
        byte[] expected = configured.getBytes(StandardCharsets.UTF_8);
        byte[] actual = provided == null ? new byte[0] : provided.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid_pilot_token");
        }
    }
}
