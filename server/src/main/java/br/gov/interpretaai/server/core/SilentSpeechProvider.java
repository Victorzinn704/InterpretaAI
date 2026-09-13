package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.VoiceTurnModels.Speaker;
public class SilentSpeechProvider implements SpeechProvider {
    @Override public byte[] synthesize(String text, Speaker speaker) { return new byte[0]; }
}
