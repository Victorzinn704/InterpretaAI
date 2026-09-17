create table story_pack_preparation_receipt (
    assignment_id varchar(64) not null references story_assignment(assignment_id),
    device_id varchar(64) not null references institution_device(device_id),
    pack_sha256 varchar(64) not null,
    first_confirmed_at timestamp with time zone not null,
    last_confirmed_at timestamp with time zone not null,
    primary key (assignment_id, device_id)
);

create index idx_story_pack_preparation_freshness
    on story_pack_preparation_receipt (assignment_id, last_confirmed_at);
