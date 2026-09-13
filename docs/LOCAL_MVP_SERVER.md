# Microservidor local do MVP

## O que está funcionando

O Mac executa quatro peças pequenas: Android chama o Spring Boot em 8088; Spring usa LangChain4j
para conversar com `qwen2.5:3b` no Ollama; o texto aprovado vira áudio pt-BR no Kokoro; um túnel
Cloudflare temporário fornece HTTPS ao APK. A foto da missão permanece no aparelho e é analisada
localmente pelo ML Kit.

```text
Android ── HTTPS temporário ──► Spring :8088
                                  ├──► Ollama/Qwen :11434
                                  └──► Kokoro pt-BR :8091
```

## Iniciar e testar no Mac

Requisitos instalados: Homebrew, Java 21, Python 3.12, Ollama e cloudflared. Na raiz do projeto:

```bash
./tools/start-local-mvp.sh
curl http://127.0.0.1:8088/actuator/health
```

O script cria a venv do Kokoro quando necessário, baixa `qwen2.5:3b`, aquece o modelo e inicia os
serviços. Logs e PIDs criados pelo script ficam em `tmp/local-mvp/`, que não entra no Git.

## Conectar um celular para a apresentação

Em outro terminal:

```bash
./tools/start-demo-tunnel.sh
./tools/build-online-apk.sh https://URL-MOSTRADA.trycloudflare.com
```

O segundo comando recompila e copia o APK para `dist/` e para a pasta de entrega no Desktop. Antes
da apresentação, abra `https://URL-MOSTRADA.trycloudflare.com/actuator/health` e confirme `UP`.
Defina `INTERPRETAAI_DELIVERY_DIR` para copiar a entrega a outro destino.
Quick Tunnel não tem garantia de disponibilidade: Mac, Ollama, Kokoro, Spring e cloudflared devem
permanecer ligados. Não use nomes, fotos faciais ou outros dados pessoais nessa demonstração.

## Falha segura

O Android espera no máximo seis segundos. Sem rede, a atividade continua com “A LEIA está sem
internet, mas continua com você” e TTS local. O servidor também devolve `degraded=true` se Qwen ou
Kokoro falhar. O limite é três interações por sessão, com respostas de até duas frases.

## Migração para Oracle

A migração mantém o contrato `POST /api/v1/voice-turn`. Na VM Oracle ARM64, instale Java 21,
Ollama e Python 3.12, copie somente o JAR do Spring e `services/kokoro`, e execute os três processos
como serviços do systemd ligados a `127.0.0.1`. Publique apenas o Spring atrás de Caddy ou Nginx com
HTTPS, autenticação de dispositivo, limite de requisições e firewall. Depois gere um novo APK com a
URL estável. Não exponha as portas 11434 e 8091 à internet.

Para piloto real ainda faltam: política de consentimento e retenção, autenticação, banco sincronizado,
monitoramento, backup e avaliação pedagógica. O túnel atual prova a conversa; não representa a
infraestrutura final.
