# InterpretaAI — MVP Android + servidor LEIA

O InterpretaAI é um MVP de alfabetização mediada por voz. A criança atua como ajudante da LEIA,
observa histórias familiares, conta ideias, constrói palavras e aplica o que aprendeu com o grupo.

## Estado atual

- Home, gibi, formação da palavra e quebra-cabeça funcionam sem rolagem infantil;
- cada cena do gibi tem três estados: observar/ouvir, responder e receber reação;
- a criança pode responder por voz ou por alternativas preparadas;
- a reconexão aparece aos 20 s e fala uma única vez aos 40 s, sem culpa;
- quatro efeitos originais usam SoundPool e podem ser silenciados em “Reduzir estímulos”;
- métricas pedagógicas ficam em SQLite local, sem áudio bruto;
- Modo Foco entra automaticamente e foi validado como Device Owner em estado LOCKED;
- o servidor Spring Boot/LangChain4j expõe `POST /api/v1/voice-turn`;
- Qwen 2.5 3B via Ollama e Kokoro pt-BR executam localmente, sem cobrança por chamada;
- OCR e rótulos de objetos usam modelos ML Kit embarcados, sem enviar a foto ao servidor;
- sem servidor ou internet, o aplicativo usa fallback claramente identificado.
- ao final do ciclo curto, o celular descansa e a aprendizagem continua em dupla.

## Documentação

Comece pelo [índice da documentação](docs/README.md). Ele separa o que está implementado, o que foi
apenas demonstrado e o que é evolução futura, além de indicar a leitura por público.

## Organização do repositório

```text
app/       APK Android, interface infantil, métricas locais e Modo Foco
server/    API Java/Spring e adaptadores de conversa e voz
services/  serviço local de voz Kokoro
docs/      produto, arquitetura, operação, segurança e critérios
tools/     scripts reproduzíveis de execução, geração e empacotamento
output/    evidências visuais versionadas; temporários não entram no Git
dist/      únicos artefatos oficiais para entrega
```

## Compilar e testar

Requisitos: JDK 21 (gerando bytecode Java 17), Android SDK 35 e um emulador/dispositivo para instrumentação.

```bash
./gradlew :app:testDebugUnitTest :server:test
./gradlew :app:lintDebug :app:assembleDebug :server:bootJar
./gradlew :app:connectedDebugAndroidTest
./tools/check-delivery.sh
```

Use `./tools/check-delivery.sh --full` para repetir também os testes locais e o build antes de uma
nova entrega. A instrumentação Android continua separada porque exige emulador ou aparelho conectado.

O APK final fica em `dist/InterpretaAI-mvp-debug.apk` e o PDF de entrega em
`dist/InterpretaAI-Proposta-MVP.pdf`.

## Servidor local e IA

No Mac de demonstração, um comando inicia Ollama, aquece o Qwen, sobe Kokoro e executa o Spring na
porta 8088:

```bash
./tools/start-local-mvp.sh
```

Para um telefone fora da rede local, abra outro terminal e execute `./tools/start-demo-tunnel.sh`.
Copie a URL HTTPS apresentada e gere o APK conectado:

```bash
./tools/build-online-apk.sh https://URL-DO-TUNEL.trycloudflare.com
```

O túnel rápido existe apenas para demonstração: o Mac precisa permanecer ligado e sua URL muda ao
reiniciar. O procedimento completo e a migração para Oracle estão em
[docs/LOCAL_MVP_SERVER.md](docs/LOCAL_MVP_SERVER.md). Nenhuma chave entra no Git ou APK.

## Modo Foco

Em tablet institucional provisionado como Device Owner, Lock Task bloqueia Home e Recentes. Em
aparelho comum, Android exige confirmação adulta para fixação de tela. Veja [docs/KIOSK.md](docs/KIOSK.md).

## Privacidade

O MVP limita transcrição a 280 caracteres, apaga áudio temporário após reprodução e não registra
áudio/transcrição nos logs da aplicação. Antes de piloto real ainda são necessários identidade
institucional, consentimento aplicável, retenção, criptografia de sincronização e avaliação de impacto.
