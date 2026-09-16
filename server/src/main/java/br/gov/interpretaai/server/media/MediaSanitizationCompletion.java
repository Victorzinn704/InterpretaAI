package br.gov.interpretaai.server.media;

import br.gov.interpretaai.server.identity.InstitutionAuditStore;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MediaSanitizationCompletion {
    private final MediaSanitizationStore jobs;
    private final InstitutionAuditStore audit;

    public MediaSanitizationCompletion(
            MediaSanitizationStore jobs, InstitutionAuditStore audit) {
        this.jobs = jobs;
        this.audit = audit;
    }

    @Transactional
    public void ready(
            MediaSanitizationStore.Job job,
            String objectKey,
            String sha256,
            int width,
            int height,
            Instant now) {
        jobs.markReady(job.mediaId(), objectKey, sha256, width, height, now);
        audit.append(
                job.ownerUserId(), job.schoolId(), "MEDIA_SANITIZED", "MEDIA", job.mediaId(), now);
    }

    @Transactional
    public void reject(MediaSanitizationStore.Job job, String errorCode, Instant now) {
        jobs.reject(job.mediaId(), errorCode, now);
        audit.append(
                job.ownerUserId(), job.schoolId(), "MEDIA_REJECTED", "MEDIA", job.mediaId(), now);
    }
}
