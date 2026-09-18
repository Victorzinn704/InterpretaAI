#!/usr/bin/env bash
set -euo pipefail

# Run with sudo on the application VM after copying the v2 JAR and systemd unit.
# This installs a private, fail-closed staging origin. It deliberately does not
# enable OIDC, Studio, pairing, media or authoring workers.

jar_source="${1:-}"
unit_source="${2:-}"
revision="${3:-}"
live_env="${LIVE_ENV:-/etc/interpretaai/server.env}"

if [[ ! -f "$jar_source" || ! -f "$unit_source" ]]; then
  echo "Uso: $0 JAR UNIT COMMIT_COMPLETO" >&2
  exit 2
fi
if ! [[ "$revision" =~ ^[0-9a-f]{40}$ ]]; then
  echo "Informe o commit Git completo de 40 caracteres." >&2
  exit 2
fi
[[ -r "$live_env" ]] || { echo "Ambiente ativo não pode ser lido: $live_env" >&2; exit 2; }

env_value() {
  local key="$1"
  local line
  line="$(grep -m 1 "^${key}=" "$live_env" || true)"
  printf '%s' "${line#*=}"
}

database_username="$(env_value DATABASE_USERNAME)"
database_password="$(env_value DATABASE_PASSWORD)"
ollama_base_url="$(env_value OLLAMA_BASE_URL)"
ollama_model="$(env_value OLLAMA_MODEL)"
: "${database_username:?DATABASE_USERNAME ausente no ambiente ativo}"
: "${database_password:?DATABASE_PASSWORD ausente no ambiente ativo}"

install -d -o interpretaai -g interpretaai -m 0750 \
  /opt/interpretaai-v2-staging \
  /var/lib/interpretaai-v2-staging \
  /var/lib/interpretaai-v2-staging/private-media
install -o root -g interpretaai -m 0640 "$jar_source" /opt/interpretaai-v2-staging/server.jar
install -o root -g root -m 0644 "$unit_source" \
  /etc/systemd/system/interpretaai-server-v2-staging.service

fingerprint_secret="$(openssl rand -hex 32)"
umask 0027
cat >/etc/interpretaai/v2-staging.env <<EOF
SERVER_ADDRESS=127.0.0.1
PORT=8188
SERVER_FORWARD_HEADERS_STRATEGY=framework
INTERPRETAAI_RELEASE_REVISION=$revision
DATABASE_URL=jdbc:postgresql://10.220.10.10:5432/interpretaai_v2_staging
DATABASE_USERNAME=$database_username
DATABASE_PASSWORD=$database_password
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=5
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=1
IDEMPOTENCY_FINGERPRINT_SECRET=$fingerprint_secret
OIDC_ENABLED=false
STUDIO_ENABLED=false
DEVICE_PAIRING_ENABLED=false
MEDIA_STORAGE=local
MEDIA_LOCAL_ROOT=/var/lib/interpretaai-v2-staging/private-media
MEDIA_WORKER_ENABLED=false
AUTHORING_WORKER_ENABLED=false
AUTHORING_PLAN_WORKER_ENABLED=false
CONVERSATION_PROVIDER=ollama
CONVERSATION_ROUTE=ollama
OLLAMA_BASE_URL=${ollama_base_url:-http://10.220.10.10:11434}
OLLAMA_MODEL=${ollama_model:-qwen2.5:1.5b}
OLLAMA_WARMUP_ENABLED=false
SPEECH_PROVIDER=kokoro
KOKORO_BASE_URL=http://127.0.0.1:8091
GEMINI_WARMUP_ENABLED=false
NVIDIA_WARMUP_ENABLED=false
PILOT_SYNC_ENABLED=false
VOICE_AUTH_ENABLED=false
VOICE_RATE_LIMIT_ENABLED=true
EOF
chown root:interpretaai /etc/interpretaai/v2-staging.env
chmod 0640 /etc/interpretaai/v2-staging.env

systemctl daemon-reload
systemctl enable --now interpretaai-server-v2-staging.service

for _ in $(seq 1 60); do
  if curl --fail --silent --show-error http://127.0.0.1:8188/actuator/health >/dev/null 2>&1; then
    echo "v2_foundation_staging=STARTED revision=$revision"
    exit 0
  fi
  sleep 1
done

journalctl -u interpretaai-server-v2-staging.service --no-pager --lines 120 >&2
echo "Staging v2 não ficou saudável dentro de 60 segundos." >&2
exit 1
