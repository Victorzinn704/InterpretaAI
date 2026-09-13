package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.VoiceTurnModels.Speaker;

public interface SpeechProvider {
    SpeechAudio synthesize(String text, Speaker speaker);

    record SpeechAudio(byte[] content, String mimeType) {
        public static SpeechAudio silent() { return new SpeechAudio(new byte[0], ""); }
    }
}
