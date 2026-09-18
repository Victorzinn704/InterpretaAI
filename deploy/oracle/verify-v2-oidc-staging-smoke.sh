#!/usr/bin/env bash
set -euo pipefail

# Run with sudo on the application VM. This briefly enables OIDC and Studio on
# the private staging service, validates a signed teacher token against the real
# Oracle PostgreSQL, removes fixtures and restores the fail-closed environment.

revision="${1:-}"
mock_server="${2:-}"
env_file="/etc/interpretaai/v2-staging.env"
service="interpretaai-server-v2-staging.service"
base_url="http://127.0.0.1:8188"
oidc_port=8199
work_dir="$(mktemp -d /run/interpretaai-oidc-smoke.XXXXXX)"
env_backup="$work_dir/v2-staging.env"
oidc_pid=""
fixture_inserted=false

if ! [[ "$revision" =~ ^[0-9a-f]{40}$ ]] || [[ ! -f "$mock_server" ]]; then
  echo "Uso: $0 COMMIT_COMPLETO CAMINHO_MOCK_OIDC" >&2
  exit 2
fi
[[ -r "$env_file" ]] || { echo "Ambiente de staging ausente." >&2; exit 2; }
cp --preserve=mode,ownership,timestamps "$env_file" "$env_backup"

env_value() {
  local key="$1"
  local line
  line="$(grep -m 1 "^${key}=" "$env_file" || true)"
  printf '%s' "${line#*=}"
}

database_username="$(env_value DATABASE_USERNAME)"
database_password="$(env_value DATABASE_PASSWORD)"
: "${database_username:?DATABASE_USERNAME ausente}"
: "${database_password:?DATABASE_PASSWORD ausente}"

psql_staging() {
  PGPASSWORD="$database_password" psql \
    -h 10.220.10.10 -p 5432 -U "$database_username" -d interpretaai_v2_staging \
    -v ON_ERROR_STOP=1 "$@"
}

cleanup() {
  set +e
  if [[ "$fixture_inserted" == true ]]; then
    psql_staging >/dev/null <<'SQL'
delete from institution_school_membership where user_id = 'user_oracle_smoke';
delete from institution_adult_user where user_id = 'user_oracle_smoke';
delete from institution_school where school_id in ('school_oracle_smoke', 'school_oracle_revoked');
delete from institution_tenant where tenant_id = 'tenant_oracle_smoke';
SQL
  fi
  cp --preserve=mode,ownership,timestamps "$env_backup" "$env_file"
  systemctl restart "$service"
  if [[ -n "$oidc_pid" ]]; then
    kill "$oidc_pid" >/dev/null 2>&1 || true
    wait "$oidc_pid" >/dev/null 2>&1 || true
  fi
  rm -rf --one-file-system "$work_dir"
}
trap cleanup EXIT INT TERM

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 \
  -out "$work_dir/oidc-private.pem" 2>/dev/null
chmod 0600 "$work_dir/oidc-private.pem"
python3 "$mock_server" --port "$oidc_port" --private-key "$work_dir/oidc-private.pem" \
  --audience interpretaai-api >"$work_dir/oidc.log" 2>&1 &
oidc_pid=$!

issuer="http://127.0.0.1:$oidc_port"
for _ in $(seq 1 40); do
  curl --silent --fail --max-time 1 "$issuer/.well-known/openid-configuration" >/dev/null && break
  sleep 0.1
done
curl --silent --fail --max-time 2 "$issuer/.well-known/openid-configuration" >/dev/null

psql_staging >/dev/null <<'SQL'
insert into institution_tenant (tenant_id, name, status, created_at)
values ('tenant_oracle_smoke', 'Rede Oracle Smoke', 'ACTIVE', now());
insert into institution_school (school_id, tenant_id, name, status, created_at)
values
  ('school_oracle_smoke', 'tenant_oracle_smoke', 'Escola Oracle Smoke', 'ACTIVE', now()),
  ('school_oracle_revoked', 'tenant_oracle_smoke', 'Escola Oracle Revogada', 'ACTIVE', now());
insert into institution_adult_user (user_id, oidc_subject, status, created_at)
values ('user_oracle_smoke', 'smoke|teacher', 'ACTIVE', now());
insert into institution_school_membership
  (user_id, school_id, role, status, created_at, updated_at)
values
  ('user_oracle_smoke', 'school_oracle_smoke', 'TEACHER', 'ACTIVE', now(), now()),
  ('user_oracle_smoke', 'school_oracle_revoked', 'TEACHER', 'REVOKED', now(), now());
SQL
fixture_inserted=true

set_key() {
  local key="$1"
  local value="$2"
  if grep -q "^${key}=" "$env_file"; then
    sed -i "s|^${key}=.*|${key}=${value}|" "$env_file"
  else
    printf '%s=%s\n' "$key" "$value" >>"$env_file"
  fi
}
set_key OIDC_ENABLED true
set_key OIDC_ISSUER_URI "$issuer"
set_key OIDC_AUDIENCE interpretaai-api
set_key STUDIO_ENABLED true
set_key STUDIO_COOKIE_SECURE true
set_key SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_STUDIO_CLIENT_ID studio-oracle-smoke
set_key SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_STUDIO_CLIENT_SECRET ephemeral-local-smoke
set_key SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_STUDIO_SCOPE openid,profile
set_key SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_STUDIO_ISSUER_URI "$issuer"

systemctl restart "$service"
ready=false
for _ in $(seq 1 60); do
  if curl --silent --fail --max-time 1 "$base_url/actuator/health" >/dev/null 2>&1; then
    ready=true
    break
  fi
  sleep 1
done
[[ "$ready" == true ]] || {
  journalctl -u "$service" --no-pager --lines 100 >&2
  echo "Staging com OIDC não ficou saudável dentro de 60 segundos." >&2
  exit 1
}

anonymous="$(curl --silent --output /dev/null --write-out '%{http_code}' \
  "$base_url/api/v2/identity/me")"
studio="$(curl --silent --output /dev/null --write-out '%{http_code}' \
  "$base_url/studio/")"
[[ "$anonymous" == 401 ]]
[[ "$studio" == 302 || "$studio" == 303 ]]

curl --silent --fail --max-time 2 "$issuer/smoke-token" >"$work_dir/token.json"
python3 - "$base_url" "$work_dir/token.json" <<'PY'
import json
import pathlib
import sys
import urllib.request

base_url = sys.argv[1]
token = json.loads(pathlib.Path(sys.argv[2]).read_text())["access_token"]
request = urllib.request.Request(
    base_url + "/api/v2/identity/me",
    headers={"Authorization": "Bearer " + token},
)
with urllib.request.urlopen(request, timeout=5) as response:
    assert response.status == 200, response.status
    identity = json.load(response)
assert identity == {
    "userId": "user_oracle_smoke",
    "schools": [{"schoolId": "school_oracle_smoke", "role": "TEACHER"}],
}, identity
PY

echo "oracle_oidc_smoke=PASS anonymous=401 teacher=200 revoked_hidden=yes studio_redirect=$studio revision=$revision"
