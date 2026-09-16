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

shell_value() {
  "$adb_bin" shell "$@" 2>/dev/null | tr -d '\r' || true
}

device_policy_dump="$(shell_value dumpsys device_policy)"
if printf '%s' "$device_policy_dump" | grep -qiE 'Device Owner|device-owner'; then
  device_owner_state=configured
elif [[ -n "$device_policy_dump" ]]; then
  device_owner_state=not_detected
else
  device_owner_state=unknown
fi

printf 'INTERPRETAAI_TABLET_AUDIT_V2\n'
printf 'manufacturer=%s\n' "$(property ro.product.manufacturer)"
printf 'model=%s\n' "$(property ro.product.model)"
printf 'device=%s\n' "$(property ro.product.device)"
printf 'android_version=%s\n' "$(property ro.build.version.release)"
printf 'android_sdk=%s\n' "$(property ro.build.version.sdk)"
printf 'abis=%s\n' "$(property ro.product.cpu.abilist)"
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
printf 'device_owner=%s\n' "$device_owner_state"
printf 'voice_recognition_service=%s\n' "$(shell_value settings get secure voice_recognition_service)"
printf 'tts_default_engine=%s\n' "$(shell_value settings get secure tts_default_synth)"
if shell_value pm path br.gov.interpretaai | grep -q '^package:'; then
  printf 'interpretaai_installed=yes\n'
else
  printf 'interpretaai_installed=no\n'
fi

printf '\nNenhum número de série, conta, arquivo ou conteúdo do estudante foi coletado.\n'
