package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.VoiceTurnModels.PedagogicalReply;
import br.gov.interpretaai.server.api.VoiceTurnModels.Request;
import br.gov.interpretaai.server.api.VoiceTurnModels.Response;
import br.gov.interpretaai.server.core.SpeechProvider.SpeechAudio;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VoiceTurnService {
    private static final Logger log = LoggerFactory.getLogger(VoiceTurnService.class);
    private final ConversationProvider conversation;
    private final SpeechSynthesisService speech;
    private final SessionMemory memory;
    private final VoiceTurnIdempotency idempotency;
    private final ScenePackCatalog scenes;

    @Autowired
    public VoiceTurnService(ConversationProvider conversation, SpeechSynthesisService speech, SessionMemory memory,
            VoiceTurnIdempotency idempotency,
            ScenePackCatalog scenes) {
        this.conversation = conversation;
        this.speech = speech;
        this.memory = memory;
        this.idempotency = idempotency;
        this.scenes = scenes;
    }

    VoiceTurnService(ConversationProvider conversation, SpeechProvider speech, SessionMemory memory) {
        this(conversation, new SpeechSynthesisService(speech), memory, null, null);
    }

    VoiceTurnService(ConversationProvider conversation, SpeechProvider speech, SessionMemory memory,
            ScenePackCatalog scenes) {
        this(conversation, new SpeechSynthesisService(speech), memory, null, scenes);
    }

    public Response execute(Request request) {
        return execute(request, null);
    }

    public Response execute(Request request, String idempotencyKey) {
        return executeStreaming(request, idempotencyKey, ignored -> {});
    }

    public Response executeStreaming(
            Request request,
            String idempotencyKey,
            Consumer<Response> onFinalText) {
        if (idempotency == null || idempotencyKey == null || idempotencyKey.isBlank()) {
            return executeOnce(request, onFinalText);
        }
        AtomicBoolean generatedNow = new AtomicBoolean();
        Response response = idempotency.execute(idempotencyKey, request, () -> {
            generatedNow.set(true);
            return executeOnce(request, onFinalText);
        });
        if (!generatedNow.get()) onFinalText.accept(withoutAudio(response));
        return response;
    }

    private Response executeOnce(Request request, Consumer<Response> onFinalText) {
        long started = System.nanoTime();
        boolean conversationFallback = false;
        boolean speechFallback = false;
        PedagogicalReply reply;
        SpeechAudio audio;
        List<String> history = memory.appendAndRead(request.sessionId(), "criança: " + request.transcript());
        try {
            var prepared = scenes == null ? java.util.Optional.<PedagogicalReply>empty()
                    : scenes.deterministicReply(request);
            reply = prepared.orElseGet(() -> conversation.reply(request, history));
            if (prepared.isPresent()) {
                log.info("conversation_route provider=scene_pack outcome=success duration_ms={}",
                        (System.nanoTime() - started) / 1_000_000);
            }
        } catch (RuntimeException error) {
            conversationFallback = true;
            reply = new SafeFallbackConversationProvider().reply(request, history);
        }
        String safeText = ReplySafety.normalize(reply.replyText(), reply.nextAction());
        memory.appendAndRead(request.sessionId(), "LEIA: " + safeText);
        Response textResponse = new Response(safeText, request.speaker(), "", "",
                reply.visualReaction(), reply.nextAction(), reply.observationCategory(),
                conversationFallback);
        onFinalText.accept(textResponse);
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

    private Response withoutAudio(Response response) {
        return new Response(response.replyText(), response.speaker(), "", "",
                response.visualReaction(), response.nextAction(), response.observationCategory(),
                response.degraded());
    }
}
