#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_dir"

for required_command in curl createdb git initdb java pg_ctl psql python3; do
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

python3 tools/mock-oidc-server.py --port "$oidc_port" >"$temporary_dir/oidc.log" 2>&1 &
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

echo "v2_staging_smoke: PostgreSQL=17, Flyway=V20, OIDC=isolado, revision=ok"
