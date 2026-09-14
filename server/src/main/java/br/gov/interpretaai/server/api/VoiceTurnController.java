package br.gov.interpretaai.server.api;

import br.gov.interpretaai.server.api.VoiceTurnModels.Request;
import br.gov.interpretaai.server.api.VoiceTurnModels.Response;
import br.gov.interpretaai.server.core.VoiceTurnService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class VoiceTurnController {
    private final VoiceTurnService service;
    public VoiceTurnController(VoiceTurnService service) { this.service = service; }

    @PostMapping("/voice-turn")
    public ResponseEntity<Response> voiceTurn(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody Request request) {
        return ResponseEntity.ok(service.execute(request, idempotencyKey));
    }
}
