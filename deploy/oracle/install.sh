#!/usr/bin/env bash
set -euo pipefail

if (( EUID != 0 )); then
  echo "Execute como root: sudo ./install.sh api.seudominio.com" >&2
  exit 2
fi

domain="${1:-}"
if ! [[ "$domain" =~ ^[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?$ ]] || [[ "$domain" != *.* ]]; then
  echo "Informe somente o dominio publico, sem https:// ou caminho." >&2
  exit 2
fi

bundle_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
required_commands=(caddy curl install java journalctl ollama openssl python3 runuser sed \
  sha256sum systemctl systemd-analyze useradd)
for required_command in "${required_commands[@]}"; do
  command -v "$required_command" >/dev/null 2>&1 || {
    echo "Dependencia ausente: $required_command" >&2
    exit 2
  }
done

if [[ "$(uname -m)" != "aarch64" && "$(uname -m)" != "arm64" ]]; then
  echo "Este bundle foi preparado para Oracle Ampere ARM64; arquitetura encontrada: $(uname -m)." >&2
  exit 2
fi

(
  cd "$bundle_dir"
  sha256sum --check MANIFEST.sha256
)

if ! id interpretaai >/dev/null 2>&1; then
  useradd --system --home /var/lib/interpretaai --shell /usr/sbin/nologin interpretaai
fi
install -d -o interpretaai -g interpretaai \
  /opt/interpretaai/kokoro /var/lib/interpretaai/data \
  /var/lib/interpretaai/huggingface /etc/interpretaai \
  /etc/systemd/system/caddy.service.d /etc/systemd/system/ollama.service.d

previous_jar=""
if [[ -f /opt/interpretaai/server.jar ]]; then
  previous_jar="/opt/interpretaai/server.jar.previous"
  cp --preserve=mode,ownership /opt/interpretaai/server.jar "$previous_jar"
fi
install -o interpretaai -g interpretaai -m 0644 \
  "$bundle_dir/server/interpretaai-server.jar" /opt/interpretaai/server.jar
install -o interpretaai -g interpretaai -m 0644 \
  "$bundle_dir/kokoro/app.py" "$bundle_dir/kokoro/requirements.txt" /opt/interpretaai/kokoro/

if [[ ! -x /opt/interpretaai/kokoro/.venv/bin/python ]]; then
  runuser -u interpretaai -- python3 -m venv /opt/interpretaai/kokoro/.venv
fi
runuser -u interpretaai -- /opt/interpretaai/kokoro/.venv/bin/pip install \
  --disable-pip-version-check --requirement /opt/interpretaai/kokoro/requirements.txt

install -m 0644 "$bundle_dir/systemd/interpretaai-server.service" \
  "$bundle_dir/systemd/interpretaai-kokoro.service" /etc/systemd/system/
install -m 0644 "$bundle_dir/systemd/caddy.service.d/interpretaai.conf" \
  /etc/systemd/system/caddy.service.d/interpretaai.conf
install -m 0644 "$bundle_dir/systemd/ollama.service.d/interpretaai.conf" \
  /etc/systemd/system/ollama.service.d/interpretaai.conf
install -m 0644 "$bundle_dir/Caddyfile" /etc/caddy/Caddyfile

if [[ ! -f /etc/caddy/.env ]]; then
  temporary_caddy_env="$(mktemp)"
  printf 'INTERPRETAAI_DOMAIN=%s\n' "$domain" >"$temporary_caddy_env"
  install -o root -g caddy -m 0640 "$temporary_caddy_env" /etc/caddy/.env
  rm -f "$temporary_caddy_env"
fi

new_secret() {
  openssl rand -hex 24
}

if [[ ! -f /etc/interpretaai/server.env ]]; then
  temporary_server_env="$(mktemp)"
  sed \
    -e "s/^IDEMPOTENCY_FINGERPRINT_SECRET=.*/IDEMPOTENCY_FINGERPRINT_SECRET=$(new_secret)/" \
    -e 's/^PILOT_SYNC_ENABLED=false/PILOT_SYNC_ENABLED=true/' \
    -e "s/^PILOT_SYNC_TEACHER_TOKEN=.*/PILOT_SYNC_TEACHER_TOKEN=$(new_secret)/" \
    -e "s/^PILOT_SYNC_DEVICE_TOKEN=.*/PILOT_SYNC_DEVICE_TOKEN=$(new_secret)/" \
    -e "s/^PILOT_SYNC_SECRETARY_TOKEN=.*/PILOT_SYNC_SECRETARY_TOKEN=$(new_secret)/" \
    "$bundle_dir/server.env.example" >"$temporary_server_env"
  install -o root -g interpretaai -m 0640 "$temporary_server_env" /etc/interpretaai/server.env
  rm -f "$temporary_server_env"
else
  echo "Ambiente existente preservado: /etc/interpretaai/server.env"
fi

caddy validate --config /etc/caddy/Caddyfile
systemd-analyze verify /etc/systemd/system/interpretaai-server.service \
  /etc/systemd/system/interpretaai-kokoro.service
systemctl daemon-reload
systemctl enable --now ollama interpretaai-kokoro interpretaai-server caddy

server_ready=false
for _ in $(seq 1 60); do
  if curl --silent --fail http://127.0.0.1:8088/actuator/health >/dev/null; then
    server_ready=true
    break
  fi
  sleep 1
done

if [[ "$server_ready" != true ]]; then
  echo "O gateway nao ficou saudavel em 60 s." >&2
  journalctl --unit interpretaai-server --lines 40 --no-pager >&2 || true
  if [[ -n "$previous_jar" && -f "$previous_jar" ]]; then
    mv "$previous_jar" /opt/interpretaai/server.jar
    chown interpretaai:interpretaai /opt/interpretaai/server.jar
    systemctl restart interpretaai-server
    echo "Rollback automatico aplicado ao JAR anterior." >&2
  fi
  exit 1
fi

echo "Instalacao local aprovada. Aguarde o TLS do Caddy e execute:"
echo "  INTERPRETAAI_DEVICE_TOKEN=... ./verify-public.sh https://$domain"
echo "O token do tablet esta protegido em /etc/interpretaai/server.env e nao foi impresso."
