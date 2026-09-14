create table voice_turn_idempotency (
    idempotency_key varchar(80) primary key,
    request_fingerprint varchar(64) not null,
    status varchar(16) not null,
    response_json text,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_voice_turn_idempotency_updated
    on voice_turn_idempotency (updated_at);
