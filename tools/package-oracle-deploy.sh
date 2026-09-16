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

cd "$repo_dir"
./gradlew :server:bootJar >/dev/null

mkdir -p "$bundle_dir/server" \
  "$bundle_dir/kokoro" \
  "$bundle_dir/systemd/caddy.service.d" \
  "$bundle_dir/systemd/ollama.service.d"

cp server/build/libs/server-0.1.0.jar "$bundle_dir/server/interpretaai-server.jar"
cp services/kokoro/app.py services/kokoro/requirements.txt "$bundle_dir/kokoro/"
cp deploy/oracle/README.md deploy/oracle/Caddyfile deploy/oracle/caddy.env.example \
  deploy/oracle/server.env.example deploy/oracle/install.sh deploy/oracle/verify-public.sh \
  "$bundle_dir/"
cp deploy/oracle/interpretaai-server.service deploy/oracle/interpretaai-kokoro.service \
  "$bundle_dir/systemd/"
cp deploy/oracle/caddy.service.d/interpretaai.conf "$bundle_dir/systemd/caddy.service.d/"
cp deploy/oracle/ollama.service.d/interpretaai.conf "$bundle_dir/systemd/ollama.service.d/"
chmod 0755 "$bundle_dir/install.sh" "$bundle_dir/verify-public.sh"

if rg -n --hidden '(nvapi-|AIza[0-9A-Za-z_-]{20,}|AQ\.[0-9A-Za-z_-]{20,})' "$bundle_dir"; then
  echo "ERRO: possível credencial encontrada no pacote Oracle." >&2
  exit 1
fi

(
  cd "$bundle_dir"
  find . -type f ! -name MANIFEST.sha256 -print0 \
    | sort -z \
    | xargs -0 shasum -a 256 > MANIFEST.sha256
)

mkdir -p "$(dirname "$output_file")"
tar -C "$stage_dir" -czf "$output_file" interpretaai-oracle

echo "Pacote Oracle: $output_file"
echo "SHA-256: $(shasum -a 256 "$output_file" | awk '{print $1}')"
