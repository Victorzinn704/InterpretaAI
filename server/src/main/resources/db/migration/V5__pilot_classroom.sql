create table pilot_classroom (
    classroom_id varchar(64) primary key,
    classroom_label varchar(30) not null,
    updated_at timestamp with time zone not null
);

create table pilot_classroom_participant (
    classroom_id varchar(64) not null,
    learner_alias varchar(12) not null,
    avatar_id varchar(16) not null,
    device_id varchar(64) not null unique,
    primary key (classroom_id, learner_alias),
    foreign key (classroom_id) references pilot_classroom(classroom_id) on delete cascade
);
