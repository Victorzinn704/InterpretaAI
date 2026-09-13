package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.VoiceTurnModels.NextAction;
import br.gov.interpretaai.server.api.VoiceTurnModels.PedagogicalReply;
import br.gov.interpretaai.server.api.VoiceTurnModels.Request;
import br.gov.interpretaai.server.api.VoiceTurnModels.VisualReaction;
import java.util.List;
public class SafeFallbackConversationProvider implements ConversationProvider {
    @Override
    public PedagogicalReply reply(Request request, List<String> recentMessages) {
        if (request.turn() >= 3) {
            return new PedagogicalReply(
                    "Você ajudou a LEIA a observar a história. Vamos continuar juntos!",
                    VisualReaction.CELEBRATE,
                    NextAction.CONTINUE,
                    "PARTICIPATION");
        }
        return new PedagogicalReply(
                "Gostei da sua ideia! O que mais você percebe nessa cena?",
                VisualReaction.ENCOURAGE,
                NextAction.SPEAK_AGAIN,
                "ORAL_EXPRESSION");
    }
}
