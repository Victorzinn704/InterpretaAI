#!/usr/bin/env bash
set -euo pipefail

base_url="${1:-http://127.0.0.1:8188}"
expected_revision="${2:-}"
if [[ "$base_url" != http://127.0.0.1:* && "$base_url" != https://* ]]; then
  echo "Uso: $0 http://127.0.0.1:8188 COMMIT_COMPLETO" >&2
  exit 2
fi
if ! [[ "$expected_revision" =~ ^[0-9a-f]{40}$ ]]; then
  echo "Informe o commit Git completo de 40 caracteres." >&2
  exit 2
fi
base_url="${base_url%/}"

status() {
  curl --silent --show-error --max-time 8 --output /dev/null --write-out '%{http_code}' "$base_url$1"
}

health="$(status /actuator/health)"
identity="$(status /api/v2/identity/me)"
studio="$(status /studio/)"
info="$(curl --silent --show-error --fail --max-time 8 "$base_url/actuator/info")"
actual_revision="$(printf '%s' "$info" | python3 -c \
  'import json,sys; print(json.load(sys.stdin).get("interpretaai",{}).get("releaseRevision",""))')"

[[ "$health" == 200 ]] || { echo "health=$health; esperado 200" >&2; exit 1; }
[[ "$identity" == 401 ]] || { echo "identity_anonymous=$identity; esperado 401" >&2; exit 1; }
[[ "$studio" == 302 || "$studio" == 303 ]] || {
  echo "studio_anonymous=$studio; esperado redirecionamento OIDC" >&2
  exit 1
}
[[ "$actual_revision" == "$expected_revision" ]] || {
  echo "release_revision divergente; origem não corresponde ao commit esperado" >&2
  exit 1
}

echo "v2_origin: health=200, identity_anonymous=401, studio_redirect=$studio, revision=ok"
echo "Login positivo, vínculo escolar, PostgreSQL e restauração ainda exigem evidência separada."
