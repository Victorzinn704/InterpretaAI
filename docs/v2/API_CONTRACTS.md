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

## Pareamento do tablet

Uma professora vinculada à turma solicita `POST /device-management/pairing-codes`. O servidor
devolve um código de oito caracteres, válido por dez minutos e exibido uma vez. Apenas o HMAC do
código fica no banco. O tablet envia código, identificador aleatório da instalação e capacidades a
`POST /device-pairings/redeem`; essa é a única rota v2 pública e possui limite de tentativas.

O resgate consome o código atomicamente e devolve `deviceToken` uma única vez. O banco guarda apenas
o HMAC da credencial de 256 bits. Em `/devices/{deviceId}/...`, o token só autentica o próprio ID;
trocar o ID da URL falha sem revelar a existência do outro aparelho. Revogar pelo Estúdio invalida
a próxima requisição. Código inválido, expirado e já usado compartilham a mesma resposta segura.

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

O Estúdio envia os bytes com `PUT {uploadUrl}`, novamente autenticado e com `X-School-Id`. No
incremento local, a API grava em diretório privado por chave gerada no servidor, confere tamanho,
tipo declarado e SHA-256 e devolve `UPLOADED`. Esse estado significa **bruto recebido**, não
imagem segura/aprovada. Decodificação, remoção de metadados, regravação e derivados pertencem ao
worker e precisam levar o item a um estado posterior antes da autoria.

O upload confirmado cria um trabalho persistente de sanitização. O worker lê dimensões antes da
decodificação completa, confere o formato real, rejeita animação e limites excedidos e regrava uma
imagem estática em PNG. A regravação remove metadados do original. Somente o objeto derivado com
estado `READY` poderá ser usado pela futura autoria; `UPLOADED` nunca é sinônimo de aprovado.

Repetir a criação com a mesma chave e metadados devolve a mesma sessão. Repetir o `PUT` com os
mesmos bytes devolve o mesmo recibo; tentar trocar conteúdo ou metadados retorna conflito. Nem o
nome do arquivo nem os bytes entram na trilha de auditoria.

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

Retorna `202 Accepted`, `jobId`, estado `QUEUED` e `Location` com a URL de acompanhamento. A
requisição não contém prompt livre de sistema, provedor, ferramenta ou credencial.

Na fundação local, criação, item de fila e evento de auditoria são confirmados na mesma transação.
A repetição da mesma chave e conteúdo devolve o mesmo `jobId`; reutilizar a chave para outro pedido
retorna conflito. Upload privado só entra na fila para sua própria autora quando o derivado
sanitizado está `READY`; compartilhamento futuro ocorrerá pela biblioteca aprovada. O worker toma
um lease persistente; se cair, outro processo pode retomar o mesmo job após a expiração. Isso prova
durabilidade da fila, não significa que as etapas de RAG, modelos e Codex já estejam conectadas.
O worker local validado hoje encerra apenas a preparação determinística em
`RETRIEVING_GUIDANCE`; a etapa seguinte permanece explicitamente pendente de integração.

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

No incremento local, essas duas rotas já exigem OIDC, `X-School-Id` e `Idempotency-Key`, aceitam
somente a autora de um rascunho (ou coordenação/administração na mesma escola), gravam auditoria e
nunca modificam o JSON ou o SHA-256 da versão. A entrada é interna: o servidor valida estrutura,
grafo, apoios, acessibilidade, procedência e linguagem do `LearningStoryPack` antes de o worker
materializar um rascunho revisável. O worker real RAG/Codex ainda não produz essa saída. A atribuição
a turma e o manifesto privado também continuam pendentes; portanto, “publicada” ainda não significa
“entregue ao tablet”.

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

O `ETag` muda com a revisão persistida do job. Uma consulta com `If-None-Match` igual recebe `304`,
evitando transferir repetidamente o mesmo estado para o Estúdio.

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
