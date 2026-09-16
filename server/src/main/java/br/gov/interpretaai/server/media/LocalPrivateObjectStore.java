package br.gov.interpretaai.server.media;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "interpretaai.media",
        name = "storage",
        havingValue = "local",
        matchIfMissing = true)
public class LocalPrivateObjectStore implements PrivateObjectStore {
    private final Path root;

    public LocalPrivateObjectStore(@Value("${interpretaai.media.local-root:./data/private-media}") Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public StagedObject stage(String mediaId, InputStream input, long maxBytes) throws IOException {
        Path stagingDirectory = contained("staging");
        Files.createDirectories(stagingDirectory);
        String stagingKey = "staging/" + mediaId + "-" + UUID.randomUUID() + ".part";
        Path target = contained(stagingKey);
        MessageDigest digest = sha256();
        long bytes = 0;
        try (OutputStream output = Files.newOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                bytes += read;
                if (bytes > maxBytes) throw new MediaUploadException(
                        413, "media_too_large", "A imagem ultrapassa o limite permitido.");
                output.write(buffer, 0, read);
                digest.update(buffer, 0, read);
            }
            return new StagedObject(stagingKey, bytes, HexFormat.of().formatHex(digest.digest()));
        } catch (IOException | RuntimeException error) {
            Files.deleteIfExists(target);
            throw error;
        }
    }

    @Override
    public void commit(StagedObject staged, String objectKey) throws IOException {
        Path source = contained(staged.stagingKey());
        Path target = contained(objectKey);
        Files.createDirectories(target.getParent());
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Override
    public void discard(StagedObject staged) {
        try {
            Files.deleteIfExists(contained(staged.stagingKey()));
        } catch (IOException ignored) {
            // A rotina de expurgo remove sobras; não esconda o resultado principal do upload.
        }
    }

    @Override
    public InputStream open(String objectKey) throws IOException {
        return Files.newInputStream(contained(objectKey));
    }

    private Path contained(String relative) {
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("invalid_private_object_key");
        return target;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
