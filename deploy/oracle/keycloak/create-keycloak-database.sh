#!/usr/bin/env bash
set -euo pipefail

secret_file="${1:-/run/interpretaai-keycloak-db-password}"
postgres_container="${POSTGRES_CONTAINER:-desk-db-postgres}"

[[ "${EUID}" -eq 0 ]] || { echo "Execute como root na VM do PostgreSQL." >&2; exit 2; }
[[ -f "$secret_file" ]] || { echo "Arquivo de segredo ausente: $secret_file" >&2; exit 2; }
[[ "$(stat -c '%a' "$secret_file")" == 600 ]] || {
  echo "O arquivo de segredo precisa estar com modo 0600." >&2
  exit 2
}

db_password="$(tr -d '\n' <"$secret_file")"
[[ "$db_password" =~ ^[A-Fa-f0-9]{64}$ ]] || {
  echo "Use um segredo hexadecimal de 64 caracteres." >&2
  exit 2
}

docker exec -i \
  -e KEYCLOAK_DB_PASSWORD="$db_password" \
  "$postgres_container" bash -s <<'INNER'
set -euo pipefail
export PGPASSWORD="$POSTGRES_PASSWORD"
psql -h 127.0.0.1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -v ON_ERROR_STOP=1 -v db_password="$KEYCLOAK_DB_PASSWORD" <<'SQL'
select format('create role interpretaai_keycloak login password %L', :'db_password')
where not exists (select 1 from pg_roles where rolname='interpretaai_keycloak') \gexec
alter role interpretaai_keycloak password :'db_password';
select 'create database interpretaai_keycloak owner interpretaai_keycloak'
where not exists (select 1 from pg_database where datname='interpretaai_keycloak') \gexec
revoke all on database interpretaai_keycloak from public;
grant connect,temporary on database interpretaai_keycloak to interpretaai_keycloak;
SQL
INNER

rm -f "$secret_file"
echo "keycloak_database=PASS database=interpretaai_keycloak owner=interpretaai_keycloak"
