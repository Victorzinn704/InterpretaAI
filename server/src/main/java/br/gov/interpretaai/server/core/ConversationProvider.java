package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.VoiceTurnModels.PedagogicalReply;
import br.gov.interpretaai.server.api.VoiceTurnModels.Request;
import java.util.List;

public interface ConversationProvider {
    PedagogicalReply reply(Request request, List<String> recentMessages);
}
