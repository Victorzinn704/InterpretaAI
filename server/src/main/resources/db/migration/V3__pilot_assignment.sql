create table pilot_assignment (
    device_id varchar(64) primary key,
    version bigint not null,
    classroom_label varchar(30) not null,
    avatar_id varchar(16) not null,
    activity varchar(16) not null,
    drawing_prompt varchar(16) not null,
    updated_at timestamp with time zone not null
);
