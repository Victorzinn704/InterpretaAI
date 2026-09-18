# Contratos HTTP da versão 2.0

## Convenções

- Base `/api/v2`, JSON UTF-8 e datas ISO-8601 UTC.
- Autenticação adulta OIDC; dispositivo usa credencial própria, revogável e com escopo mínimo.
- Mutações adultas de autoria/distribuição aceitam `Idempotency-Key`; a confirmação de preparo
  do aparelho é idempotente pela dupla atribuição/aparelho e pelo hash do pacote. Respostas
  incluem `X-Correlation-Id`.
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

O [Estúdio web](STUDIO_REVIEW.md) usa um BFF separado em `/studio/api/**`: login OIDC com sessão e
CSRF, sem bearer token no JavaScript. `GET /studio/api/me` devolve escolas ativas; as rotas de
revisão/listagem/mídia e as mutações de aprovação/publicação repetem as regras da API v2 no servidor.
Após publicação, ele lista somente turmas acessíveis em `GET /studio/api/schools/{schoolId}/classrooms`,
mostra atribuições em `GET /studio/api/schools/{schoolId}/stories/{storyId}/versions/{version}/assignments`
e recebe `POST` nessa última rota com `classroomId` e `availableFrom`. O servidor constrói o alvo
`CLASSROOM`, revalida o vínculo, exige chave idempotente e não aceita pedido de IA como autoridade
para distribuir conteúdo. A resposta indica atribuição registrada, **não cache preparado**.
Esse BFF está desligado por padrão e não altera a autenticação do APK infantil nem do tablet.

A professora pode retirar uma atribuição ativa em
`POST /studio/api/schools/{schoolId}/assignments/{assignmentId}/revoke`, com sessão, CSRF,
`Idempotency-Key` e confirmação explícita na interface. A API adulta correspondente é
`POST /api/v2/assignments/{assignmentId}/revoke` com JWT e `X-School-Id`. O servidor revalida
o vínculo com a turma, marca a atribuição como `REVOKED` e audita somente a primeira transição.
Repetições são idempotentes; não há reativação implícita.

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
durabilidade da fila. A segunda fila recupera apenas orientações aprovadas e, quando existem,
chama o planejador LangChain4j/Ollama para gravar um plano docente estruturado. A saída é validada
e recebe SHA-256; não é um pacote infantil e não chega ao tablet. Sem fonte aprovada, o estado vira
`NEEDS_TEACHER_INPUT` sem chamada ao modelo. Ambos os workers são opt-in e o catálogo distribuído
ainda não tem fontes aprovadas.

`GET /api/v2/authoring/jobs/{jobId}/plan` devolve o rascunho somente à autora ou à coordenação
autorizada da mesma escola, com ETag/`If-None-Match`. Antes de o plano existir, retorna
`409 authoring_plan_not_ready`. Codex, geração de mídia, validação do pacote e revisão visual
continuam fora deste trecho implementado.

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
GET  /stories/{storyId}/versions/{version}/review
GET  /stories/{storyId}/versions/{version}/review/assets/{assetId}/{role}
POST /stories/{storyId}/versions/{version}/approve
POST /stories/{storyId}/versions/{version}/publish
```

A revisão devolve `packJson`, `packSha256`, `revision` e URLs privadas de cada variante de mídia,
somente à autora ou à coordenação/administração da mesma escola. A resposta não inclui caminho
interno do objeto e usa `Cache-Control: no-store`. O Estúdio deve exibir a história e as imagens
antes de habilitar o botão de aprovação. Cada URL de imagem repete a autorização adulta.

O corpo de aprovação exige a revisão e o hash exatos vistos pela professora:

```json
{
  "expectedRevision": 1,
  "expectedPackSha256": "<sha256_do_rascunho>",
  "confirmedAssets": [
    { "assetId": "maca_objeto", "role": "PHONE", "sha256": "<sha256_da_variante>" }
  ],
  "confirmedWarningIds": []
}
```

O conjunto de `confirmedAssets` deve corresponder exatamente a cada variante vinculada e seu
SHA-256 (incluindo PHONE e TABLET, quando ambos existirem). Hash do rascunho ou
revisão obsoletos falham. O servidor cria, na mesma transação de aprovação, um novo snapshot
validado com `approvedBy`, `approvedAt` e `reviewedByTeacher=true`; a partir daí JSON e hash não
mudam. Publicação e atribuição à turma são atos posteriores. Nem job, modelo nem Codex chamam
essas rotas com credenciais próprias.

No incremento local, as rotas de mutação exigem OIDC, `X-School-Id` e `Idempotency-Key`, aceitam
somente a autora de um rascunho (ou coordenação/administração na mesma escola), gravam auditoria e
congelam o snapshot após aprovação. A entrada é interna: o servidor valida estrutura,
grafo, apoios, acessibilidade, procedência e linguagem do rascunho antes de o worker
materializar uma versão revisável; o esquema `learning-story-pack.schema.json` corresponde ao
snapshot entregável, não ao rascunho. O worker real RAG/Codex ainda não produz essa saída. A atribuição
à turma, o manifesto privado e a rota de mídia já existem localmente, mas “publicada” ainda não
significa “pronta no tablet”: o JSON e as variantes selecionadas precisam passar pelas verificações
do cache do próprio aparelho.

## Fluxo do aparelho

### Manifesto incremental

`GET /api/v2/devices/{deviceId}/manifest?after={cursor}` retorna atribuições compatíveis, versão,
hash e bytes. Se nada mudou, retorna página vazia com o mesmo cursor. No servidor local, o APK monta
deterministicamente `GET /api/v2/devices/{deviceId}/assignments/{assignmentId}/pack` na mesma origem
da credencial e o chama com a credencial do aparelho. O cliente não segue `downloadUrl` fornecida por
manifesto. O pacote devolvido usa `ETag` igual ao SHA-256 anunciado e `Cache-Control: no-store`.
URLs assinadas de objetos continuam como evolução do adaptador privado. A rota local de recursos
abaixo não torna a história pronta até que o Android baixe e valide a variante adequada ao viewport.

### Recurso privado da história

`GET /api/v2/devices/{deviceId}/assignments/{assignmentId}/assets/{assetId}/{role}` entrega somente
uma variante declarada em `story_version_asset` para a atribuição ativa do próprio aparelho. O
servidor guarda o vínculo imutável com a derivação sanitizada e compara tipo, tamanho e SHA-256 antes
da versão poder ser criada. A resposta devolve o tipo de mídia, `Content-Length`, `ETag` SHA-256 e
`Cache-Control: no-store`; não expõe `object_key`, `mediaId` ou caminho do armazenamento. O downloader
Android baixa uma variante por vez, confere tipo, tamanho, ETag e SHA-256, grava por troca atômica e
só avança o cursor do manifesto quando todos os recursos selecionados da página foram aceitos. Room
vincula a atribuição ao aparelho e à expiração; a Home abre apenas pacote `FULLY_CACHED` no renderer
infantil. O percurso foi testado com fixture local em emulador, mas publicação → download → execução
contra um servidor real e aparelhos escolares ainda não foram validados.

Antes de anunciar um item ou devolver seus bytes, o servidor recalcula o SHA-256 do JSON persistido.
Uma divergência suspende aquela entrega com estado técnico recuperável; ela não se transforma em
atividade parcial no tablet.

### Confirmação de preparo do cache

Após verificar o JSON e todos os arquivos selecionados para o viewport, o Android envia, com a
credencial do próprio aparelho, `POST /api/v2/devices/{deviceId}/assignments/{assignmentId}/prepared`:

```json
{ "packSha256": "<sha256_do_pacote_publicado>" }
```

O servidor confere aparelho ativo, escola/turma, atribuição ativa, compatibilidade e hash publicado.
Repetir a confirmação do mesmo pacote atualiza `lastConfirmedAt`; hash divergente é recusado. O
cursor do manifesto só avança após a confirmação, e falha temporária é repetida pelo WorkManager.
Uma sincronização posterior reafirma pacotes ainda íntegros, inclusive quando o manifesto vem vazio.

`GET /api/v2/assignments/{assignmentId}/preparation` exige JWT adulto e `X-School-Id`; o Estúdio
usa `GET /studio/api/schools/{schoolId}/assignments/{assignmentId}/preparation` via sessão. Ambos
devolvem `pairedCompatibleDevices`, `recentlyConfirmedDevices`, `lastConfirmationAt`, `observedAt`
e `freshnessHours` (24). O painel separa aparelhos pareados de confirmações recentes; sem contato
recente, mostra falta de confirmação, não abandono infantil. O recibo é declaração autenticada do
aplicativo após checagem local, **não prova criptográfica de que o arquivo permaneça no aparelho**,
nem comprova que a criança executou a atividade. Após a retirada, o servidor deixa de entregar
pacote e aceitar recibos. Na próxima sincronização online, o `404` da reconfirmação remove o
vínculo local e encerra sua sessão persistida; a Home não oferece mais a história. Os bytes
imutáveis podem permanecer no cache físico porque outra atribuição pode compartilhá-los. Não há
push nem garantia de revogação enquanto o aparelho estiver offline; uma cena já aberta em memória
também não é interrompida instantaneamente. Esses limites exigem teste de campo antes do piloto.

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
