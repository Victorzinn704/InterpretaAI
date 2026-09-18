#!/usr/bin/env bash
set -euo pipefail

origin="${1:-http://127.0.0.1:8188}"
public_base="${PUBLIC_BASE_URL:-https://interpretaai.deskimperial.online}"
runtime_env="${RUNTIME_ENV:-/etc/interpretaai/keycloak-runtime.env}"
compose_file="${COMPOSE_FILE:-/opt/interpretaai-keycloak/compose.yml}"

[[ "${EUID}" -eq 0 ]] || { echo "Execute como root." >&2; exit 2; }
[[ -f "$runtime_env" && -f "$compose_file" ]] || { echo "Ambiente OIDC ausente." >&2; exit 2; }

set -a
# shellcheck disable=SC1090
. "$runtime_env"
export VERIFY_ORIGIN="$origin" PUBLIC_BASE_URL="$public_base"
set +a

set_pilot_password() {
  local temporary_flag=()
  [[ "$1" == temporary ]] && temporary_flag=(-t)
  docker compose -f "$compose_file" exec -T \
    -e PILOT_PASSWORD="$PILOT_TEACHER_TEMPORARY_PASSWORD" \
    -e PILOT_ID="$PILOT_TEACHER_SUBJECT" \
    keycloak bash -ceu '
      cfg=/tmp/interpretaai-real-oidc-kcadm.config
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
import base64
import html
import http.cookiejar
import json
import os
import re
import secrets
import urllib.error
import urllib.parse
import urllib.request

public_base = os.environ["PUBLIC_BASE_URL"]
issuer = os.environ["OIDC_ISSUER_URI"]
origin = os.environ["VERIFY_ORIGIN"]
username = os.environ["PILOT_TEACHER_USERNAME"]
password = os.environ["PILOT_TEACHER_TEMPORARY_PASSWORD"]
client_id = os.environ["STUDIO_CLIENT_ID"]
client_secret = os.environ["STUDIO_CLIENT_SECRET"]
redirect_uri = public_base + "/login/oauth2/code/studio"

cookies = http.cookiejar.CookieJar()
default_opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookies))
browser_headers = {
    "User-Agent": "Mozilla/5.0 (X11; Linux aarch64) AppleWebKit/537.36 Chrome/140 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8",
}
query = urllib.parse.urlencode({
    "client_id": client_id,
    "redirect_uri": redirect_uri,
    "response_type": "code",
    "scope": "openid profile",
    "state": secrets.token_urlsafe(24),
    "nonce": secrets.token_urlsafe(24),
})
login_page = default_opener.open(urllib.request.Request(
    issuer + "/protocol/openid-connect/auth?" + query,
    headers=browser_headers), timeout=15)
body = login_page.read().decode("utf-8")
match = re.search(r'<form[^>]+id="kc-form-login"[^>]+action="([^"]+)"', body, re.S)
if not match:
    raise SystemExit("Formulario de login OIDC nao encontrado")
action = html.unescape(match.group(1))

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, msg, headers, newurl):
        return None

login_opener = urllib.request.build_opener(
    urllib.request.HTTPCookieProcessor(cookies), NoRedirect())
request = urllib.request.Request(action, data=urllib.parse.urlencode({
    "username": username,
    "password": password,
    "credentialId": "",
}).encode(), headers=browser_headers, method="POST")
try:
    response = login_opener.open(request, timeout=15)
    login_status = response.status
    location = response.headers.get("Location", "")
    login_body = response.read().decode("utf-8", errors="replace")
except urllib.error.HTTPError as error:
    if error.code not in (302, 303):
        raise
    login_status = error.code
    location = error.headers.get("Location", "")
    login_body = error.read().decode("utf-8", errors="replace")

if not location.startswith(redirect_uri):
    target = urllib.parse.urlsplit(location)
    query_keys = sorted(urllib.parse.parse_qs(target.query).keys())
    invalid = "invalid username or password" in login_body.lower()
    raise SystemExit(
        "Login OIDC nao retornou ao callback esperado "
        f"status={login_status} target={target.scheme}://{target.netloc}{target.path} "
        f"query_keys={query_keys} invalid_credentials={invalid}")
callback = urllib.parse.urlsplit(location)
params = urllib.parse.parse_qs(callback.query)
code = params.get("code", [""])[0]
if not code:
    raise SystemExit("Callback OIDC sem codigo de autorizacao")

token_request = urllib.request.Request(
    issuer + "/protocol/openid-connect/token",
    data=urllib.parse.urlencode({
        "grant_type": "authorization_code",
        "code": code,
        "redirect_uri": redirect_uri,
        "client_id": client_id,
        "client_secret": client_secret,
    }).encode(),
    headers={
        "Content-Type": "application/x-www-form-urlencoded",
        "User-Agent": browser_headers["User-Agent"],
    },
    method="POST")
with urllib.request.urlopen(token_request, timeout=15) as response:
    tokens = json.load(response)
access_token = tokens["access_token"]
payload_segment = access_token.split(".")[1]
payload_segment += "=" * (-len(payload_segment) % 4)
claims = json.loads(base64.urlsafe_b64decode(payload_segment))
if claims.get("iss") != issuer:
    raise SystemExit("Issuer inesperado no token")
audience = claims.get("aud", [])
if isinstance(audience, str):
    audience = [audience]
if "interpretaai-api" not in audience:
    raise SystemExit("Audience interpretaai-api ausente")

identity_request = urllib.request.Request(
    origin + "/api/v2/identity/me",
    headers={"Authorization": "Bearer " + access_token})
with urllib.request.urlopen(identity_request, timeout=15) as response:
    if response.status != 200:
        raise SystemExit(f"API adulta retornou HTTP {response.status}")
    identity = response.read().decode("utf-8")
if "school_pilot" not in identity:
    raise SystemExit("Vinculo da escola piloto ausente")

classroom_id = os.environ.get("VERIFY_CLASSROOM_ID", "class_pilot")
school_id = "school_pilot"

def api(path, method="GET", payload=None, token=access_token, extra_headers=None):
    request_headers = {"Accept": "application/json"}
    if token:
        request_headers["Authorization"] = "Bearer " + token
    if payload is not None:
        request_headers["Content-Type"] = "application/json"
    if extra_headers:
        request_headers.update(extra_headers)
    request = urllib.request.Request(
        origin + path,
        data=None if payload is None else json.dumps(payload).encode(),
        headers=request_headers,
        method=method)
    with urllib.request.urlopen(request, timeout=20) as response:
        raw = response.read()
        return {} if not raw else json.loads(raw)

names = [f"Aluno demonstracao {number:02d}" for number in range(1, 6)]
roster = api(
    f"/api/v2/classroom-management/classrooms/{classroom_id}/roster",
    "PUT", {"names": names})
if len(roster.get("learners", [])) != len(names):
    raise SystemExit("A lista demonstrativa nao foi salva")

session = api(
    f"/api/v2/classroom-management/classrooms/{classroom_id}/sessions", "POST")
session_id = session["sessionId"]
join_code = session["joinCode"]
device_id = None
try:
    pairing = api(
        "/api/v2/device-management/pairing-codes", "POST",
        {"classroomId": classroom_id},
        extra_headers={"X-School-Id": school_id})
    credential = api(
        "/api/v2/device-pairings/redeem", "POST", {
            "code": pairing["code"],
            "installationId": "oracle-demo-" + secrets.token_hex(12),
            "appVersion": 23,
            "architecture": "ARM64",
            "viewportWidthDp": 800,
            "viewportHeightDp": 1280,
        }, token=None)
    device_id = credential["deviceId"]
    device_token = credential["deviceToken"]
    lobby = api(
        f"/api/v2/devices/{device_id}/classroom-sessions/resolve", "POST",
        {"code": join_code}, token=device_token)
    available = [item for item in lobby["learners"] if item["available"]]
    if len(available) != len(names):
        raise SystemExit("A sala nao listou os cinco lugares livres")
    seat = api(
        f"/api/v2/devices/{device_id}/classroom-sessions/join", "POST",
        {"code": join_code, "learnerId": available[0]["learnerId"]},
        token=device_token)
    context = api(f"/api/v2/devices/{device_id}/context", token=device_token)
    status = api(f"/api/v2/classroom-management/sessions/{session_id}")
    if (seat.get("classroomId") != classroom_id
            or context.get("classroomId") != classroom_id
            or status.get("connectedDevices") != 1
            or status.get("learnerCount") != len(names)):
        raise SystemExit("O vinculo temporario do tablet nao foi confirmado")
    print("mobile_classroom=PASS learners=5 connected=1 dynamic_context=PASS")
finally:
    api(f"/api/v2/classroom-management/sessions/{session_id}/close", "POST")
    if device_id:
        api(
            f"/api/v2/device-management/devices/{device_id}/revoke", "POST",
            extra_headers={
                "X-School-Id": school_id,
                "Idempotency-Key": "oracle-demo-revoke-" + secrets.token_hex(8),
            })
PY
