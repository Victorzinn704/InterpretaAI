#!/usr/bin/env bash
set -euo pipefail

adb_bin="${ADB_BIN:-adb}"

if ! command -v "$adb_bin" >/dev/null 2>&1; then
  printf 'Erro: adb não encontrado. Defina ADB_BIN com o caminho do executável.\n' >&2
  exit 1
fi

device_count="$("$adb_bin" devices | awk 'NR > 1 && $2 == "device" {count++} END {print count+0}')"
if [[ "$device_count" -ne 1 ]]; then
  printf 'Erro: conecte exatamente um tablet autorizado; encontrados: %s.\n' "$device_count" >&2
  exit 1
fi

property() {
  local key="$1"
  "$adb_bin" shell getprop "$key" | tr -d '\r'
}

printf 'INTERPRETAAI_TABLET_AUDIT_V1\n'
printf 'manufacturer=%s\n' "$(property ro.product.manufacturer)"
printf 'model=%s\n' "$(property ro.product.model)"
printf 'device=%s\n' "$(property ro.product.device)"
printf 'android_version=%s\n' "$(property ro.build.version.release)"
printf 'android_sdk=%s\n' "$(property ro.build.version.sdk)"
printf 'security_patch=%s\n' "$(property ro.build.version.security_patch)"
printf 'screen=%s\n' "$("$adb_bin" shell wm size | tr '\n' ';' | tr -d '\r')"
printf 'density=%s\n' "$("$adb_bin" shell wm density | tr '\n' ';' | tr -d '\r')"
printf 'ram_kib=%s\n' "$("$adb_bin" shell cat /proc/meminfo | awk '/MemTotal/ {print $2}' | tr -d '\r')"
printf 'input_features=%s\n' "$("$adb_bin" shell pm list features \
  | tr -d '\r' \
  | awk -F: '/touchscreen|faketouch|stylus|microphone|camera/ {printf "%s,", $2}' \
  | sed 's/,$//')"
printf 'lock_task=%s\n' "$("$adb_bin" shell dumpsys activity activities \
  | awk -F= '/mLockTaskModeState=/ {print $2; exit}' \
  | tr -d ' \r')"

printf '\nNenhum número de série, conta, arquivo ou conteúdo do estudante foi coletado.\n'
