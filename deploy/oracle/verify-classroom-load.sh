#!/usr/bin/env bash
set -euo pipefail

# Ensaio sintético de concorrência. Nunca use fala, áudio ou identificadores reais de crianças.
base_url="${1:-}"
concurrency="${LOAD_CONCURRENCY:-2}"
turns="${LOAD_TURNS:-12}"
require_hot="${LOAD_REQUIRE_HOT:-true}"
warmup_seconds="${LOAD_WARMUP_SECONDS:-30}"

if [[ -z "$base_url" ]]; then
  echo "Uso: INTERPRETAAI_DEVICE_TOKEN=... $0 https://api.seudominio.com" >&2
  exit 2
fi
base_url="${base_url%/}"
if [[ "$base_url" != https://* && "$base_url" != http://127.0.0.1:* && "$base_url" != http://localhost:* ]]; then
  echo "A URL publica deve usar HTTPS; HTTP e aceito somente em loopback." >&2
  exit 2
fi
if ! [[ "$concurrency" =~ ^[1-9][0-9]*$ && "$turns" =~ ^[1-9][0-9]*$ ]]; then
  echo "LOAD_CONCURRENCY e LOAD_TURNS devem ser inteiros positivos." >&2
  exit 2
fi
if (( concurrency > turns )); then
  echo "LOAD_CONCURRENCY nao pode ser maior que LOAD_TURNS." >&2
  exit 2
fi
if [[ "$require_hot" != "true" && "$require_hot" != "false" ]]; then
  echo "LOAD_REQUIRE_HOT deve ser true ou false." >&2
  exit 2
fi
if ! [[ "$warmup_seconds" =~ ^[1-9][0-9]*$ ]]; then
  echo "LOAD_WARMUP_SECONDS deve ser um inteiro positivo." >&2
  exit 2
fi
command -v python3 >/dev/null 2>&1 || { echo "python3 ausente" >&2; exit 2; }

BASE_URL="$base_url" LOAD_CONCURRENCY="$concurrency" LOAD_TURNS="$turns" \
LOAD_REQUIRE_HOT="$require_hot" LOAD_WARMUP_SECONDS="$warmup_seconds" \
INTERPRETAAI_DEVICE_TOKEN="${INTERPRETAAI_DEVICE_TOKEN:-}" python3 - <<'PY'
import concurrent.futures
import json
import math
import os
import statistics
import sys
import threading
import time
import urllib.error
import urllib.request

base_url = os.environ["BASE_URL"]
concurrency = int(os.environ["LOAD_CONCURRENCY"])
turns = int(os.environ["LOAD_TURNS"])
device_token = os.environ.get("INTERPRETAAI_DEVICE_TOKEN", "")
require_hot = os.environ.get("LOAD_REQUIRE_HOT", "true").casefold() == "true"
warmup_seconds = int(os.environ.get("LOAD_WARMUP_SECONDS", "30"))
ack_limit = int(os.environ.get("LOAD_ACK_P95_MS", "300"))
text_limit = int(os.environ.get("LOAD_TEXT_P95_MS", "3000"))
complete_limit = int(os.environ.get("LOAD_COMPLETE_P95_MS", "6000"))
max_degraded = int(os.environ.get("LOAD_MAX_DEGRADED", "0"))
start_gate = threading.Event()

def open_json(path):
    with urllib.request.urlopen(base_url + path, timeout=8) as response:
        return response.status, json.load(response)

def percentile(values, probability):
    ordered = sorted(values)
    return ordered[max(0, math.ceil(probability * len(ordered)) - 1)]

def synthetic_turn(run):
    payload = json.dumps({
        "sessionId": f"synthetic-load-{time.time_ns()}-{run}",
        "sceneId": "gallery-1",
        "turn": 1,
        "transcript": "Eu percebi uma pista redonda perto do chão",
        "speaker": "LEIA_FEMALE",
        "reducedStimuli": True,
    }).encode()
    headers = {
        "Accept": "application/x-ndjson",
        "Content-Type": "application/json",
        "Idempotency-Key": f"load-{time.time_ns()}-{run}",
    }
    if device_token:
        headers["X-Device-Token"] = device_token
    request = urllib.request.Request(
        base_url + "/api/v1/voice-turn/stream", data=payload, method="POST", headers=headers
    )
    start_gate.wait()
    started = time.perf_counter()
    events = []
    timings = {}
    degraded = True
    try:
        with urllib.request.urlopen(request, timeout=10) as response:
            cache_control = response.headers.get("Cache-Control", "")
            if "no-store" not in cache_control.casefold():
                raise ValueError("stream sem Cache-Control no-store")
            for raw_line in response:
                event = json.loads(raw_line)
                if event.get("protocolVersion") != 1:
                    raise ValueError("versao de protocolo inesperada")
                event_type = event.get("type")
                events.append(event_type)
                timings[event_type] = round((time.perf_counter() - started) * 1000)
                if event_type in ("COMPLETE", "FALLBACK"):
                    degraded = bool(event["response"].get("degraded", True))
                    break
        if events[:2] != ["ACK", "FINAL_TEXT"] or events[-1:] not in (["COMPLETE"], ["FALLBACK"]):
            raise ValueError(f"ordem de eventos invalida: {events}")
        return {
            "ok": True,
            "ack": timings["ACK"],
            "text": timings["FINAL_TEXT"],
            "complete": timings[events[-1]],
            "degraded": degraded,
        }
    except Exception as error:
        return {"ok": False, "error": type(error).__name__}

# O ensaio de capacidade deve começar quente; cold start é medido separadamente.
health_status, health = open_json("/actuator/health")
if health_status != 200 or health.get("status") != "UP":
    raise SystemExit("health check invalido")
try:
    warmup = urllib.request.Request(base_url + "/api/v1/gateway/warmup", data=b"", method="POST")
    urllib.request.urlopen(warmup, timeout=3).close()
except Exception:
    pass

gateway = {}
deadline = time.monotonic() + warmup_seconds
while True:
    gateway_status, gateway = open_json("/api/v1/gateway/status")
    if gateway_status != 200 or "state" not in gateway:
        raise SystemExit("gateway status invalido")
    if gateway["state"] == "HOT" or not require_hot or time.monotonic() >= deadline:
        break
    time.sleep(.5)
if require_hot and gateway["state"] != "HOT":
    print(json.dumps({
        "health": health["status"],
        "gatewayState": gateway["state"],
        "model": gateway.get("model", "not_exposed"),
        "syntheticOnly": True,
    }, ensure_ascii=False, indent=2))
    print("NAO PROMOVER: gateway nao ficou HOT antes do ensaio", file=sys.stderr)
    raise SystemExit(3)

with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as executor:
    futures = [executor.submit(synthetic_turn, run) for run in range(1, turns + 1)]
    wall_started = time.perf_counter()
    start_gate.set()
    results = [future.result() for future in futures]
    wall_seconds = time.perf_counter() - wall_started

valid = [result for result in results if result["ok"]]
failed = [result for result in results if not result["ok"]]
if not valid:
    print(json.dumps({
        "turns": turns,
        "concurrency": concurrency,
        "successful": 0,
        "failed": len(failed),
        "failureTypes": sorted({result["error"] for result in failed}),
    }, ensure_ascii=False, indent=2))
    raise SystemExit(3)

summary = {
    "health": health["status"],
    "gatewayState": gateway["state"],
    "model": gateway.get("model", "not_exposed"),
    "syntheticOnly": True,
    "turns": turns,
    "concurrency": concurrency,
    "successful": len(valid),
    "failed": len(failed),
    "degraded": sum(result["degraded"] for result in valid),
    "throughputTurnsPerSecond": round(len(results) / wall_seconds, 2),
    "ackP50Ms": round(statistics.median(result["ack"] for result in valid)),
    "ackP95Ms": percentile([result["ack"] for result in valid], .95),
    "validatedTextP50Ms": round(statistics.median(result["text"] for result in valid)),
    "validatedTextP95Ms": percentile([result["text"] for result in valid], .95),
    "completeP95Ms": percentile([result["complete"] for result in valid], .95),
    "failureTypes": sorted({result["error"] for result in failed}),
}
print(json.dumps(summary, ensure_ascii=False, indent=2))

problems = []
if failed:
    problems.append("houve falha de transporte ou contrato")
if summary["degraded"] > max_degraded:
    problems.append(f"degradacoes {summary['degraded']} acima do limite {max_degraded}")
if summary["ackP95Ms"] >= ack_limit:
    problems.append(f"ACK p95 acima de {ack_limit} ms")
if summary["validatedTextP95Ms"] >= text_limit:
    problems.append(f"texto p95 acima de {text_limit} ms")
if summary["completeP95Ms"] >= complete_limit:
    problems.append(f"conclusao p95 acima de {complete_limit} ms")
if problems:
    print("NAO PROMOVER: " + "; ".join(problems), file=sys.stderr)
    raise SystemExit(3)
PY
