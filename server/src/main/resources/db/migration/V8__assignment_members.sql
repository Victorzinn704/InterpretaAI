create table pilot_assignment_member (
    device_id varchar(64) not null,
    learner_alias varchar(12) not null,
    avatar_id varchar(16) not null,
    roster_order integer not null,
    primary key (device_id, learner_alias),
    foreign key (device_id) references pilot_assignment(device_id) on delete cascade
);

insert into pilot_assignment_member (device_id, learner_alias, avatar_id, roster_order)
select device_id, learner_alias, avatar_id, 0 from pilot_assignment;
