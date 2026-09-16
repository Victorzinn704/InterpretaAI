create table media_sanitization_job (
    media_id varchar(64) primary key references media_upload_session(media_id),
    status varchar(24) not null check (status in (
        'QUEUED', 'PROCESSING', 'RETRYABLE', 'READY', 'REJECTED', 'FAILED_FINAL'
    )),
    attempts integer not null default 0,
    available_at timestamp with time zone not null,
    lease_until timestamp with time zone,
    error_code varchar(64),
    sanitized_object_key varchar(255),
    sanitized_sha256 varchar(64),
    width integer,
    height integer,
    output_media_type varchar(32),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_media_sanitization_ready
    on media_sanitization_job (status, available_at, media_id);
