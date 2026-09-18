#!/usr/bin/env bash
set -euo pipefail

# Run with sudo on the application VM after foundation staging has passed.
# V11-V20 are additive; rolling the JAR back does not remove migrated tables.

jar_source="${1:-}"
revision="${2:-}"
service="interpretaai-server.service"
env_file="/etc/interpretaai/server.env"
active_jar="/opt/interpretaai/server.jar"

[[ -f "$jar_source" ]] || { echo "Uso: $0 JAR COMMIT_COMPLETO" >&2; exit 2; }
if ! [[ "$revision" =~ ^[0-9a-f]{40}$ ]]; then
  echo "Informe o commit Git completo de 40 caracteres." >&2
  exit 2
fi

staging_revision="$(curl --silent --show-error --fail --max-time 5 \
  http://127.0.0.1:8188/actuator/info | python3 -c \
  'import json,sys; print(json.load(sys.stdin).get("interpretaai",{}).get("releaseRevision",""))')"
[[ "$staging_revision" == "$revision" ]] || {
  echo "O staging privado não corresponde ao commit solicitado." >&2
  exit 1
}

stamp="$(date -u +%Y%m%dT%H%M%SZ)"
release_dir="/opt/interpretaai/releases/${stamp}-pre-v2"
install -d -o root -g interpretaai -m 0750 "$release_dir"
cp --preserve=mode,ownership,timestamps "$active_jar" "$release_dir/server.jar"
cp --preserve=mode,ownership,timestamps "$env_file" "$release_dir/server.env"
sha256sum "$active_jar" "$jar_source" >"$release_dir/SHA256.txt"
chmod 0640 "$release_dir/SHA256.txt"

rollback() {
  echo "Falha na promoção; restaurando JAR e ambiente anteriores." >&2
  cp --preserve=mode,ownership,timestamps "$release_dir/server.jar" "$active_jar"
  cp --preserve=mode,ownership,timestamps "$release_dir/server.env" "$env_file"
  systemctl restart "$service" || true
}
trap rollback ERR

set_key() {
  local key="$1"
  local value="$2"
  if grep -q "^${key}=" "$env_file"; then
    sed -i "s|^${key}=.*|${key}=${value}|" "$env_file"
  else
    printf '%s=%s\n' "$key" "$value" >>"$env_file"
  fi
}

install -d -o interpretaai -g interpretaai -m 0750 /var/lib/interpretaai/private-media
set_key INTERPRETAAI_RELEASE_REVISION "$revision"
set_key OIDC_ENABLED false
set_key STUDIO_ENABLED false
set_key DEVICE_PAIRING_ENABLED false
set_key MEDIA_STORAGE local
set_key MEDIA_LOCAL_ROOT /var/lib/interpretaai/private-media
set_key MEDIA_WORKER_ENABLED false
set_key AUTHORING_WORKER_ENABLED false
set_key AUTHORING_PLAN_WORKER_ENABLED false
chown root:interpretaai "$env_file"
chmod 0640 "$env_file"

install -o root -g interpretaai -m 0640 "$jar_source" "${active_jar}.new"
mv -f "${active_jar}.new" "$active_jar"
systemctl restart "$service"

healthy=false
for _ in $(seq 1 60); do
  if curl --fail --silent --show-error http://172.18.0.1:8088/actuator/health >/dev/null 2>&1; then
    healthy=true
    break
  fi
  sleep 1
done
[[ "$healthy" == true ]] || {
  journalctl -u "$service" --no-pager --lines 120 >&2
  false
}

actual_revision="$(curl --silent --show-error --fail http://172.18.0.1:8088/actuator/info | \
  python3 -c 'import json,sys; print(json.load(sys.stdin).get("interpretaai",{}).get("releaseRevision",""))')"
[[ "$actual_revision" == "$revision" ]]
[[ "$(curl --silent --output /dev/null --write-out '%{http_code}' \
  http://172.18.0.1:8088/api/v2/identity/me)" == 403 ]]
curl --silent --show-error --fail http://172.18.0.1:8088/api/v1/gateway/status >/dev/null

trap - ERR
echo "v2_production=PASS revision=$revision rollback=$release_dir"
