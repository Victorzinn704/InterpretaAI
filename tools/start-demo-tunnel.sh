#!/usr/bin/env bash
set -euo pipefail

command -v cloudflared >/dev/null || { echo "Falta instalar cloudflared" >&2; exit 1; }
curl -fsS http://127.0.0.1:8088/actuator/health >/dev/null || {
  echo "Inicie primeiro com ./tools/start-local-mvp.sh" >&2
  exit 1
}

echo "Copie a URL HTTPS exibida abaixo. Este terminal deve permanecer aberto."
exec cloudflared tunnel --no-autoupdate --url http://127.0.0.1:8088
