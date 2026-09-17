alter table media_sanitization_job
    add column sanitized_bytes bigint;

alter table media_sanitization_job
    add constraint chk_media_sanitization_bytes
    check (sanitized_bytes is null or sanitized_bytes > 0);

create table story_version_asset (
    story_id varchar(64) not null,
    story_version integer not null,
    asset_id varchar(64) not null,
    role varchar(16) not null check (role in ('PHONE', 'TABLET', 'THUMBNAIL', 'AUDIO')),
    media_id varchar(64) not null references media_upload_session(media_id),
    object_key varchar(255) not null,
    media_type varchar(32) not null check (media_type in (
        'image/jpeg', 'image/png', 'image/webp', 'audio/ogg', 'audio/wav'
    )),
    bytes bigint not null check (bytes > 0 and bytes <= 8388608),
    sha256 varchar(64) not null check (char_length(sha256) = 64),
    created_at timestamp with time zone not null,
    primary key (story_id, story_version, asset_id, role),
    foreign key (story_id, story_version) references story_version(story_id, version)
);

create index idx_story_version_asset_media
    on story_version_asset (media_id);
