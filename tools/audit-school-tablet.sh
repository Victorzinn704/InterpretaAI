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

android_sdk="$(property ro.build.version.sdk)"
input_features="$("$adb_bin" shell pm list features \
  | tr -d '\r' \
  | awk -F: '/touchscreen|faketouch|stylus|microphone|camera/ {printf "%s,", $2}' \
  | sed 's/,$//')"
screen_dump="$("$adb_bin" shell wm size | tr '\n' ';' | tr -d '\r')"
density_dump="$("$adb_bin" shell wm density | tr '\n' ';' | tr -d '\r')"
lock_task_state="$("$adb_bin" shell dumpsys activity activities \
  | awk -F= '/mLockTaskModeState=/ {print $2; exit}' \
  | tr -d ' \r')"

screen_px="$(printf '%s' "$screen_dump" | grep -oE '[0-9]+x[0-9]+' | tail -1 || true)"
density_dpi="$(printf '%s' "$density_dump" | grep -oE '[0-9]+' | tail -1 || true)"
short_edge_dp=unknown
screen_class=unknown
if [[ "$screen_px" =~ ^([0-9]+)x([0-9]+)$ ]]; then
  width_px="${BASH_REMATCH[1]}"
  height_px="${BASH_REMATCH[2]}"
  if [[ "$density_dpi" =~ ^[1-9][0-9]*$ ]]; then
    short_edge_px="$width_px"
    if (( height_px < short_edge_px )); then short_edge_px="$height_px"; fi
    short_edge_dp="$((short_edge_px * 160 / density_dpi))"
    if (( short_edge_dp >= 600 )); then screen_class=tablet; else screen_class=compact; fi
  fi
fi

if [[ "$android_sdk" =~ ^[0-9]+$ ]] && (( android_sdk >= 26 )); then
  install_compatibility=compatible
else
  install_compatibility=incompatible
fi
if [[ ",$input_features," == *",android.hardware.touchscreen,"* || \
      ",$input_features," == *",android.hardware.faketouch,"* ]]; then
  touch_readiness=ready
else
  touch_readiness=not_detected
fi
if [[ ",$input_features," == *",android.hardware.microphone,"* ]]; then
  voice_input_readiness=hardware_present
else
  voice_input_readiness=touch_fallback_only
fi
if [[ "$device_owner_state" == configured && "$lock_task_state" == LOCKED ]]; then
  managed_focus_readiness=verified_locked
elif [[ "$device_owner_state" == configured ]]; then
  managed_focus_readiness=provisioned_not_active
else
  managed_focus_readiness=requires_device_owner_validation
fi

printf 'INTERPRETAAI_TABLET_AUDIT_V3\n'
printf 'manufacturer=%s\n' "$(property ro.product.manufacturer)"
printf 'model=%s\n' "$(property ro.product.model)"
printf 'device=%s\n' "$(property ro.product.device)"
printf 'android_version=%s\n' "$(property ro.build.version.release)"
printf 'android_sdk=%s\n' "$android_sdk"
printf 'abis=%s\n' "$(property ro.product.cpu.abilist)"
printf 'security_patch=%s\n' "$(property ro.build.version.security_patch)"
printf 'screen=%s\n' "$screen_dump"
printf 'density=%s\n' "$density_dump"
printf 'short_edge_dp=%s\n' "$short_edge_dp"
printf 'screen_class=%s\n' "$screen_class"
printf 'ram_kib=%s\n' "$("$adb_bin" shell cat /proc/meminfo | awk '/MemTotal/ {print $2}' | tr -d '\r')"
printf 'input_features=%s\n' "$input_features"
printf 'lock_task=%s\n' "$lock_task_state"
printf 'device_owner=%s\n' "$device_owner_state"
printf 'voice_recognition_service=%s\n' "$(shell_value settings get secure voice_recognition_service)"
printf 'tts_default_engine=%s\n' "$(shell_value settings get secure tts_default_synth)"
if shell_value pm path br.gov.interpretaai | grep -q '^package:'; then
  printf 'interpretaai_installed=yes\n'
else
  printf 'interpretaai_installed=no\n'
fi

printf '\nDECISAO_DE_PILOTO\n'
printf 'install_compatibility=%s\n' "$install_compatibility"
printf 'touch_readiness=%s\n' "$touch_readiness"
printf 'voice_input_readiness=%s\n' "$voice_input_readiness"
printf 'managed_focus_readiness=%s\n' "$managed_focus_readiness"
printf 'stylus_requirement=optional\n'

printf '\nNenhum número de série, conta, arquivo ou conteúdo do estudante foi coletado.\n'
