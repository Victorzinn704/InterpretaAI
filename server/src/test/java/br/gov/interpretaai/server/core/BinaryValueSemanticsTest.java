package br.gov.interpretaai.server.core;

import static org.assertj.core.api.Assertions.assertThat;

import br.gov.interpretaai.server.media.ImageSanitizer.SanitizedImage;
import org.junit.jupiter.api.Test;

class BinaryValueSemanticsTest {
    @Test
    void speechAudioComparesBinaryContentInsteadOfArrayIdentity() {
        var first = new SpeechProvider.SpeechAudio(new byte[] {1, 2, 3}, "audio/ogg");
        var same = new SpeechProvider.SpeechAudio(new byte[] {1, 2, 3}, "audio/ogg");
        var different = new SpeechProvider.SpeechAudio(new byte[] {1, 2, 4}, "audio/ogg");

        assertThat(first).isEqualTo(same).hasSameHashCodeAs(same).isNotEqualTo(different);
        assertThat(first.toString()).contains("contentBytes=3", "audio/ogg");
    }

    @Test
    void sanitizedImageComparesBinaryContentAndDimensions() {
        var first = new SanitizedImage(new byte[] {4, 5}, 3, 2, "image/png");
        var same = new SanitizedImage(new byte[] {4, 5}, 3, 2, "image/png");
        var different = new SanitizedImage(new byte[] {4, 6}, 3, 2, "image/png");

        assertThat(first).isEqualTo(same).hasSameHashCodeAs(same).isNotEqualTo(different);
        assertThat(first.toString()).contains("pngBytes=2", "width=3", "height=2", "image/png");
    }
}
