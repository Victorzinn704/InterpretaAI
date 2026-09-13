# Servidor de mediação da LEIA

O servidor expõe `POST /api/v1/voice-turn`. Sem credenciais ele inicia normalmente e
responde com mediação preparada (`degraded=true`); com `GEMINI_API_KEY` usa Gemini 2.5
Flash via LangChain4j. Para áudio Cloud TTS, configure credenciais ADC da service account
e `GOOGLE_TTS_ENABLED=true`. O áudio e a transcrição nunca são registrados nos logs.

```bash
./gradlew :server:bootRun
./gradlew :server:test
```

Para Cloud Run, gere o JAR com `./gradlew :server:bootJar`, construa a imagem a partir da
raiz do projeto e associe `GEMINI_API_KEY` ao Secret Manager. Região prevista:
`southamerica-east1`. Não inclua chaves na imagem, no Git ou no APK.
