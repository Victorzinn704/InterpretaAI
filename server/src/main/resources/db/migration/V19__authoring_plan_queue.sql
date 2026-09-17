create table authoring_plan_queue (
    job_id varchar(64) primary key references authoring_job(job_id),
    status varchar(24) not null check (status in (
        'QUEUED', 'PROCESSING', 'RETRYABLE', 'DELIVERED', 'PAUSED', 'DEAD'
    )),
    attempts integer not null default 0,
    available_at timestamp with time zone not null,
    lease_until timestamp with time zone,
    last_error_code varchar(64),
    plan_json text,
    plan_sha256 varchar(64),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    check ((status = 'DELIVERED' and plan_json is not null and plan_sha256 is not null)
        or (status <> 'DELIVERED' and plan_json is null and plan_sha256 is null))
);

create index idx_authoring_plan_ready
    on authoring_plan_queue (status, available_at, job_id);
