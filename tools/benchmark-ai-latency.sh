#!/usr/bin/env bash
set -euo pipefail

# Compara o caminho real Spring + LangChain4j usando apenas uma fala sintética fixa.
# As chaves entram somente pelo ambiente e nunca são impressas ou gravadas no repositório.

iterations="${1:-5}"
benchmark_port="${BENCHMARK_PORT:-18088}"
base_url="http://127.0.0.1:${benchmark_port}"
# Não coincide com acceptedAnswers: assim o ensaio mede o provedor, não o atalho do ScenePack.
synthetic_transcript="Eu vi uma coisa redonda perto do chão"

if ! [[ "$iterations" =~ ^[1-9][0-9]*$ ]]; then
  echo "Uso: $0 [iteracoes-positivas]" >&2
  exit 2
fi

missing=()
[[ -n "${GEMINI_API_KEY:-}" ]] || missing+=(GEMINI_API_KEY)
[[ -n "${NVIDIA_API_KEY:-}" ]] || missing+=(NVIDIA_API_KEY)
if (( ${#missing[@]} > 0 )); then
  echo "Variaveis ausentes: ${missing[*]}" >&2
  echo "Exporte chaves novas no terminal; o script nao aceita nem registra chaves em argumentos." >&2
  exit 2
fi

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
temporary_dir="$(mktemp -d "${TMPDIR:-/tmp}/interpretaai-benchmark.XXXXXX")"
server_pid=""

cleanup() {
  if [[ -n "$server_pid" ]] && kill -0 "$server_pid" 2>/dev/null; then
    kill "$server_pid" 2>/dev/null || true
    wait "$server_pid" 2>/dev/null || true
  fi
  rm -rf "$temporary_dir"
}
trap cleanup EXIT INT TERM

cd "$repo_dir"
./gradlew :server:bootJar --quiet
server_jar="$(find server/build/libs -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' -print -quit)"
if [[ -z "$server_jar" ]]; then
  echo "JAR do servidor nao encontrado." >&2
  exit 1
fi

wait_for_health() {
  local attempts=0
  until curl --silent --fail "$base_url/actuator/health" >/dev/null; do
    attempts=$((attempts + 1))
    if (( attempts >= 80 )); then
      echo "Servidor nao ficou pronto; consulte $temporary_dir/server.log" >&2
      return 1
    fi
    sleep 0.25
  done
}

stop_server() {
  if [[ -n "$server_pid" ]] && kill -0 "$server_pid" 2>/dev/null; then
    kill "$server_pid" 2>/dev/null || true
    wait "$server_pid" 2>/dev/null || true
  fi
  server_pid=""
}

start_server() {
  local provider="$1"
  local model="$2"
  local data_file="$temporary_dir/${provider}-db"
  PORT="$benchmark_port" \
  DATABASE_URL="jdbc:h2:file:${data_file};MODE=PostgreSQL" \
  CONVERSATION_PROVIDER="$provider" \
  GEMINI_MODEL="gemini-3.8-flash" \
  GEMINI_THINKING_LEVEL="LOW" \
  NVIDIA_MODEL="mistralai/mistral-nemotron" \
  NVIDIA_WARMUP_ENABLED="true" \
  REMOTE_WARMUP_INITIAL_DELAY_MS="600000" \
  java -jar "$server_jar" >"$temporary_dir/server.log" 2>&1 &
  server_pid=$!
  wait_for_health
  echo "Rota pronta: $provider ($model)" >&2
}

wait_for_hot_route() {
  local provider="$1"
  curl --silent --fail -X POST "$base_url/api/v1/gateway/warmup" >/dev/null
  local attempts=0
  while :; do
    local state
    state="$(curl --silent --fail "$base_url/api/v1/gateway/status" \
      | python3 -c 'import json,sys; print(json.load(sys.stdin)["state"])')"
    [[ "$state" == "HOT" ]] && return 0
    attempts=$((attempts + 1))
    if (( attempts >= 60 )); then
      echo "$provider nao ficou HOT em 30 s; benchmark interrompido." >&2
      return 1
    fi
    sleep 0.5
  done
}

measure_provider() {
  local provider="$1"
  local run_number="$2"
  PROVIDER="$provider" RUN_NUMBER="$run_number" BASE_URL="$base_url" \
    SYNTHETIC_TRANSCRIPT="$synthetic_transcript" python3 - <<'PY'
import json
import os
import time
import urllib.request

provider = os.environ["PROVIDER"]
run_number = int(os.environ["RUN_NUMBER"])
payload = json.dumps({
    "sessionId": f"synthetic-{provider}-{run_number}",
    "sceneId": "comic-ball",
    "turn": 1,
    "transcript": os.environ["SYNTHETIC_TRANSCRIPT"],
    "speaker": "LEIA_FEMALE",
    "reducedStimuli": True,
}).encode()
request = urllib.request.Request(
    os.environ["BASE_URL"] + "/api/v1/voice-turn/stream",
    data=payload,
    method="POST",
    headers={
        "Content-Type": "application/json",
        "Idempotency-Key": f"benchmark-{provider}-{run_number}-{time.time_ns()}",
    },
)
started = time.perf_counter()
ack_ms = None
final_text_ms = None
server_validated_ms = None
server_complete_ms = None
conversation_degraded = None
reply_text = ""
next_action = ""
with urllib.request.urlopen(request, timeout=8) as response:
    for raw_line in response:
        event = json.loads(raw_line)
        elapsed_ms = round((time.perf_counter() - started) * 1000)
        if event["type"] == "ACK":
            ack_ms = elapsed_ms
        if event["type"] == "FINAL_TEXT":
            final_text_ms = elapsed_ms
            server_validated_ms = event.get("serverElapsedMs")
            conversation_degraded = event["response"]["degraded"]
            reply_text = event["response"]["replyText"]
            next_action = event["response"]["nextAction"]
        if event["type"] in ("COMPLETE", "FALLBACK"):
            server_complete_ms = event.get("serverElapsedMs")
            complete_degraded = event["response"]["degraded"]
            question_count = reply_text.count("?")
            action_consistent = ((next_action == "SPEAK_AGAIN" and question_count == 1)
                                 or (next_action == "CONTINUE" and question_count == 0))
            forbidden = ("você errou", "nota", "diagnóstico")
            quality_ok = action_consistent and not reply_text.casefold().startswith("lia,") \
                and not any(term in reply_text.casefold() for term in forbidden)
            print(f"{provider},{run_number},{ack_ms or elapsed_ms},{final_text_ms or elapsed_ms},{elapsed_ms},"
                  f"{server_validated_ms if server_validated_ms is not None else -1},"
                  f"{server_complete_ms if server_complete_ms is not None else -1},"
                  f"{str(conversation_degraded if conversation_degraded is not None else complete_degraded).lower()},"
                  f"{str(complete_degraded).lower()},{str(quality_ok).lower()}")
            break
PY
}

results_file="$temporary_dir/results.csv"
echo "provider,run,ack_ms,validated_text_ms,complete_ms,server_validated_ms,server_complete_ms,conversation_degraded,complete_degraded,quality_ok" | tee "$results_file"
benchmark_incomplete=0

for provider in gemini nvidia; do
  if [[ "$provider" == "gemini" ]]; then
    model="gemini-3.8-flash"
  else
    model="mistralai/mistral-nemotron"
  fi
  start_server "$provider" "$model"
  if ! wait_for_hot_route "$provider"; then
    benchmark_incomplete=3
    stop_server
    continue
  fi

  # Uma execução sintética fora da amostra aquece DNS, TLS, pool HTTP e rota do provedor.
  if ! measure_provider "$provider" 0 >/dev/null; then
    echo "$provider falhou na execução de preparação; amostra ignorada." >&2
    benchmark_incomplete=3
    stop_server
    continue
  fi
  for ((run_number = 1; run_number <= iterations; run_number++)); do
    if ! measurement="$(measure_provider "$provider" "$run_number")"; then
      echo "$provider falhou durante a amostra $run_number." >&2
      benchmark_incomplete=3
      break
    fi
    printf '%s\n' "$measurement" | tee -a "$results_file"
  done
  stop_server
done

RESULTS_FILE="$results_file" python3 - <<'PY'
import csv
import math
import os
import statistics

def percentile(values, probability):
    ordered = sorted(values)
    return ordered[max(0, math.ceil(probability * len(ordered)) - 1)]

with open(os.environ["RESULTS_FILE"], newline="") as stream:
    rows = list(csv.DictReader(stream))

print("\nResumo (ms; menor e melhor):")
for provider in ("gemini", "nvidia"):
    provider_rows = [row for row in rows if row["provider"] == provider]
    ack_values = [int(row["ack_ms"]) for row in provider_rows]
    valid_values = [int(row["validated_text_ms"]) for row in provider_rows
                    if row["conversation_degraded"] == "false" and row["quality_ok"] == "true"]
    complete_values = [int(row["complete_ms"]) for row in provider_rows
                       if row["conversation_degraded"] == "false" and row["quality_ok"] == "true"]
    server_valid_values = [int(row["server_validated_ms"]) for row in provider_rows
                           if row["conversation_degraded"] == "false"
                           and row["quality_ok"] == "true" and int(row["server_validated_ms"]) >= 0]
    server_complete_values = [int(row["server_complete_ms"]) for row in provider_rows
                              if row["conversation_degraded"] == "false"
                              and row["quality_ok"] == "true" and int(row["server_complete_ms"]) >= 0]
    fallback_values = [int(row["validated_text_ms"]) for row in provider_rows
                       if row["conversation_degraded"] == "true"]
    invalid_quality = sum(row["conversation_degraded"] == "false" and row["quality_ok"] != "true"
                          for row in provider_rows)
    if not provider_rows:
        print(f"{provider}: indisponivel; sem amostra valida")
        continue
    if valid_values:
        print(f"{provider}: validas={len(valid_values)} "
              f"ack_p50={round(statistics.median(ack_values))} ack_p95={percentile(ack_values, .95)} "
              f"texto_p50={round(statistics.median(valid_values))} texto_p95={percentile(valid_values, .95)} "
              f"completo_p50={round(statistics.median(complete_values))} "
              f"completo_p95={percentile(complete_values, .95)} "
              f"servidor_texto_p50={round(statistics.median(server_valid_values)) if server_valid_values else 'n/a'} "
              f"servidor_completo_p50={round(statistics.median(server_complete_values)) if server_complete_values else 'n/a'} "
              f"fallbacks={len(fallback_values)} "
              f"qualidade_invalida={invalid_quality}")
    else:
        fallback_summary = (f" mediana_fallback={round(statistics.median(fallback_values))}"
                            if fallback_values else "")
        print(f"{provider}: validas=0 fallbacks={len(fallback_values)}"
              f" qualidade_invalida={invalid_quality}{fallback_summary}; sem amostra promovível")
PY

echo "Amostra temporaria removida ao encerrar; copie somente o resumo sem chaves se quiser documentar." >&2
exit "$benchmark_incomplete"
