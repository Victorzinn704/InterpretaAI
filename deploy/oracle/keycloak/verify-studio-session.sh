#!/usr/bin/env bash
set -euo pipefail

public_base="${1:-https://interpretaai.deskimperial.online}"
runtime_env="${RUNTIME_ENV:-/etc/interpretaai/keycloak-runtime.env}"
compose_file="${COMPOSE_FILE:-/opt/interpretaai-keycloak/compose.yml}"

[[ "${EUID}" -eq 0 ]] || { echo "Execute como root." >&2; exit 2; }
[[ -f "$runtime_env" && -f "$compose_file" ]] || { echo "Ambiente OIDC ausente." >&2; exit 2; }

set -a
# shellcheck disable=SC1090
. "$runtime_env"
export PUBLIC_BASE_URL="$public_base"
set +a

set_pilot_password() {
  local temporary_flag=()
  [[ "$1" == temporary ]] && temporary_flag=(-t)
  docker compose -f "$compose_file" exec -T \
    -e PILOT_PASSWORD="$PILOT_TEACHER_TEMPORARY_PASSWORD" \
    -e PILOT_ID="$PILOT_TEACHER_SUBJECT" \
    keycloak bash -ceu '
      cfg=/tmp/interpretaai-studio-kcadm.config
      /opt/keycloak/bin/kcadm.sh config credentials --config "$cfg" \
        --server http://127.0.0.1:8080/auth --realm master \
        --user "$KC_BOOTSTRAP_ADMIN_USERNAME" --password "$KC_BOOTSTRAP_ADMIN_PASSWORD" >/dev/null
      /opt/keycloak/bin/kcadm.sh set-password --config "$cfg" \
        -r interpretaai --userid "$PILOT_ID" --new-password "$PILOT_PASSWORD" "$@" >/dev/null
    ' bash "${temporary_flag[@]}"
}

set_pilot_password permanent
trap 'set_pilot_password temporary >/dev/null 2>&1 || true' EXIT

python3 <<'PY'
import html
import http.cookiejar
import json
import os
import re
import urllib.parse
import urllib.request

public_base = os.environ["PUBLIC_BASE_URL"]
username = os.environ["PILOT_TEACHER_USERNAME"]
password = os.environ["PILOT_TEACHER_TEMPORARY_PASSWORD"]
headers = {
    "User-Agent": "Mozilla/5.0 (X11; Linux aarch64) AppleWebKit/537.36 Chrome/140 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8",
}
cookies = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookies))

with opener.open(urllib.request.Request(public_base + "/studio/", headers=headers), timeout=20) as response:
    login_html = response.read().decode("utf-8")
match = re.search(r'<form[^>]+id="kc-form-login"[^>]+action="([^"]+)"', login_html, re.S)
if not match:
    raise SystemExit("Estudio nao chegou ao formulario OIDC")
action = html.unescape(match.group(1))

request = urllib.request.Request(action, data=urllib.parse.urlencode({
    "username": username,
    "password": password,
    "credentialId": "",
}).encode(), headers=headers, method="POST")
with opener.open(request, timeout=25) as response:
    final_url = response.geturl()
    studio_html = response.read().decode("utf-8")
final_target = urllib.parse.urlsplit(final_url)
if final_target.path not in ("/studio/", "/studio/index.html"):
    raise SystemExit(
        "Login nao terminou no Estudio "
        f"target={final_target.scheme}://{final_target.netloc}{final_target.path} ")
if "Revisão docente" not in studio_html or "Revisão da professora" not in studio_html:
    raise SystemExit("Pagina autenticada do Estudio inesperada")

with opener.open(urllib.request.Request(
        public_base + "/studio/api/me",
        headers={**headers, "Accept": "application/json"}), timeout=15) as response:
    if response.status != 200:
        raise SystemExit(f"BFF do Estudio retornou HTTP {response.status}")
    identity = json.load(response)
serialized = json.dumps(identity, ensure_ascii=False)
if "school_pilot" not in serialized:
    raise SystemExit("BFF nao retornou a escola piloto")

print("studio_session=PASS login=PASS callback=PASS bff=PASS membership=PASS")
PY
