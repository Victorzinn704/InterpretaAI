package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.VoiceTurnModels.Speaker;

public interface SpeechProvider {
    byte[] synthesize(String text, Speaker speaker);
}
