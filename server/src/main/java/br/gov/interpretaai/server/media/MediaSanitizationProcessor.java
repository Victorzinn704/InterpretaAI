package br.gov.interpretaai.server.media;

import br.gov.interpretaai.server.media.ImageSanitizer.UnsafeImageException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MediaSanitizationProcessor {
    private static final Logger log = LoggerFactory.getLogger(MediaSanitizationProcessor.class);
    private static final Duration LEASE = Duration.ofSeconds(60);

    private final MediaSanitizationStore jobs;
    private final PrivateObjectStore objects;
    private final ImageSanitizer sanitizer;
    private final MediaSanitizationCompletion completion;
    private final Clock clock;

    public MediaSanitizationProcessor(
            MediaSanitizationStore jobs,
            PrivateObjectStore objects,
            ImageSanitizer sanitizer,
            MediaSanitizationCompletion completion,
            Clock clock) {
        this.jobs = jobs;
        this.objects = objects;
        this.sanitizer = sanitizer;
        this.completion = completion;
        this.clock = clock;
    }

    public boolean processOne() {
        var claimed = jobs.claimNext(clock.instant(), LEASE);
        if (claimed.isEmpty()) return false;
        var job = claimed.orElseThrow();
        try (var raw = objects.open(job.rawObjectKey())) {
            var clean = sanitizer.sanitize(raw, job.declaredMediaType());
            var staged = objects.stage(
                    "sanitized-" + job.mediaId(),
                    new ByteArrayInputStream(clean.png()),
                    clean.png().length);
            String objectKey = "sanitized/" + job.schoolId() + "/" + job.mediaId()
                    + "/" + staged.sha256() + ".png";
            try {
                objects.commit(staged, objectKey);
            } catch (IOException error) {
                objects.discard(staged);
                throw error;
            }
            var now = clock.instant();
            completion.ready(
                    job, objectKey, staged.sha256(), staged.bytes(), clean.width(), clean.height(), now);
            return true;
        } catch (UnsafeImageException unsafe) {
            var now = clock.instant();
            completion.reject(job, unsafe.code(), now);
            return true;
        } catch (IOException | RuntimeException transientFailure) {
            jobs.retryOrFail(job, "media_sanitization_failed", clock.instant());
            log.warn("media_sanitization_failed media={} attempt={}", job.mediaId(), job.attempts());
            return true;
        }
    }

}
