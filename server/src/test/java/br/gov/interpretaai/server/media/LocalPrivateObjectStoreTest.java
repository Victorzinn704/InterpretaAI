package br.gov.interpretaai.server.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalPrivateObjectStoreTest {
    @TempDir Path root;

    @Test
    void stagesHashesAndCommitsOnlyBelowThePrivateRoot() throws Exception {
        var store = new LocalPrivateObjectStore(root);

        var staged = store.stage(
                "media_test", new ByteArrayInputStream(new byte[] {1, 2, 3}), 3);
        assertThat(staged.bytes()).isEqualTo(3);
        assertThat(staged.sha256())
                .isEqualTo("039058c6f2c0cb492c533b0a4d14ef77cc0f78abccced5287d84a1a2011cfb81");

        store.commit(staged, "raw/school/media/hash");

        assertThat(Files.readAllBytes(root.resolve("raw/school/media/hash")))
                .containsExactly(1, 2, 3);
        assertThat(root.resolve(staged.stagingKey())).doesNotExist();
    }

    @Test
    void removesThePartialFileWhenTheStreamExceedsTheLimit() throws Exception {
        var store = new LocalPrivateObjectStore(root);

        assertThatThrownBy(() -> store.stage(
                        "media_large", new ByteArrayInputStream(new byte[] {1, 2, 3}), 2))
                .isInstanceOf(MediaUploadException.class)
                .extracting("code")
                .isEqualTo("media_too_large");
        try (var files = Files.list(root.resolve("staging"))) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    void rejectsTraversalInGeneratedStorageKeys() {
        var store = new LocalPrivateObjectStore(root);

        assertThatThrownBy(() -> store.stage(
                        "../../outside", new ByteArrayInputStream(new byte[] {1}), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid_private_object_key");
    }
}
