create table institution_device (
    device_id varchar(64) primary key,
    school_id varchar(64) not null references institution_school(school_id),
    classroom_id varchar(64) not null references institution_classroom(classroom_id),
    installation_hash varchar(64) not null unique,
    label varchar(80) not null,
    status varchar(16) not null check (status in ('ACTIVE', 'REVOKED')),
    credential_hash varchar(64) not null,
    credential_version integer not null default 1,
    app_version integer not null,
    architecture varchar(16) not null check (architecture in ('ARM64', 'UNIVERSAL', 'UNKNOWN')),
    viewport_width_dp integer not null check (viewport_width_dp between 240 and 2000),
    viewport_height_dp integer not null check (viewport_height_dp between 320 and 3000),
    created_at timestamp with time zone not null,
    revoked_at timestamp with time zone
);

create index idx_institution_device_classroom
    on institution_device (classroom_id, status, device_id);

create table device_pairing_code (
    pairing_id varchar(64) primary key,
    code_hash varchar(64) not null unique,
    school_id varchar(64) not null references institution_school(school_id),
    classroom_id varchar(64) not null references institution_classroom(classroom_id),
    created_by_user_id varchar(64) not null references institution_adult_user(user_id),
    status varchar(16) not null check (status in ('ACTIVE', 'CONSUMED', 'CANCELLED')),
    expires_at timestamp with time zone not null,
    consumed_at timestamp with time zone,
    created_at timestamp with time zone not null
);

create index idx_device_pairing_expiry
    on device_pairing_code (status, expires_at, pairing_id);
