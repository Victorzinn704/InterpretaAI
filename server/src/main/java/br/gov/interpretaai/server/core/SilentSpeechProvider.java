package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.VoiceTurnModels.Speaker;
public class SilentSpeechProvider implements SpeechProvider {
    @Override public SpeechAudio synthesize(String text, Speaker speaker) { return SpeechAudio.silent(); }
}
