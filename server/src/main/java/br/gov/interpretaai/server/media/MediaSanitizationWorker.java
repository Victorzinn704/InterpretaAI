package br.gov.interpretaai.server.media;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "interpretaai.media",
        name = "worker-enabled",
        havingValue = "true")
public class MediaSanitizationWorker {
    private static final int BATCH_SIZE = 4;
    private final MediaSanitizationProcessor processor;

    public MediaSanitizationWorker(MediaSanitizationProcessor processor) {
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${interpretaai.media.worker-poll-ms:500}")
    public void drain() {
        for (int index = 0; index < BATCH_SIZE; index++) {
            if (!processor.processOne()) return;
        }
    }
}
