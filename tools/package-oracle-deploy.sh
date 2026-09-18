#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
output_file="${1:-$repo_dir/build/interpretaai-oracle-arm64.tar.gz}"
stage_dir="$(mktemp -d)"
bundle_dir="$stage_dir/interpretaai-oracle"

cleanup() {
  rm -rf "$stage_dir"
}
trap cleanup EXIT

if command -v sha256sum >/dev/null 2>&1; then
  hash_command=(sha256sum)
elif command -v shasum >/dev/null 2>&1; then
  hash_command=(shasum -a 256)
else
  echo "ERRO: instale sha256sum ou shasum." >&2
  exit 2
fi

cd "$repo_dir"
if [[ "${SKIP_SERVER_BUILD:-false}" != true ]]; then
  ./gradlew :server:bootJar >/dev/null
elif [[ ! -f server/build/libs/server-0.1.0.jar ]]; then
  echo "ERRO: SKIP_SERVER_BUILD=true, mas o JAR nao existe." >&2
  exit 2
fi

mkdir -p "$bundle_dir/server" \
  "$bundle_dir/kokoro" \
  "$bundle_dir/keycloak" \
  "$bundle_dir/systemd/caddy.service.d" \
  "$bundle_dir/systemd/ollama.service.d"

cp server/build/libs/server-0.1.0.jar "$bundle_dir/server/interpretaai-server.jar"
cp services/kokoro/app.py services/kokoro/requirements.txt "$bundle_dir/kokoro/"
cp deploy/oracle/README.md deploy/oracle/Caddyfile deploy/oracle/Caddyfile.v2.example \
  deploy/oracle/caddy.env.example \
  deploy/oracle/server.env.example deploy/oracle/v2-staging.env.example \
  deploy/oracle/interpretaai-server-v2-staging.service.example \
  deploy/oracle/install-v2-foundation-staging.sh \
  deploy/oracle/promote-v2-production.sh \
  deploy/oracle/verify-db-restore-drill.sh \
  deploy/oracle/verify-v2-oidc-staging-smoke.sh \
  deploy/oracle/install.sh deploy/oracle/verify-public.sh \
  deploy/oracle/verify-classroom-load.sh deploy/oracle/verify-v2-public.sh \
  deploy/oracle/verify-v2-origin.sh \
  "$bundle_dir/"
cp deploy/oracle/interpretaai-server.service deploy/oracle/interpretaai-kokoro.service \
  "$bundle_dir/systemd/"
cp tools/mock-oidc-server.py "$bundle_dir/"
cp deploy/oracle/keycloak/Containerfile deploy/oracle/keycloak/compose.yml \
  deploy/oracle/keycloak/keycloak.env.example deploy/oracle/keycloak/nginx-location.conf \
  deploy/oracle/keycloak/*.sh "$bundle_dir/keycloak/"
cp deploy/oracle/caddy.service.d/interpretaai.conf "$bundle_dir/systemd/caddy.service.d/"
cp deploy/oracle/ollama.service.d/interpretaai.conf "$bundle_dir/systemd/ollama.service.d/"
chmod 0755 "$bundle_dir/install.sh" "$bundle_dir/verify-public.sh" \
  "$bundle_dir/verify-classroom-load.sh" "$bundle_dir/verify-v2-public.sh" \
  "$bundle_dir/verify-v2-origin.sh" "$bundle_dir/install-v2-foundation-staging.sh" \
  "$bundle_dir/promote-v2-production.sh" "$bundle_dir/verify-db-restore-drill.sh" \
  "$bundle_dir/verify-v2-oidc-staging-smoke.sh" "$bundle_dir/mock-oidc-server.py"
chmod 0755 "$bundle_dir/keycloak"/*.sh

credential_pattern='(nvapi-|AIza[0-9A-Za-z_-]{20,}|AQ\.[0-9A-Za-z_-]{20,})'
if command -v rg >/dev/null 2>&1; then
  credential_hits="$(rg -n --hidden "$credential_pattern" "$bundle_dir" || true)"
else
  credential_hits="$(grep -RInE --binary-files=without-match "$credential_pattern" "$bundle_dir" || true)"
fi
if [[ -n "$credential_hits" ]]; then
  printf '%s\n' "$credential_hits"
  echo "ERRO: possível credencial encontrada no pacote Oracle." >&2
  exit 1
fi

(
  cd "$bundle_dir"
  if [[ "${hash_command[0]}" == sha256sum ]]; then
    find . -type f ! -name MANIFEST.sha256 -print0 | sort -z | xargs -0 sha256sum > MANIFEST.sha256
  else
    find . -type f ! -name MANIFEST.sha256 -print0 | sort -z | xargs -0 shasum -a 256 > MANIFEST.sha256
  fi
)

mkdir -p "$(dirname "$output_file")"
tar -C "$stage_dir" -czf "$output_file" interpretaai-oracle

echo "Pacote Oracle: $output_file"
echo "SHA-256: $("${hash_command[@]}" "$output_file" | awk '{print $1}')"
