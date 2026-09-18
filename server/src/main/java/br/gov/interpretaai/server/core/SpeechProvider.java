package br.gov.interpretaai.server.core;

import br.gov.interpretaai.server.api.VoiceTurnModels.Speaker;
import java.util.Arrays;
import java.util.Objects;

public interface SpeechProvider {
    SpeechAudio synthesize(String text, Speaker speaker);

    record SpeechAudio(byte[] content, String mimeType) {
        public static SpeechAudio silent() { return new SpeechAudio(new byte[0], ""); }

        @Override
        public boolean equals(Object other) {
            return this == other || other instanceof SpeechAudio that
                    && Arrays.equals(content, that.content)
                    && Objects.equals(mimeType, that.mimeType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(content), mimeType);
        }

        @Override
        public String toString() {
            return "SpeechAudio[contentBytes=" + content.length + ", mimeType=" + mimeType + "]";
        }
    }
}
