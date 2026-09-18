#!/usr/bin/env bash
set -euo pipefail

runtime_env="${RUNTIME_ENV:-/etc/interpretaai/keycloak-runtime.env}"
app_env="${APP_ENV:-/etc/interpretaai/v2-staging.env}"

[[ "${EUID}" -eq 0 ]] || { echo "Execute como root." >&2; exit 2; }
[[ -f "$runtime_env" ]] || { echo "Ambiente Keycloak ausente." >&2; exit 2; }
[[ -f "$app_env" ]] || { echo "Ambiente da aplicacao ausente: $app_env" >&2; exit 2; }

set -a
# shellcheck disable=SC1090
. "$runtime_env"
# shellcheck disable=SC1090
. "$app_env"
set +a

[[ -n "${PILOT_TEACHER_SUBJECT:-}" ]] || { echo "Subject piloto ausente." >&2; exit 2; }
db_url="${DATABASE_URL#jdbc:}"

PGPASSWORD="$DATABASE_PASSWORD" psql "$db_url" -U "$DATABASE_USERNAME" \
  -v ON_ERROR_STOP=1 -v pilot_subject="$PILOT_TEACHER_SUBJECT" <<'SQL'
begin;
insert into institution_tenant(tenant_id,name,status,created_at)
values ('tenant_pilot','InterpretaAI Piloto','ACTIVE',now())
on conflict (tenant_id) do update set name=excluded.name,status='ACTIVE';

insert into institution_school(school_id,tenant_id,name,status,created_at)
values ('school_pilot','tenant_pilot','Escola Piloto InterpretaAI','ACTIVE',now())
on conflict (school_id) do update set name=excluded.name,status='ACTIVE';

insert into institution_classroom(classroom_id,school_id,name,status,created_at)
values ('class_pilot','school_pilot','Turma Piloto','ACTIVE',now())
on conflict (classroom_id) do update set name=excluded.name,status='ACTIVE';

insert into institution_adult_user(user_id,oidc_subject,status,created_at)
values ('user_teacher_pilot',:'pilot_subject','ACTIVE',now())
on conflict (user_id) do update set oidc_subject=excluded.oidc_subject,status='ACTIVE';

insert into institution_school_membership(user_id,school_id,role,status,created_at,updated_at)
values ('user_teacher_pilot','school_pilot','TEACHER','ACTIVE',now(),now())
on conflict (user_id,school_id) do update set role='TEACHER',status='ACTIVE',updated_at=now();

insert into institution_teacher_classroom(user_id,classroom_id,status,created_at,updated_at)
values ('user_teacher_pilot','class_pilot','ACTIVE',now(),now())
on conflict (user_id,classroom_id) do update set status='ACTIVE',updated_at=now();
commit;
SQL

echo "pilot_membership=PASS env=$app_env school=school_pilot classroom=class_pilot"
