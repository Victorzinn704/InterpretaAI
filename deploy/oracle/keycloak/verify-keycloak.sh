#!/usr/bin/env bash
set -euo pipefail

public_base="${1:-https://interpretaai.deskimperial.online}"
issuer="$public_base/auth/realms/interpretaai"

ready="$(curl --silent --show-error --fail --max-time 5 http://127.0.0.1:9092/auth/health/ready)"
python3 -c 'import json,sys; assert json.load(sys.stdin)["status"] == "UP"' <<<"$ready"

metadata="$(curl --silent --show-error --fail --max-time 10 \
  "$issuer/.well-known/openid-configuration")"
python3 -c '
import json, sys
expected = sys.argv[1]
document = json.load(sys.stdin)
assert document["issuer"] == expected, (document["issuer"], expected)
assert document["authorization_endpoint"].startswith(expected + "/")
assert document["token_endpoint"].startswith(expected + "/")
assert document["jwks_uri"].startswith(expected + "/")
' "$issuer" <<<"$metadata"

[[ "$(curl --silent --output /dev/null --write-out '%{http_code}' --max-time 10 \
  "$public_base/auth/admin/")" == 404 ]]
[[ "$(curl --silent --output /dev/null --write-out '%{http_code}' --max-time 10 \
  "$public_base/auth/realms/master/.well-known/openid-configuration")" == 404 ]]

echo "keycloak_public=PASS issuer=$issuer admin=hidden health=UP"
