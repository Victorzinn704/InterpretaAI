create table pilot_classroom_participant_v2 (
    classroom_id varchar(64) not null,
    learner_alias varchar(12) not null,
    avatar_id varchar(16) not null,
    device_id varchar(64) not null,
    primary key (classroom_id, learner_alias),
    foreign key (classroom_id) references pilot_classroom(classroom_id) on delete cascade
);

insert into pilot_classroom_participant_v2 (classroom_id, learner_alias, avatar_id, device_id)
select classroom_id, learner_alias, avatar_id, device_id from pilot_classroom_participant;

drop table pilot_classroom_participant;
alter table pilot_classroom_participant_v2 rename to pilot_classroom_participant;
create index idx_classroom_participant_device on pilot_classroom_participant (device_id);
