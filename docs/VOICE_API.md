# Contrato atual da API de voz

## Endpoint

`POST /api/v1/voice-turn` recebe uma contribuição curta da criança dentro de uma cena fechada e
retorna mediação pedagógica estruturada com áudio. `GET /actuator/health` é o health check.

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

## Segurança e degradação

- A resposta é normalizada para no máximo duas frases e uma pergunta orientadora.
- A memória fica em RAM, limitada a seis mensagens; sessões com mais de dez minutos são descartadas
  na próxima atividade do servidor.
- Logs registram duração, turno e fallback; não registram áudio ou transcrição.
- Falha de conversa ou síntese mantém HTTP 200 e marca `degraded=true` com resposta preparada.
- O Android abandona a espera após seis segundos e usa fala local.
- O endpoint público do MVP é temporário e ainda não possui autenticação ou rate limit.

## Exemplo de teste

```bash
curl -sS -H 'Content-Type: application/json' \
  -d '{"sessionId":"demo-001","sceneId":"comic-ball-01","turn":1,"transcript":"Vi uma bola perto da árvore","speaker":"LEIA_FEMALE","reducedStimuli":false}' \
  http://127.0.0.1:8088/api/v1/voice-turn
```
