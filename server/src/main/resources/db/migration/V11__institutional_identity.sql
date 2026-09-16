create table institution_tenant (
    tenant_id varchar(64) primary key,
    name varchar(160) not null,
    status varchar(16) not null check (status in ('ACTIVE', 'SUSPENDED')),
    created_at timestamp with time zone not null
);

create table institution_school (
    school_id varchar(64) primary key,
    tenant_id varchar(64) not null references institution_tenant(tenant_id),
    name varchar(160) not null,
    status varchar(16) not null check (status in ('ACTIVE', 'SUSPENDED')),
    created_at timestamp with time zone not null
);

create index idx_institution_school_tenant
    on institution_school (tenant_id, school_id);

create table institution_adult_user (
    user_id varchar(64) primary key,
    oidc_subject varchar(255) not null unique,
    status varchar(16) not null check (status in ('ACTIVE', 'REVOKED')),
    created_at timestamp with time zone not null
);

create table institution_school_membership (
    user_id varchar(64) not null references institution_adult_user(user_id),
    school_id varchar(64) not null references institution_school(school_id),
    role varchar(32) not null check (role in ('TEACHER', 'COORDINATOR', 'SCHOOL_ADMIN')),
    status varchar(16) not null check (status in ('ACTIVE', 'REVOKED')),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    primary key (user_id, school_id)
);

create index idx_institution_membership_school
    on institution_school_membership (school_id, status, user_id);

create table institution_classroom (
    classroom_id varchar(64) primary key,
    school_id varchar(64) not null references institution_school(school_id),
    name varchar(120) not null,
    status varchar(16) not null check (status in ('ACTIVE', 'ARCHIVED')),
    created_at timestamp with time zone not null
);

create index idx_institution_classroom_school
    on institution_classroom (school_id, status, classroom_id);

create table institution_teacher_classroom (
    user_id varchar(64) not null references institution_adult_user(user_id),
    classroom_id varchar(64) not null references institution_classroom(classroom_id),
    status varchar(16) not null check (status in ('ACTIVE', 'REVOKED')),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    primary key (user_id, classroom_id)
);

create index idx_institution_teacher_classroom_active
    on institution_teacher_classroom (classroom_id, status, user_id);
