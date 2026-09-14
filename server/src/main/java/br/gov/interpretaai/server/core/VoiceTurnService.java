package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.VoiceTurnModels.PedagogicalReply;
import br.gov.interpretaai.server.api.VoiceTurnModels.Request;
import br.gov.interpretaai.server.api.VoiceTurnModels.Response;
import java.util.Base64;
import java.util.List;
import br.gov.interpretaai.server.core.SpeechProvider.SpeechAudio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VoiceTurnService {
    private static final Logger log = LoggerFactory.getLogger(VoiceTurnService.class);
    private final ConversationProvider conversation;
    private final SpeechProvider speech;
    private final SessionMemory memory;
    private final ConversationDeadline deadline;
    private final VoiceTurnIdempotency idempotency;

    @Autowired
    public VoiceTurnService(ConversationProvider conversation, SpeechProvider speech, SessionMemory memory,
            ConversationDeadline deadline, VoiceTurnIdempotency idempotency) {
        this.conversation = conversation;
        this.speech = speech;
        this.memory = memory;
        this.deadline = deadline;
        this.idempotency = idempotency;
    }

    VoiceTurnService(ConversationProvider conversation, SpeechProvider speech, SessionMemory memory,
            ConversationDeadline deadline) {
        this(conversation, speech, memory, deadline, null);
    }

    public Response execute(Request request) {
        return execute(request, null);
    }

    public Response execute(Request request, String idempotencyKey) {
        if (idempotency == null || idempotencyKey == null || idempotencyKey.isBlank()) {
            return executeOnce(request);
        }
        return idempotency.execute(idempotencyKey, request, () -> executeOnce(request));
    }

    private Response executeOnce(Request request) {
        long started = System.nanoTime();
        boolean conversationFallback = false;
        boolean speechFallback = false;
        PedagogicalReply reply;
        SpeechAudio audio;
        List<String> history = memory.appendAndRead(request.sessionId(), "criança: " + request.transcript());
        try {
            reply = deadline.call(() -> conversation.reply(request, history));
        } catch (RuntimeException error) {
            conversationFallback = true;
            reply = new SafeFallbackConversationProvider().reply(request, history);
        }
        String safeText = ReplySafety.normalize(reply.replyText(), reply.nextAction());
        memory.appendAndRead(request.sessionId(), "LEIA: " + safeText);
        try {
            audio = speech.synthesize(safeText, request.speaker());
            speechFallback = audio.content().length == 0;
        } catch (RuntimeException error) {
            speechFallback = true;
            audio = SpeechAudio.silent();
        }
        long durationMs = (System.nanoTime() - started) / 1_000_000;
        boolean degraded = conversationFallback || speechFallback;
        log.info("voice_turn status=ok duration_ms={} turn={} conversation_fallback={} speech_fallback={}",
                durationMs, request.turn(), conversationFallback, speechFallback);
        return new Response(safeText, request.speaker(), Base64.getEncoder().encodeToString(audio.content()),
                audio.mimeType(), reply.visualReaction(), reply.nextAction(),
                reply.observationCategory(), degraded);
    }
}
