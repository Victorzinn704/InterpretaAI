package br.gov.interpretaai.server.media;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageSanitizer {
    public record SanitizedImage(byte[] png, int width, int height, String inputMediaType) {}

    public static final class UnsafeImageException extends RuntimeException {
        private final String code;

        UnsafeImageException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String code() {
            return code;
        }
    }

    private final int maxDimension;
    private final long maxPixels;
    private final long maxOutputBytes;

    public ImageSanitizer(
            @Value("${interpretaai.media.max-dimension:4096}") int maxDimension,
            @Value("${interpretaai.media.max-pixels:16000000}") long maxPixels,
            @Value("${interpretaai.media.max-sanitized-bytes:12582912}") long maxOutputBytes) {
        this.maxDimension = maxDimension;
        this.maxPixels = maxPixels;
        this.maxOutputBytes = maxOutputBytes;
    }

    public SanitizedImage sanitize(InputStream raw, String declaredMediaType) {
        try (ImageInputStream imageInput = ImageIO.createImageInputStream(raw)) {
            if (imageInput == null) throw unsafe("image_unreadable", "Imagem não reconhecida.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) throw unsafe("image_unreadable", "Imagem não reconhecida.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, false, true);
                String detectedMediaType = mediaType(reader.getFormatName());
                if (!detectedMediaType.equals(declaredMediaType)) {
                    throw unsafe("image_format_mismatch", "O conteúdo não corresponde ao tipo declarado.");
                }
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width < 1 || height < 1
                        || width > maxDimension || height > maxDimension
                        || (long) width * height > maxPixels) {
                    throw unsafe("image_dimensions_rejected", "As dimensões da imagem não são permitidas.");
                }
                int imageCount = reader.getNumImages(true);
                if (imageCount != 1) {
                    throw unsafe("animated_image_rejected", "Envie uma imagem estática.");
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) throw unsafe("image_unreadable", "Imagem não reconhecida.");
                boolean alpha = decoded.getColorModel().hasAlpha();
                BufferedImage clean = new BufferedImage(
                        width, height, alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = clean.createGraphics();
                try {
                    graphics.drawImage(decoded, 0, 0, null);
                } finally {
                    graphics.dispose();
                    decoded.flush();
                }
                try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    if (!ImageIO.write(clean, "png", output)) {
                        throw unsafe("image_encode_failed", "Não foi possível normalizar a imagem.");
                    }
                    clean.flush();
                    if (output.size() > maxOutputBytes) {
                        throw unsafe("image_output_too_large", "A imagem normalizada excede o limite.");
                    }
                    return new SanitizedImage(output.toByteArray(), width, height, detectedMediaType);
                }
            } finally {
                reader.dispose();
            }
        } catch (UnsafeImageException error) {
            throw error;
        } catch (IOException | RuntimeException error) {
            throw unsafe("image_decode_failed", "Não foi possível processar a imagem.");
        }
    }

    private String mediaType(String formatName) {
        return switch (formatName.toUpperCase(Locale.ROOT)) {
            case "JPEG", "JPG" -> "image/jpeg";
            case "PNG" -> "image/png";
            case "WEBP" -> "image/webp";
            default -> throw unsafe("image_format_rejected", "O formato da imagem não é permitido.");
        };
    }

    private UnsafeImageException unsafe(String code, String message) {
        return new UnsafeImageException(code, message);
    }
}
