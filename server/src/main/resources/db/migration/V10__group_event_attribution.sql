alter table pilot_learning_event alter column learner_alias drop not null;

alter table pilot_learning_event
    add column participation_scope varchar(12) not null default 'INDIVIDUAL';

alter table pilot_learning_event
    add column participant_count integer not null default 1;

alter table pilot_learning_event
    add constraint chk_pilot_participation_scope
    check (participation_scope in ('INDIVIDUAL', 'GROUP'));

alter table pilot_learning_event
    add constraint chk_pilot_participant_count
    check (participant_count between 1 and 4);
