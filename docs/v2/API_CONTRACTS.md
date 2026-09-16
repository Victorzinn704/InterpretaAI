# Contratos HTTP da versão 2.0

## Convenções

- Base `/api/v2`, JSON UTF-8 e datas ISO-8601 UTC.
- Autenticação adulta OIDC; dispositivo usa credencial própria, revogável e com escopo mínimo.
- Toda mutação aceita `Idempotency-Key`; respostas incluem `X-Correlation-Id`.
- Listas usam cursor opaco. Erros seguem `application/problem+json`.
- IDs e autorizações são resolvidos no servidor; `schoolId` enviado pelo cliente não concede acesso.
- Jobs longos são assíncronos. O navegador consulta estado ou recebe eventos autenticados; ele não
  mantém uma requisição de geração aberta.

## Contexto adulto

`GET /api/v2/identity/me` transforma o `sub` do JWT nos vínculos institucionais ativos consultados
no banco. A resposta contém o identificador interno e as escolas/papéis permitidos; papel, escola e
turma não são confiados a claims livres do navegador. O Estúdio escolhe um dos contextos retornados
e cada operação seguinte volta a conferir ação, escola e eventual vínculo com a turma.

O resource server aceita somente JWT do emissor configurado e destinado à audience da API. A
escolha do provedor permanece externa ao domínio: trocar o OIDC não muda IDs internos, vínculos ou
regras de autorização.

## Fluxo da professora

### 1. Criar upload privado

`POST /api/v2/media/uploads`

```json
{
  "fileName": "maca.jpg",
  "mediaType": "image/jpeg",
  "bytes": 284210,
  "purpose": "STORY_SOURCE"
}
```

A API devolve `mediaId`, URL curta para upload, limites e expiração. Concluir o envio dispara
sanitização; o cliente nunca escolhe o caminho final do objeto.

### 2. Criar trabalho de autoria

`POST /api/v2/authoring/jobs`

```json
{
  "objectiveIds": ["reconhecer_maca", "formar_palavra_maca"],
  "yearRange": "1_YEAR",
  "durationMinutes": 8,
  "participationMode": "PAIR",
  "source": { "type": "TEACHER_UPLOAD", "mediaId": "media_maca_001" },
  "confirmedWord": "MAÇÃ",
  "requestedComponents": ["COMIC", "PUZZLE", "WORD_BUILDER", "GROUP_HANDOFF"]
}
```

Retorna `202 Accepted`, `jobId`, `statusUrl` e estado `QUEUED`. A requisição não contém prompt
livre de sistema, provedor, ferramenta ou credencial.

### 3. Resolver ambiguidade ou pedir ajuste

`POST /api/v2/authoring/jobs/{jobId}/inputs`

```json
{
  "questionId": "objeto_principal",
  "selectedChoice": "MAÇÃ"
}
```

Para revisão, ações fechadas usam `SHORTEN`, `SIMPLIFY_LANGUAGE`, `NEW_HINT`, `ADAPT_TO_PAIR`,
`REGENERATE_IMAGE` ou `REDO_SCENE`. Texto livre é conteúdo pedagógico, não instrução operacional.

### 4. Aprovar e publicar

```text
POST /stories/{storyId}/versions/{version}/approve
POST /stories/{storyId}/versions/{version}/publish
```

Aprovação exige versão/revisão atual e confirma avisos. Publicação falha se houver bloqueio, ativo
sem procedência, incompatibilidade de app ou aprovação ausente. Nem job, modelo nem Codex chamam
essas rotas com credenciais próprias.

## Fluxo do aparelho

### Manifesto incremental

`GET /api/v2/devices/{deviceId}/manifest?after={cursor}` retorna atribuições compatíveis, versão,
hash, bytes, prioridade e URLs assinadas curtas. Se nada mudou, retorna `304` ou página vazia.

### Confirmação de entrega

`POST /api/v2/devices/{deviceId}/delivery-events`

```json
{
  "eventId": "delivery_event_001",
  "assignmentId": "assignment_001",
  "storyVersion": 1,
  "state": "READY",
  "verifiedAssetCount": 3,
  "occurredAt": "2026-09-16T12:30:00Z"
}
```

O servidor só apresenta `PRONTA` após `READY` válido. Ausência de contato vira estado técnico, não
abandono infantil.

### Sessão e eventos

Criar sessão devolve recibo opaco do contexto imutável. `POST /learning-events:batch` aceita até
100 eventos ordenados, deduplica `eventId` e devolve aceitos, duplicados, rejeitados e a maior
sequência confirmada. Um item inválido não descarta silenciosamente os demais.

## Atualização de jobs

No primeiro corte, polling com `ETag` a cada 2–5 segundos é suficiente porque autoria não está no
caminho infantil. Server-Sent Events pode reduzir polling quando houver escala. WebSocket fica
reservado para interação bidirecional real; não é necessário para criar imagens ou pacotes.

## Erro padronizado

```json
{
  "type": "https://interpreta.ai/problems/story-contract-invalid",
  "title": "A história precisa de correção",
  "status": 422,
  "code": "story_contract_invalid",
  "safeMessage": "Revise os itens destacados antes de publicar.",
  "correlationId": "corr_01",
  "issues": [
    { "path": "/nodes/2/nextNodeId", "code": "reference_not_found" }
  ]
}
```

Mensagens de interface são seguras; detalhes de provedor e stack ficam em observabilidade interna
com correlação. `429` informa quando tentar novamente; falha externa pode degradar somente a etapa
afetada e preservar o rascunho.
