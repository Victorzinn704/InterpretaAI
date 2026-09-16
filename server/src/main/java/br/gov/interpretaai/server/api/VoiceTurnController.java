package br.gov.interpretaai.server.api;

import br.gov.interpretaai.server.api.VoiceTurnModels.NextAction;
import br.gov.interpretaai.server.api.VoiceTurnModels.Request;
import br.gov.interpretaai.server.api.VoiceTurnModels.Response;
import br.gov.interpretaai.server.api.VoiceTurnModels.StreamEvent;
import br.gov.interpretaai.server.api.VoiceTurnModels.VisualReaction;
import br.gov.interpretaai.server.core.VoiceTurnService;
import br.gov.interpretaai.server.core.PilotAccess;
import br.gov.interpretaai.server.core.VoiceTurnRateLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1")
public class VoiceTurnController {
    private final VoiceTurnService service;
    private final ObjectMapper json;
    private final PilotAccess access;
    private final boolean authenticationEnabled;
    private final VoiceTurnRateLimiter rateLimiter;

    public VoiceTurnController(
            VoiceTurnService service,
            ObjectMapper json,
            PilotAccess access,
            VoiceTurnRateLimiter rateLimiter,
            @Value("${interpretaai.voice-auth.enabled:false}") boolean authenticationEnabled) {
        this.service = service;
        this.json = json;
        this.access = access;
        this.rateLimiter = rateLimiter;
        this.authenticationEnabled = authenticationEnabled;
    }

    @PostMapping("/voice-turn")
    public ResponseEntity<Response> voiceTurn(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            @Valid @RequestBody Request request) {
        authorize(deviceToken);
        rateLimiter.check(request.sessionId());
        return ResponseEntity.ok(service.execute(request, idempotencyKey));
    }

    @PostMapping(value = "/voice-turn/stream", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public ResponseEntity<StreamingResponseBody> voiceTurnStream(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            @Valid @RequestBody Request request) {
        authorize(deviceToken);
        rateLimiter.check(request.sessionId());
        StreamingResponseBody body = output -> {
            long streamStarted = System.nanoTime();
            write(output, StreamEvent.ack(elapsedMs(streamStarted)));
            try {
                Response response = service.executeStreaming(request, idempotencyKey,
                        partial -> writeUnchecked(output,
                                StreamEvent.finalText(elapsedMs(streamStarted), partial)));
                write(output, StreamEvent.complete(elapsedMs(streamStarted), response));
            } catch (UncheckedIOException disconnected) {
                throw disconnected.getCause();
            } catch (RuntimeException error) {
                Response fallback = new Response(
                        "A LEIA está sem internet, mas continua com você.",
                        request.speaker(), "", "", VisualReaction.ENCOURAGE,
                        request.turn() >= 3 ? NextAction.CONTINUE : NextAction.SPEAK_AGAIN,
                        "ORAL_EXPRESSION", true);
                write(output, StreamEvent.fallback(elapsedMs(streamStarted), fallback));
            }
        };
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Accel-Buffering", "no")
                .contentType(MediaType.APPLICATION_NDJSON)
                .body(body);
    }

    private void authorize(String deviceToken) {
        if (authenticationEnabled) access.authorizeDevice(deviceToken);
    }

    private void writeUnchecked(OutputStream output, StreamEvent event) {
        try {
            write(output, event);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }

    private void write(OutputStream output, StreamEvent event) throws IOException {
        output.write(json.writeValueAsBytes(event));
        output.write('\n');
        output.flush();
    }

    private long elapsedMs(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }
}
