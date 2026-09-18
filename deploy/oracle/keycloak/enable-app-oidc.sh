#!/usr/bin/env bash
set -euo pipefail

app_env="${1:-}"
service="${2:-}"
health_url="${3:-}"
runtime_env="${RUNTIME_ENV:-/etc/interpretaai/keycloak-runtime.env}"

[[ "${EUID}" -eq 0 ]] || { echo "Execute como root." >&2; exit 2; }
[[ -n "$app_env" && -n "$service" && -n "$health_url" ]] || {
  echo "Uso: $0 ARQUIVO_ENV SERVICO_SYSTEMD URL_HEALTH" >&2
  exit 2
}
[[ -f "$app_env" && -f "$runtime_env" ]] || { echo "Ambiente ausente." >&2; exit 2; }

set -a
# shellcheck disable=SC1090
. "$runtime_env"
set +a

stamp="$(date -u +%Y%m%dT%H%M%SZ)"
backup="${app_env}.${stamp}.pre-oidc"
cp --preserve=mode,ownership,timestamps "$app_env" "$backup"

set_key() {
  local key="$1" value="$2" temporary
  temporary="$(mktemp)"
  awk -F= -v wanted="$key" '$1 != wanted { print }' "$app_env" >"$temporary"
  printf '%s=%s\n' "$key" "$value" >>"$temporary"
  chown --reference="$app_env" "$temporary"
  chmod --reference="$app_env" "$temporary"
  mv -f "$temporary" "$app_env"
}

set_key OIDC_ENABLED true
set_key OIDC_ISSUER_URI "$OIDC_ISSUER_URI"
set_key OIDC_AUDIENCE "$OIDC_AUDIENCE"
set_key STUDIO_ENABLED true
set_key STUDIO_COOKIE_SECURE true
set_key SERVER_FORWARD_HEADERS_STRATEGY framework
set_key SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_STUDIO_CLIENT_ID "$STUDIO_CLIENT_ID"
set_key SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_STUDIO_CLIENT_SECRET "$STUDIO_CLIENT_SECRET"
set_key SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_STUDIO_SCOPE openid,profile
set_key SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_STUDIO_ISSUER_URI "$OIDC_ISSUER_URI"

systemctl restart "$service"
for _ in $(seq 1 75); do
  if systemctl is-active --quiet "$service" && \
      curl --silent --show-error --fail --max-time 2 "$health_url" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done
if ! systemctl is-active --quiet "$service" || \
    ! curl --silent --show-error --fail --max-time 3 "$health_url" >/dev/null; then
  cp --preserve=mode,ownership,timestamps "$backup" "$app_env"
  systemctl restart "$service" || true
  echo "OIDC falhou; ambiente anterior restaurado." >&2
  exit 1
fi

echo "app_oidc=ENABLED service=$service rollback=$backup"
