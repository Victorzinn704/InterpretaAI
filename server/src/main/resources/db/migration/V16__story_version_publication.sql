create table story_version (
    story_id varchar(64) not null,
    version integer not null check (version >= 1),
    school_id varchar(64) not null references institution_school(school_id),
    author_user_id varchar(64) not null references institution_adult_user(user_id),
    authoring_job_id varchar(64) unique references authoring_job(job_id),
    pack_json text not null,
    pack_sha256 varchar(64) not null check (char_length(pack_sha256) = 64),
    state varchar(16) not null check (state in ('DRAFT', 'APPROVED', 'PUBLISHED', 'ARCHIVED')),
    revision bigint not null check (revision >= 1),
    approved_by_user_id varchar(64) references institution_adult_user(user_id),
    approved_at timestamp with time zone,
    published_by_user_id varchar(64) references institution_adult_user(user_id),
    published_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    primary key (story_id, version),
    unique (school_id, pack_sha256),
    check ((state = 'DRAFT' and approved_by_user_id is null and approved_at is null
            and published_by_user_id is null and published_at is null)
        or (state = 'APPROVED' and approved_by_user_id is not null and approved_at is not null
            and published_by_user_id is null and published_at is null)
        or (state = 'PUBLISHED' and approved_by_user_id is not null and approved_at is not null
            and published_by_user_id is not null and published_at is not null)
        or state = 'ARCHIVED')
);

create index idx_story_version_school_state
    on story_version (school_id, state, updated_at, story_id, version);

create table story_version_transition (
    story_id varchar(64) not null,
    version integer not null,
    action varchar(16) not null check (action in ('APPROVE', 'PUBLISH')),
    actor_user_id varchar(64) not null references institution_adult_user(user_id),
    idempotency_key varchar(128) not null,
    request_fingerprint varchar(64) not null,
    applied_revision bigint not null,
    created_at timestamp with time zone not null,
    primary key (story_id, version, action, actor_user_id, idempotency_key),
    foreign key (story_id, version) references story_version(story_id, version)
);

create index idx_story_version_transition_lookup
    on story_version_transition (story_id, version, action, actor_user_id);
