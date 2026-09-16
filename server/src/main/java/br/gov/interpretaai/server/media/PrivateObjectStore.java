package br.gov.interpretaai.server.media;

import java.io.IOException;
import java.io.InputStream;

public interface PrivateObjectStore {
    record StagedObject(String stagingKey, long bytes, String sha256) {}

    StagedObject stage(String mediaId, InputStream input, long maxBytes) throws IOException;

    void commit(StagedObject staged, String objectKey) throws IOException;

    void discard(StagedObject staged);
}
