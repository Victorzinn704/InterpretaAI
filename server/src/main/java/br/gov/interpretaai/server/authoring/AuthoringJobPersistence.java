package br.gov.interpretaai.server.authoring;

import br.gov.interpretaai.server.identity.InstitutionAuditStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthoringJobPersistence {
    private final AuthoringJobStore jobs;
    private final InstitutionAuditStore audit;

    public AuthoringJobPersistence(
            AuthoringJobStore jobs,
            InstitutionAuditStore audit) {
        this.jobs = jobs;
        this.audit = audit;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(AuthoringJobStore.Job job) {
        jobs.insertWithQueue(job);
        audit.append(job.requestedByUserId(), job.schoolId(),
                "AUTHORING_JOB_CREATED", "AUTHORING_JOB", job.jobId(), job.createdAt());
    }
}
