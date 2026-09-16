# Contrato atual da API de voz

## Endpoint

`POST /api/v1/voice-turn` preserva a resposta JSON única para compatibilidade. O Android usa
`POST /api/v1/voice-turn/stream`, com a mesma requisição e resposta NDJSON progressiva.
`GET /actuator/health` é o health check.

O aplicativo envia também `Idempotency-Key: <uuid>` no cabeçalho. A mesma tentativa pode ser
reenviada com essa chave e recebe exatamente a resposta já concluída, sem nova inferência.

## Requisição

```json
{
  "sessionId": "demo-001",
  "sceneId": "comic-ball-01",
  "turn": 1,
  "transcript": "A bola pode estar perto da árvore",
  "speaker": "LEIA_FEMALE",
  "reducedStimuli": false
}
```

| Campo | Regra |
|---|---|
| `sessionId` | obrigatório; máximo de 80 caracteres |
| `sceneId` | obrigatório; máximo de 80 caracteres |
| `turn` | inteiro entre 1 e 3 |
| `transcript` | obrigatório; máximo de 280 caracteres |
| `speaker` | `LEIA_FEMALE` ou `DAVI_MALE` |
| `reducedStimuli` | booleano |

## Resposta

```json
{
  "replyText": "Você teve uma boa ideia! Como podemos procurar juntos?",
  "speaker": "LEIA_FEMALE",
  "audioBase64": "UklGR...",
  "audioMimeType": "audio/wav",
  "visualReaction": "CURIOUS",
  "nextAction": "SPEAK_AGAIN",
  "observationCategory": "CONTEXTUAL_HYPOTHESIS",
  "degraded": false
}
```

Enums fechados:

- `visualReaction`: `CURIOUS`, `ENCOURAGE`, `CELEBRATE`;
- `nextAction`: `SPEAK_AGAIN`, `CONTINUE`.

`observationCategory` é um sinal descritivo para observação pedagógica. Não é nota, diagnóstico ou
classificação absoluta da criança. `audioBase64` pode vir vazio no fallback de síntese.

## Troca progressiva

O endpoint `/voice-turn/stream` responde com uma linha JSON por evento, nesta ordem:

```jsonl
{"protocolVersion":1,"type":"ACK","serverElapsedMs":0,"response":null}
{"protocolVersion":1,"type":"FINAL_TEXT","serverElapsedMs":820,"response":{"replyText":"Sua pista ajuda! Onde podemos procurar?","audioBase64":"","visualReaction":"CURIOUS","nextAction":"SPEAK_AGAIN","degraded":false}}
{"protocolVersion":1,"type":"COMPLETE","serverElapsedMs":1050,"response":{"replyText":"Sua pista ajuda! Onde podemos procurar?","audioBase64":"...","audioMimeType":"audio/ogg","visualReaction":"CURIOUS","nextAction":"SPEAK_AGAIN","degraded":false}}
```

`protocolVersion` permite evoluir o envelope sem confundir clientes antigos. `serverElapsedMs` mede o
tempo monotônico desde o primeiro evento no gateway: `FINAL_TEXT` aproxima conversa + validação e a
diferença até `COMPLETE` aproxima a síntese. Ele não é relógio de parede, identificador nem dado da
criança. O benchmark mede também o tempo observado no Android; comparar os dois separa a parcela de
rede da parcela processada no servidor.

`FINAL_TEXT` só sai depois da resposta completa do modelo e da normalização pedagógica; nunca contém
token parcial ou raciocínio interno. O Android já pode atualizar balão e reação enquanto a síntese
termina. `COMPLETE` entrega áudio ou sinaliza fallback. Falha operacional após o cabeçalho gera
`FALLBACK`. Ao trocar de tela, o cancelamento da coroutine fecha a chamada OkHttp em andamento.
Se `/stream` ainda não existir no servidor e responder `404` ou `405`, o Android usa uma vez o
endpoint JSON compatível, com o mesmo corpo e a mesma `Idempotency-Key`.

## Segurança e degradação

- A resposta é normalizada para no máximo duas frases e uma pergunta orientadora.
- A memória fica em RAM, limitada a seis mensagens; sessões com mais de dez minutos são descartadas
  na próxima atividade do servidor.
- Logs registram duração, turno e fallback; não registram áudio ou transcrição.
- Falha de conversa ou síntese mantém HTTP 200 e marca `degraded=true` com resposta preparada.
- A resposta da LEIA fica até dez minutos em cache e no banco para replay idempotente; áudio e
  transcrição captados da criança não são persistidos.
- Chave reutilizada com outra etapa ou transcrição retorna `409`; duplicata ainda processando retorna `425` com
  `Retry-After`.
- O fingerprint da requisição completa é um HMAC; a transcrição não é persistida em claro. Em
  produção, `IDEMPOTENCY_FINGERPRINT_SECRET` deve ser um segredo estável e exclusivo do ambiente.
- O circuito abre por falha/lentidão e a concorrência remota é limitada a duas chamadas sem fila.
- O Android abandona a espera após seis segundos e usa fala local.
- O servidor pode exigir `X-Device-Token` e limitar oito chamadas por sessão/minuto. Ambos estão
  testados localmente; a aplicação dessas configurações no endpoint Oracle ainda não foi comprovada.

## Exemplo de teste

```bash
curl -sS -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-turn-0001' \
  -H 'X-Device-Token: TOKEN_CONFIGURADO_NO_SERVIDOR' \
  -d '{"sessionId":"demo-001","sceneId":"comic-ball-01","turn":1,"transcript":"Vi uma bola perto da árvore","speaker":"LEIA_FEMALE","reducedStimuli":false}' \
  http://127.0.0.1:8088/api/v1/voice-turn
```

Para observar cada evento assim que ele chega:

```bash
curl -N -H 'Accept: application/x-ndjson' -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-stream-0001' \
  -d '{"sessionId":"demo-001","sceneId":"comic-ball-01","turn":1,"transcript":"Vi uma bola","speaker":"LEIA_FEMALE","reducedStimuli":false}' \
  http://127.0.0.1:8088/api/v1/voice-turn/stream
```
