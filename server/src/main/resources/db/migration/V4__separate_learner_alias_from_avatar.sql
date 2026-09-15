alter table pilot_assignment
    add column learner_alias varchar(12);

update pilot_assignment
   set learner_alias = avatar_id || '-01';

alter table pilot_assignment
    alter column learner_alias set not null;
