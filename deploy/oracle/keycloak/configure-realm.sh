#!/usr/bin/env bash
set -euo pipefail

compose_file="${COMPOSE_FILE:-/opt/interpretaai-keycloak/compose.yml}"
runtime_env="${RUNTIME_ENV:-/etc/interpretaai/keycloak-runtime.env}"
realm="${KEYCLOAK_REALM:-interpretaai}"
public_base="${PUBLIC_BASE_URL:-https://interpretaai.deskimperial.online}"
pilot_username="${PILOT_TEACHER_USERNAME:-professor-piloto}"
pilot_email="${PILOT_TEACHER_EMAIL:-professor-piloto@deskimperial.online}"
kcadm_config="/tmp/interpretaai-kcadm.config"

[[ "${EUID}" -eq 0 ]] || { echo "Execute como root." >&2; exit 2; }
[[ -f "$compose_file" ]] || { echo "Compose ausente: $compose_file" >&2; exit 2; }

dc() {
  docker compose -f "$compose_file" "$@"
}

kc() {
  local command="$1"
  shift
  dc exec -T keycloak /opt/keycloak/bin/kcadm.sh "$command" \
    --config "$kcadm_config" "$@"
}

dc exec -T keycloak bash -ceu '
  /opt/keycloak/bin/kcadm.sh config credentials --config /tmp/interpretaai-kcadm.config \
    --server http://127.0.0.1:8080/auth --realm master \
    --user "$KC_BOOTSTRAP_ADMIN_USERNAME" --password "$KC_BOOTSTRAP_ADMIN_PASSWORD" >/dev/null
'

if ! kc get "realms/$realm" >/dev/null 2>&1; then
  kc create realms -s "realm=$realm" -s enabled=true >/dev/null
fi

kc update "realms/$realm" \
  -s enabled=true \
  -s displayName=InterpretaAI \
  -s sslRequired=external \
  -s registrationAllowed=false \
  -s resetPasswordAllowed=true \
  -s rememberMe=true \
  -s bruteForceProtected=true \
  -s failureFactor=5 \
  -s waitIncrementSeconds=60 \
  -s maxFailureWaitSeconds=900 >/dev/null

client_id() {
  kc get clients -r "$realm" -q "clientId=$1" --fields id --format csv --noquotes 2>/dev/null | tr -d '\r' | head -n 1
}

api_id="$(client_id interpretaai-api)"
if [[ -z "$api_id" ]]; then
  kc create clients -r "$realm" \
    -s clientId=interpretaai-api -s name='InterpretaAI API' \
    -s enabled=true -s bearerOnly=true -s publicClient=false >/dev/null
fi

studio_id="$(client_id studio)"
if [[ -z "$studio_id" ]]; then
  kc create clients -r "$realm" \
    -s clientId=studio -s name='Estudio docente InterpretaAI' \
    -s enabled=true -s publicClient=false -s clientAuthenticatorType=client-secret \
    -s standardFlowEnabled=true -s directAccessGrantsEnabled=false \
    -s serviceAccountsEnabled=false \
    -s "rootUrl=$public_base" -s 'baseUrl=/studio/' \
    -s "redirectUris=[\"$public_base/login/oauth2/code/studio\"]" \
    -s "webOrigins=[\"$public_base\"]" >/dev/null
  studio_id="$(client_id studio)"
else
  kc update "clients/$studio_id" -r "$realm" \
    -s enabled=true -s publicClient=false -s clientAuthenticatorType=client-secret \
    -s standardFlowEnabled=true -s directAccessGrantsEnabled=false \
    -s serviceAccountsEnabled=false \
    -s "rootUrl=$public_base" -s 'baseUrl=/studio/' \
    -s "redirectUris=[\"$public_base/login/oauth2/code/studio\"]" \
    -s "webOrigins=[\"$public_base\"]" >/dev/null
fi

mapper_id="$(kc get "clients/$studio_id/protocol-mappers/models" -r "$realm" \
  -q name=interpretaai-api-audience --fields id --format csv --noquotes 2>/dev/null |
  tr -d '\r' | head -n 1)"
if [[ -z "$mapper_id" ]]; then
  kc create "clients/$studio_id/protocol-mappers/models" -r "$realm" \
    -s name=interpretaai-api-audience \
    -s protocol=openid-connect \
    -s protocolMapper=oidc-audience-mapper \
    -s 'config."included.client.audience"=interpretaai-api' \
    -s 'config."access.token.claim"=true' \
    -s 'config."id.token.claim"=true' >/dev/null
fi

install -d -o root -g interpretaai -m 0750 "$(dirname "$runtime_env")"
touch "$runtime_env"
chown root:interpretaai "$runtime_env"
chmod 0640 "$runtime_env"

read_runtime() {
  sed -n "s/^$1=//p" "$runtime_env" | tail -n 1
}

write_runtime() {
  local key="$1" value="$2" temporary
  temporary="$(mktemp)"
  awk -F= -v wanted="$key" '$1 != wanted { print }' "$runtime_env" >"$temporary"
  printf '%s=%s\n' "$key" "$value" >>"$temporary"
  install -o root -g interpretaai -m 0640 "$temporary" "$runtime_env"
  rm -f "$temporary"
}

client_secret_json="$(kc get "clients/$studio_id/client-secret" -r "$realm")"
client_secret="$(python3 -c 'import json,sys; print(json.load(sys.stdin)["value"])' <<<"$client_secret_json")"
write_runtime STUDIO_CLIENT_ID studio
write_runtime STUDIO_CLIENT_SECRET "$client_secret"
write_runtime OIDC_ISSUER_URI "$public_base/auth/realms/$realm"
write_runtime OIDC_AUDIENCE interpretaai-api

pilot_id="$(kc get users -r "$realm" -q "username=$pilot_username" --fields id --format csv --noquotes 2>/dev/null |
  tr -d '\r' | head -n 1)"
if [[ -z "$pilot_id" ]]; then
  kc create users -r "$realm" -s "username=$pilot_username" -s enabled=true \
    -s "email=$pilot_email" -s emailVerified=true \
    -s firstName=Professora -s lastName=Piloto >/dev/null
  pilot_id="$(kc get users -r "$realm" -q "username=$pilot_username" --fields id --format csv --noquotes |
    tr -d '\r' | head -n 1)"
fi
kc update "users/$pilot_id" -r "$realm" -s enabled=true \
  -s "email=$pilot_email" -s emailVerified=true \
  -s firstName=Professora -s lastName=Piloto >/dev/null

pilot_password="$(read_runtime PILOT_TEACHER_TEMPORARY_PASSWORD)"
if [[ -z "$pilot_password" ]]; then
  pilot_password="$(openssl rand -base64 30 | tr -dc 'A-Za-z0-9@#%+=' | head -c 30)"
  kc set-password -r "$realm" --userid "$pilot_id" --new-password "$pilot_password" --temporary >/dev/null
  write_runtime PILOT_TEACHER_TEMPORARY_PASSWORD "$pilot_password"
fi
write_runtime PILOT_TEACHER_USERNAME "$pilot_username"
write_runtime PILOT_TEACHER_EMAIL "$pilot_email"
write_runtime PILOT_TEACHER_SUBJECT "$pilot_id"

echo "keycloak_realm=PASS realm=$realm client=studio pilot=$pilot_username"
