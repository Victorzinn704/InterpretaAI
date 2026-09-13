#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 || "$1" != https://* ]]; then
  echo "Uso: $0 https://URL-HTTPS-DO-SERVIDOR" >&2
  exit 1
fi

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
api_url="${1%/}"
desktop_dir="/Users/joaovictordemoraesdacruz/Desktop/InterpretaAI-Entrega-11h45"
apk_name="InterpretaAI-mvp-debug.apk"

curl -fsS --max-time 20 "$api_url/actuator/health" >/dev/null || {
  echo "O servidor não respondeu UP em $api_url" >&2
  exit 1
}

cd "$repo_dir"
./gradlew :app:assembleDebug -PvoiceApiUrl="$api_url"
mkdir -p dist "$desktop_dir"
cp app/build/outputs/apk/debug/app-debug.apk "dist/$apk_name"
cp "dist/$apk_name" "$desktop_dir/$apk_name"

apk_hash="$(shasum -a 256 "dist/$apk_name" | awk '{print $1}')"
pdf_hash="$(shasum -a 256 dist/InterpretaAI-Proposta-MVP.pdf | awk '{print $1}')"
printf '%s  %s\n%s  %s\n' "$apk_hash" "$apk_name" "$pdf_hash" "InterpretaAI-Proposta-MVP.pdf" >dist/SHA256.txt
cp dist/SHA256.txt "$desktop_dir/SHA256.txt"

echo "APK online: $desktop_dir/$apk_name"
echo "SHA-256: $apk_hash"
