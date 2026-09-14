package br.gov.interpretaai.server.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class VoiceTurnModels {
    private VoiceTurnModels() {}

    public enum Speaker { LEIA_FEMALE, DAVI_MALE }
    public enum VisualReaction { CURIOUS, ENCOURAGE, CELEBRATE }
    public enum NextAction { SPEAK_AGAIN, CONTINUE }
    public enum StreamEventType { ACK, FINAL_TEXT, COMPLETE, FALLBACK }

    public record Request(
            @NotBlank @Size(max = 80) String sessionId,
            @NotBlank @Size(max = 80) String sceneId,
            @Min(1) @Max(3) int turn,
            @NotBlank @Size(max = 280) String transcript,
            @NotNull Speaker speaker,
            boolean reducedStimuli
    ) {}

    public record Response(
            String replyText,
            Speaker speaker,
            String audioBase64,
            String audioMimeType,
            VisualReaction visualReaction,
            NextAction nextAction,
            String observationCategory,
            boolean degraded
    ) {}

    public record PedagogicalReply(
            String replyText,
            VisualReaction visualReaction,
            NextAction nextAction,
            String observationCategory
    ) {}

    public record StreamEvent(StreamEventType type, Response response) {
        public static StreamEvent ack() { return new StreamEvent(StreamEventType.ACK, null); }
        public static StreamEvent finalText(Response response) {
            return new StreamEvent(StreamEventType.FINAL_TEXT, response);
        }
        public static StreamEvent complete(Response response) {
            return new StreamEvent(StreamEventType.COMPLETE, response);
        }
        public static StreamEvent fallback(Response response) {
            return new StreamEvent(StreamEventType.FALLBACK, response);
        }
    }
}
