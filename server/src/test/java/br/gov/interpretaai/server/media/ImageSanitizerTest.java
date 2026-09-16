package br.gov.interpretaai.server.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class ImageSanitizerTest {
    private final ImageSanitizer sanitizer = new ImageSanitizer(100, 10_000, 1_048_576);

    @Test
    void decodesAndReencodesAStaticImageAsMetadataFreePng() throws Exception {
        byte[] source = image("png", 2, 3);

        var clean = sanitizer.sanitize(new ByteArrayInputStream(source), "image/png");

        assertThat(clean.width()).isEqualTo(2);
        assertThat(clean.height()).isEqualTo(3);
        assertThat(clean.inputMediaType()).isEqualTo("image/png");
        assertThat(clean.png()).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4e, (byte) 0x47);
        assertThat(ImageIO.read(new ByteArrayInputStream(clean.png())).getWidth()).isEqualTo(2);
    }

    @Test
    void acceptsJpegButStillNormalizesTheOutputToPng() throws Exception {
        var clean = sanitizer.sanitize(
                new ByteArrayInputStream(image("jpeg", 2, 2)), "image/jpeg");

        assertThat(clean.inputMediaType()).isEqualTo("image/jpeg");
        assertThat(clean.png()).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4e, (byte) 0x47);
    }

    @Test
    void rejectsMagicTypeMismatchInvalidBytesAndOversizedDimensions() throws Exception {
        byte[] smallPng = image("png", 2, 2);
        byte[] widePng = image("png", 101, 1);
        assertCode("image_format_mismatch", () -> sanitizer.sanitize(
                new ByteArrayInputStream(smallPng), "image/jpeg"));
        assertCode("image_unreadable", () -> sanitizer.sanitize(
                new ByteArrayInputStream(new byte[] {1, 2, 3}), "image/png"));
        assertCode("image_dimensions_rejected", () -> sanitizer.sanitize(
                new ByteArrayInputStream(widePng), "image/png"));
    }

    @Test
    void registersTheHardenedWebpReader() {
        assertThat(ImageIO.getImageReadersByFormatName("webp").hasNext()).isTrue();
    }

    private void assertCode(String code, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ImageSanitizer.UnsafeImageException.class)
                .extracting("code")
                .isEqualTo(code);
    }

    private byte[] image(String format, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(Color.GREEN);
            graphics.fillRect(0, 0, width, height);
        } finally {
            graphics.dispose();
        }
        try (var output = new ByteArrayOutputStream()) {
            assertThat(ImageIO.write(image, format, output)).isTrue();
            return output.toByteArray();
        } finally {
            image.flush();
        }
    }
}
