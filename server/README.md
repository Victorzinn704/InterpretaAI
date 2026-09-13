# Servidor de mediação da LEIA

O servidor expõe `POST /api/v1/voice-turn`. O modo padrão usa Qwen 2.5 3B local via Ollama
para a mediação e Kokoro para as vozes `pf_dora` (LEIA) e `pm_alex` (Davi). Não há cobrança
por chamada nem chave no APK. O áudio e a transcrição nunca são registrados nos logs.

```bash
./tools/start-local-mvp.sh
./gradlew :server:test
```

O Spring escuta em `http://127.0.0.1:8088`, o Ollama em 11434 e o Kokoro em 8091. Se um
provedor não responder, o contrato continua válido com fala preparada e `degraded=true`.
O adaptador Gemini/Google TTS permanece no código somente como opção futura e não é o padrão
do produto infantil. Veja o [contrato da API](../docs/VOICE_API.md) e o
[guia do microservidor](../docs/LOCAL_MVP_SERVER.md).
