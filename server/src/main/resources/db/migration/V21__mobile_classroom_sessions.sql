create table institution_classroom_learner (
    learner_id varchar(64) primary key,
    classroom_id varchar(64) not null references institution_classroom(classroom_id),
    display_name varchar(80) not null,
    learner_alias varchar(12) not null,
    seat_number integer not null check (seat_number between 1 and 40),
    status varchar(16) not null check (status in ('ACTIVE', 'ARCHIVED')),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    unique (classroom_id, learner_alias),
    unique (classroom_id, seat_number)
);

create index idx_classroom_learner_active
    on institution_classroom_learner (classroom_id, status, seat_number);

create table classroom_session (
    session_id varchar(64) primary key,
    classroom_id varchar(64) not null references institution_classroom(classroom_id),
    created_by_user_id varchar(64) not null references institution_adult_user(user_id),
    join_code_hash varchar(64) not null unique,
    status varchar(16) not null check (status in ('ACTIVE', 'CLOSED', 'EXPIRED')),
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    closed_at timestamp with time zone
);

create index idx_classroom_session_code
    on classroom_session (join_code_hash, status, expires_at);

create table classroom_session_device (
    session_id varchar(64) not null references classroom_session(session_id) on delete cascade,
    device_id varchar(64) not null references institution_device(device_id),
    learner_id varchar(64) not null references institution_classroom_learner(learner_id),
    joined_at timestamp with time zone not null,
    primary key (session_id, device_id),
    unique (session_id, learner_id)
);

create index idx_classroom_session_device_active
    on classroom_session_device (device_id, session_id);
