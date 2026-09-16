#!/usr/bin/env bash
set -euo pipefail

base_url="${1:-}"
iterations="${VERIFY_ITERATIONS:-5}"
if [[ -z "$base_url" ]]; then
  echo "Uso: INTERPRETAAI_DEVICE_TOKEN=... $0 https://api.seudominio.com" >&2
  exit 2
fi
base_url="${base_url%/}"
if [[ "$base_url" != https://* && "$base_url" != http://127.0.0.1:* && "$base_url" != http://localhost:* ]]; then
  echo "A URL publica deve usar HTTPS; HTTP e aceito somente em loopback." >&2
  exit 2
fi
if ! [[ "$iterations" =~ ^[1-9][0-9]*$ ]]; then
  echo "VERIFY_ITERATIONS deve ser um inteiro positivo." >&2
  exit 2
fi
command -v python3 >/dev/null 2>&1 || { echo "python3 ausente" >&2; exit 2; }

BASE_URL="$base_url" VERIFY_ITERATIONS="$iterations" \
INTERPRETAAI_DEVICE_TOKEN="${INTERPRETAAI_DEVICE_TOKEN:-}" python3 - <<'PY'
import json
import math
import os
import statistics
import sys
import time
import urllib.error
import urllib.request

base_url = os.environ["BASE_URL"]
iterations = int(os.environ["VERIFY_ITERATIONS"])
device_token = os.environ.get("INTERPRETAAI_DEVICE_TOKEN", "")

def open_json(path):
    with urllib.request.urlopen(base_url + path, timeout=8) as response:
        return response.status, dict(response.headers), json.load(response)

health_status, _, health = open_json("/actuator/health")
gateway_status, _, gateway = open_json("/api/v1/gateway/status")
if health_status != 200 or health.get("status") != "UP":
    raise SystemExit("health check invalido")
if gateway_status != 200 or "state" not in gateway:
    raise SystemExit("gateway status invalido")

if device_token:
    unauthorized_payload = json.dumps({
        "sessionId": "synthetic-auth-check",
        "sceneId": "gallery-1",
        "turn": 1,
        "transcript": "Uma pista redonda",
        "speaker": "LEIA_FEMALE",
        "reducedStimuli": True,
    }).encode()
    unauthorized_request = urllib.request.Request(
        base_url + "/api/v1/voice-turn",
        data=unauthorized_payload,
        method="POST",
        headers={"Content-Type": "application/json", "Idempotency-Key": "synthetic-auth-check"},
    )
    try:
        urllib.request.urlopen(unauthorized_request, timeout=8)
        raise SystemExit("endpoint aceitou turno sem token")
    except urllib.error.HTTPError as error:
        if error.code != 401:
            raise SystemExit(f"turno sem token retornou {error.code}, esperado 401")

samples = []
for run in range(1, iterations + 1):
    payload = json.dumps({
        "sessionId": f"synthetic-public-{time.time_ns()}-{run}",
        "sceneId": "gallery-1",
        "turn": 1,
        "transcript": "Eu percebi uma pista redonda perto do chão",
        "speaker": "LEIA_FEMALE",
        "reducedStimuli": True,
    }).encode()
    headers = {
        "Accept": "application/x-ndjson",
        "Content-Type": "application/json",
        "Idempotency-Key": f"public-verify-{time.time_ns()}-{run}",
    }
    if device_token:
        headers["X-Device-Token"] = device_token
    request = urllib.request.Request(
        base_url + "/api/v1/voice-turn/stream", data=payload, method="POST", headers=headers
    )
    started = time.perf_counter()
    events = []
    timings = {}
    degraded = True
    cache_control = ""
    with urllib.request.urlopen(request, timeout=8) as response:
        cache_control = response.headers.get("Cache-Control", "")
        for raw_line in response:
            event = json.loads(raw_line)
            if event.get("protocolVersion") != 1:
                raise SystemExit("versao de protocolo inesperada")
            event_type = event.get("type")
            events.append(event_type)
            timings[event_type] = round((time.perf_counter() - started) * 1000)
            if event_type in ("COMPLETE", "FALLBACK"):
                degraded = bool(event["response"].get("degraded", True))
                break
    if events[:2] != ["ACK", "FINAL_TEXT"] or not events[-1:] or events[-1] not in ("COMPLETE", "FALLBACK"):
        raise SystemExit(f"ordem de eventos invalida: {events}")
    if "no-store" not in cache_control.casefold():
        raise SystemExit("stream sem Cache-Control no-store")
    samples.append({
        "ack": timings["ACK"],
        "text": timings["FINAL_TEXT"],
        "complete": timings[events[-1]],
        "degraded": degraded,
    })

def percentile(values, probability):
    ordered = sorted(values)
    return ordered[max(0, math.ceil(probability * len(ordered)) - 1)]

summary = {
    "health": health["status"],
    "gatewayState": gateway["state"],
    "model": gateway.get("model", "not_exposed"),
    "samples": len(samples),
    "ackP50Ms": round(statistics.median(sample["ack"] for sample in samples)),
    "ackP95Ms": percentile([sample["ack"] for sample in samples], .95),
    "validatedTextP50Ms": round(statistics.median(sample["text"] for sample in samples)),
    "validatedTextP95Ms": percentile([sample["text"] for sample in samples], .95),
    "completeP95Ms": percentile([sample["complete"] for sample in samples], .95),
    "degradedTurns": sum(sample["degraded"] for sample in samples),
    "authenticationChecked": bool(device_token),
}
print(json.dumps(summary, ensure_ascii=False, indent=2))

if summary["ackP95Ms"] >= 300:
    print("NAO PROMOVER: ACK p95 acima de 300 ms", file=sys.stderr)
    raise SystemExit(3)
if summary["validatedTextP95Ms"] >= 3000:
    print("NAO PROMOVER: texto validado p95 acima de 3 s", file=sys.stderr)
    raise SystemExit(3)
if summary["degradedTurns"]:
    print("NAO PROMOVER: houve fallback/degradacao", file=sys.stderr)
    raise SystemExit(3)
PY

