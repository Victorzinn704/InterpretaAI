#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
state_dir="$repo_dir/tmp/local-mvp"
python_bin="${PYTHON_BIN:-python3.12}"
model_name="${OLLAMA_MODEL:-qwen2.5:1.5b}"
server_port="${PORT:-8088}"
mkdir -p "$state_dir"

for command_name in brew ollama curl "$python_bin"; do
  command -v "$command_name" >/dev/null || { echo "Falta instalar: $command_name" >&2; exit 1; }
done

brew services start ollama >/dev/null
for _ in {1..30}; do
  curl -fsS http://127.0.0.1:11434/api/tags >/dev/null && break
  sleep 1
done
curl -fsS http://127.0.0.1:11434/api/tags >/dev/null || { echo "Ollama não iniciou" >&2; exit 1; }

if ! ollama list | awk 'NR > 1 {print $1}' | grep -Fxq "$model_name"; then
  ollama pull "$model_name"
fi

curl -fsS --max-time 60 http://127.0.0.1:11434/api/generate \
  -H 'Content-Type: application/json' \
  -d "{\"model\":\"$model_name\",\"prompt\":\"Responda somente OK.\",\"stream\":false,\"keep_alive\":\"30m\"}" \
  >/dev/null

venv_dir="$repo_dir/services/kokoro/.venv"
if [[ ! -x "$venv_dir/bin/python" ]]; then
  "$python_bin" -m venv "$venv_dir"
  "$venv_dir/bin/pip" install -r "$repo_dir/services/kokoro/requirements.txt"
fi

if ! curl -fsS http://127.0.0.1:8091/health >/dev/null 2>&1; then
  nohup "$venv_dir/bin/python" "$repo_dir/services/kokoro/app.py" \
    >"$state_dir/kokoro.log" 2>&1 &
  echo "$!" >"$state_dir/kokoro.pid"
fi
for _ in {1..60}; do
  curl -fsS http://127.0.0.1:8091/health >/dev/null && break
  sleep 1
done
curl -fsS http://127.0.0.1:8091/health >/dev/null || { echo "Kokoro não iniciou" >&2; exit 1; }

if ! curl -fsS "http://127.0.0.1:$server_port/actuator/health" >/dev/null 2>&1; then
  (
    cd "$repo_dir"
    nohup env DEBUG=false PORT="$server_port" CONVERSATION_PROVIDER=ollama \
      OLLAMA_MODEL="$model_name" SPEECH_PROVIDER=kokoro ./gradlew :server:bootRun \
      >"$state_dir/server.log" 2>&1 &
    echo "$!" >"$state_dir/server.pid"
  )
fi
for _ in {1..60}; do
  curl -fsS "http://127.0.0.1:$server_port/actuator/health" >/dev/null && break
  sleep 1
done
curl -fsS "http://127.0.0.1:$server_port/actuator/health" >/dev/null || { echo "Spring não iniciou" >&2; exit 1; }

echo "InterpretaAI local: http://127.0.0.1:$server_port (UP)"
echo "Próximo passo: ./tools/start-demo-tunnel.sh"
