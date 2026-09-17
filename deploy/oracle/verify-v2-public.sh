#!/usr/bin/env bash
set -euo pipefail

base_url="${1:-}"
if [[ "$base_url" != https://* && "$base_url" != http://127.0.0.1:* ]]; then
  echo "Uso: INTERPRETAAI_ADULT_TOKEN=... $0 https://api.seudominio.com" >&2
  exit 2
fi
base_url="${base_url%/}"

INTERPRETAAI_VERIFY_URL="$base_url" python3 - <<'PY'
import json
import os
import urllib.error
import urllib.request

base = os.environ["INTERPRETAAI_VERIFY_URL"]
token = os.environ.get("INTERPRETAAI_ADULT_TOKEN", "")

def request(path, headers=None):
    req = urllib.request.Request(base + path,
                                 headers={"User-Agent": "curl/8.0", **(headers or {})})
    try:
        with urllib.request.urlopen(req, timeout=8) as response:
            return response.status, dict(response.headers), response.read(8192)
    except urllib.error.HTTPError as error:
        return error.code, dict(error.headers), error.read(8192)

health_status, _, health_raw = request("/actuator/health")
if health_status != 200 or json.loads(health_raw).get("status") != "UP":
    raise SystemExit("health público inválido")

path = "/api/v2/identity/me"
anonymous_status, anonymous_headers, _ = request(path)
if anonymous_status != 401:
    raise SystemExit(f"rota v2 sem token retornou {anonymous_status}; esperado 401")
if "no-store" not in anonymous_headers.get("Cache-Control", "").lower():
    raise SystemExit("rota v2 sem Cache-Control: no-store")

if not token:
    print("v2: health=UP, anônimo=401; autenticação positiva NÃO VERIFICADA")
    raise SystemExit(0)

status, headers, body = request(path, {"Authorization": "Bearer " + token})
if status != 200:
    raise SystemExit(f"rota v2 com token retornou {status}; esperado 200")
if "no-store" not in headers.get("Cache-Control", "").lower():
    raise SystemExit("rota v2 autenticada sem Cache-Control: no-store")
context = json.loads(body)
if not context.get("userId") or not context.get("schools"):
    raise SystemExit("contexto institucional vazio")
print("v2: health=UP, anônimo=401, OIDC=200, escola ativa=sim; conteúdo omitido")
PY
