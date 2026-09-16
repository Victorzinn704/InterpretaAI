create table authoring_job (
    job_id varchar(64) primary key,
    school_id varchar(64) not null references institution_school(school_id),
    requested_by_user_id varchar(64) not null references institution_adult_user(user_id),
    idempotency_key varchar(128) not null,
    request_fingerprint varchar(64) not null,
    request_json text not null,
    source_type varchar(32) not null check (source_type in (
        'TEACHER_UPLOAD', 'APPROVED_LIBRARY', 'THEME'
    )),
    source_ref varchar(160) not null,
    status varchar(32) not null check (status in (
        'QUEUED', 'ANALYZING_MEDIA', 'NEEDS_TEACHER_INPUT',
        'RETRIEVING_GUIDANCE', 'GENERATING_STORY', 'GENERATING_MEDIA',
        'VALIDATING', 'READY_FOR_REVIEW', 'FAILED_RETRYABLE',
        'FAILED_FINAL', 'CANCELLED', 'APPROVED', 'PUBLISHED'
    )),
    progress_step varchar(80) not null,
    completed_steps integer not null check (completed_steps between 0 and 5),
    total_steps integer not null check (total_steps = 5),
    revision bigint not null default 1,
    failure_code varchar(64),
    failure_safe_message varchar(280),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    unique (requested_by_user_id, school_id, idempotency_key)
);

create index idx_authoring_job_school_time
    on authoring_job (school_id, updated_at, job_id);

create table authoring_job_queue (
    job_id varchar(64) primary key references authoring_job(job_id),
    status varchar(24) not null check (status in (
        'QUEUED', 'PROCESSING', 'RETRYABLE', 'DELIVERED', 'DEAD'
    )),
    attempts integer not null default 0,
    available_at timestamp with time zone not null,
    lease_until timestamp with time zone,
    last_error_code varchar(64),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_authoring_queue_ready
    on authoring_job_queue (status, available_at, job_id);
