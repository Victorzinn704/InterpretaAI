#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_dir"

for required_command in curl createdb git initdb java openssl pg_ctl psql python3; do
  command -v "$required_command" >/dev/null 2>&1 || {
    echo "SKIP: $required_command não está disponível para o smoke de staging." >&2
    exit 77
  }
done

free_port() {
  python3 -c 'import socket; s=socket.socket(); s.bind(("127.0.0.1",0)); print(s.getsockname()[1]); s.close()'
}

temporary_dir="$(mktemp -d -t interpretaai-v2-staging-smoke)"
postgres_port="$(free_port)"
oidc_port="$(free_port)"
application_port="$(free_port)"
postgres_started=false
oidc_pid=""
application_pid=""

cleanup() {
  if [[ -n "$application_pid" ]]; then
    kill "$application_pid" 2>/dev/null || true
    wait "$application_pid" 2>/dev/null || true
  fi
  if [[ -n "$oidc_pid" ]]; then
    kill "$oidc_pid" 2>/dev/null || true
    wait "$oidc_pid" 2>/dev/null || true
  fi
  if [[ "$postgres_started" == true ]]; then
    pg_ctl -D "$temporary_dir/postgres" -m fast -w stop >/dev/null 2>&1 || true
  fi
  rm -r -- "$temporary_dir"
}
trap cleanup EXIT INT TERM

./gradlew :server:bootJar --console=plain >/dev/null
initdb -D "$temporary_dir/postgres" --auth-local=trust --auth-host=trust --no-instructions >/dev/null
pg_ctl -D "$temporary_dir/postgres" -o "-h 127.0.0.1 -p $postgres_port" \
  -l "$temporary_dir/postgres.log" -w start >/dev/null
postgres_started=true
createdb -h 127.0.0.1 -p "$postgres_port" interpretaai_v2_staging

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 \
  -out "$temporary_dir/oidc-private.pem" 2>/dev/null
chmod 600 "$temporary_dir/oidc-private.pem"
python3 tools/mock-oidc-server.py --port "$oidc_port" \
  --private-key "$temporary_dir/oidc-private.pem" --audience interpretaai-api \
  >"$temporary_dir/oidc.log" 2>&1 &
oidc_pid=$!
issuer="http://127.0.0.1:$oidc_port"
for _ in {1..40}; do
  curl --silent --fail --max-time 1 "$issuer/.well-known/openid-configuration" >/dev/null && break
  sleep 0.1
done
curl --silent --fail --max-time 2 "$issuer/.well-known/openid-configuration" >/dev/null

revision="$(git rev-parse HEAD)"
env \
  SERVER_ADDRESS=127.0.0.1 \
  PORT="$application_port" \
  DATABASE_URL="jdbc:postgresql://127.0.0.1:$postgres_port/interpretaai_v2_staging" \
  DATABASE_USERNAME="$(id -un)" \
  DATABASE_PASSWORD= \
  INTERPRETAAI_RELEASE_REVISION="$revision" \
  OIDC_ENABLED=true \
  OIDC_ISSUER_URI="$issuer" \
  OIDC_AUDIENCE=interpretaai-api \
  STUDIO_ENABLED=true \
  STUDIO_COOKIE_SECURE=true \
  SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_STUDIO_CLIENT_ID=studio-smoke \
  SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_STUDIO_CLIENT_SECRET=local-smoke-only \
  SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_STUDIO_SCOPE=openid,profile \
  SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_STUDIO_ISSUER_URI="$issuer" \
  DEVICE_PAIRING_ENABLED=false \
  MEDIA_WORKER_ENABLED=false \
  AUTHORING_WORKER_ENABLED=false \
  AUTHORING_PLAN_WORKER_ENABLED=false \
  OLLAMA_WARMUP_ENABLED=false \
  GEMINI_WARMUP_ENABLED=false \
  NVIDIA_WARMUP_ENABLED=false \
  java -jar server/build/libs/server-0.1.0.jar >"$temporary_dir/application.log" 2>&1 &
application_pid=$!

base_url="http://127.0.0.1:$application_port"
for _ in {1..80}; do
  if ! kill -0 "$application_pid" 2>/dev/null; then
    tail -60 "$temporary_dir/application.log" >&2
    exit 1
  fi
  curl --silent --fail --max-time 1 "$base_url/actuator/health" >/dev/null && break
  sleep 0.25
done

deploy/oracle/verify-v2-origin.sh "$base_url" "$revision"
migration_result="$(psql -h 127.0.0.1 -p "$postgres_port" -d interpretaai_v2_staging -Atqc \
  "select count(*) || ':' || max(installed_rank) from flyway_schema_history where success")"
[[ "$migration_result" == "20:20" ]] || {
  echo "Migrações inesperadas: $migration_result; esperado 20:20" >&2
  exit 1
}

psql -h 127.0.0.1 -p "$postgres_port" -d interpretaai_v2_staging -v ON_ERROR_STOP=1 >/dev/null <<'SQL'
insert into institution_tenant (tenant_id, name, status, created_at)
values ('tenant_smoke', 'Rede Smoke', 'ACTIVE', now());
insert into institution_school (school_id, tenant_id, name, status, created_at)
values
  ('school_smoke', 'tenant_smoke', 'Escola Smoke', 'ACTIVE', now()),
  ('school_revoked', 'tenant_smoke', 'Escola Revogada', 'ACTIVE', now());
insert into institution_adult_user (user_id, oidc_subject, status, created_at)
values ('user_smoke_teacher', 'smoke|teacher', 'ACTIVE', now());
insert into institution_school_membership
  (user_id, school_id, role, status, created_at, updated_at)
values
  ('user_smoke_teacher', 'school_smoke', 'TEACHER', 'ACTIVE', now(), now()),
  ('user_smoke_teacher', 'school_revoked', 'TEACHER', 'REVOKED', now(), now());
SQL

curl --silent --fail --max-time 2 "$issuer/smoke-token" >"$temporary_dir/token.json"
python3 - "$base_url" "$temporary_dir/token.json" <<'PY'
import json
import pathlib
import sys
import urllib.error
import urllib.request

base_url = sys.argv[1]
token = json.loads(pathlib.Path(sys.argv[2]).read_text())["access_token"]
request = urllib.request.Request(
    base_url + "/api/v2/identity/me",
    headers={"Authorization": "Bearer " + token},
)
try:
    with urllib.request.urlopen(request, timeout=3) as response:
        assert response.status == 200, response.status
        identity = json.load(response)
except urllib.error.HTTPError as error:
    raise SystemExit(f"OIDC positivo falhou com HTTP {error.code}") from error

assert identity == {
    "userId": "user_smoke_teacher",
    "schools": [{"schoolId": "school_smoke", "role": "TEACHER"}],
}, identity
PY

echo "v2_staging_smoke: PostgreSQL=17, Flyway=V20, OIDC=negativo+positivo, escopo=ok, revision=ok"
