# Contrato de sincronização pedagógica

## Implementado no servidor: atribuição anônima de piloto

O professor pode publicar uma das oito missões fechadas para um `deviceId` anônimo. O tablet
consulta somente a versão posterior àquela que já possui:

```http
PUT /api/v1/pilot/assignments/tablet-001
X-Teacher-Token: <segredo operacional>
Content-Type: application/json

{"classroomLabel":"Turma 1A","avatarId":"pipa","learnerAlias":"pipa-07","activity":"DRAWING","drawingPrompt":"TREE"}
```

```http
GET /api/v1/pilot/assignments/tablet-001?afterVersion=2
X-Device-Token: <segredo operacional diferente>
```

O primeiro envio recebe versão `1`; cada substituição incrementa a versão. A consulta devolve `204`
quando nada mudou. O JSON aceita apenas turma curta, um dos quatro avatares, atividade e pista de
desenho. `learnerAlias` é um código fechado como `pipa-07`, separado da aparência `avatarId`; assim
duas crianças com o mesmo tipo de avatar não se tornam o mesmo registro. Nome, matrícula, texto
livre e campos desconhecidos são rejeitados. O recurso fica
indisponível enquanto `PILOT_SYNC_ENABLED`, `PILOT_SYNC_TEACHER_TOKEN` e
`PILOT_SYNC_DEVICE_TOKEN` não forem configurados.

Durante atualização gradual, o APK 0.8 anterior ainda é aceito: se não enviar `learnerAlias`, o
servidor devolve `<avatarId>-01`. Esse fallback preserva testes entre versões, mas não deve ser usado
para cadastrar vários participantes, pois produziria colisões.

Este é um canal de uma instância para piloto: os tokens compartilhados reduzem exposição acidental,
mas não substituem login, RBAC, token por dispositivo e auditoria institucional. O Android já permite
configurar o receptor na área adulta, consulta versões novas ao criar o `ViewModel` e oferece busca
manual. O professor pode publicar a seleção para outro `deviceId`; seu token não é persistido. O
token do tablet fica no sandbox privado, excluído de backup e transferência.

Evidência de 15/09/2026: o professor publicou a versão 2 (`Turma 3B`, avatar `estrela`, atividade
`PUZZLE`); o tablet recebeu a versão e a Home passou a mostrar `⭐ Estrela • Turma 3B` e
`Quebra-cabeça`. Depois, a versão 3 (`Turma 5A`, avatar `foguete`, atividade `SOUND_M`) apareceu sem
toque por meio da consulta automática cancelável da Home. O teste usou loopback com `adb reverse`;
não comprova Oracle ou internet pública.

O aplicativo atual grava eventos locais com `learnerAlias`, não com `avatarId`. O código aparece
somente na área adulta; a Home infantil continua mostrando personagem, turma e missão. A associação
entre esse pseudônimo e qualquer identidade institucional permanece fora do MVP até existirem login,
RBAC e trilha de auditoria.

Smoke de migração em 15/09/2026: Flyway aplicou `V1` a `V4` em banco vazio; a publicação nova e a
consulta do tablet preservaram `pipa-07`; uma requisição no formato do APK 0.8 foi aceita e recebeu
`sol-01`. O ensaio foi local e usou apenas dados fictícios.

## Sala e envio em grupo implementados no servidor

O contrato do piloto também permite que o professor monte uma sala pseudonimizada, com no máximo
40 participantes. Cada alias e cada `deviceId` são únicos; duas crianças podem usar o mesmo tipo de
avatar sem compartilhar métricas. Nomes, matrículas e texto livre continuam proibidos.

```http
PUT /api/v1/pilot/classrooms/turma-1a
X-Teacher-Token: <segredo operacional>
Content-Type: application/json

{
  "classroomLabel": "Turma 1A",
  "participants": [
    {"learnerAlias":"pipa-07","avatarId":"pipa","deviceId":"tablet-room-01"},
    {"learnerAlias":"pipa-08","avatarId":"pipa","deviceId":"tablet-room-02"}
  ]
}
```

O professor consulta a composição com `GET /api/v1/pilot/classrooms/{classroomId}`. Para enviar a
mesma missão a toda a sala, publica uma lista vazia de aliases; para uma dupla, grupo ou atendimento
individual, informa somente os aliases desejados:

```http
POST /api/v1/pilot/classrooms/turma-1a/assignments
X-Teacher-Token: <segredo operacional>
Content-Type: application/json

{"learnerAliases":["pipa-08"],"activity":"DRAWING","drawingPrompt":"TREE"}
```

A publicação inteira é transacional e reutiliza a fila versionada que cada tablet já consulta. O
servidor, os testes e o cliente Android estão implementados. Na área adulta, o professor adiciona as
seleções à sala, marca quem participa e envia a missão para todos, dupla, grupo ou indivíduo. A
usabilidade desse editor adulto ainda precisa ser validada com educadores em um tablet real.

## Implementado: envio de eventos pedagógicos e agregados

O Android mantém uma outbox SQLite e envia sinais fechados para apoiar a observação docente sem
transportar áudio, imagem, resposta livre, nome ou matrícula. O lote aceita no máximo 50 eventos e
só é marcado como sincronizado depois de uma resposta HTTP bem-sucedida:
`POST /api/v1/pilot/learning-events:batch`.

```json
{
  "deviceId": "tablet-escola-001",
  "events": [
    {
      "eventId": "evt-01JABCDEFG",
      "occurredAt": "2026-09-12T18:30:00Z",
      "activityId": "missao-letra-m",
      "type": "RESPONSE_SUBMITTED",
      "modality": "VOICE",
      "durationMs": 4200,
      "observationCategory": "ORAL_EXPRESSION"
    }
  ]
}
```

O tablet envia somente `deviceId` e o esquema fechado do evento. O servidor resolve
`classroomId` e `learnerAlias` pelo cadastro prévio do aparelho na sala; esses campos não são
aceitos do cliente, evitando que o tablet escolha outra turma ou participante.

Regras implementadas:

- `eventId` é idempotente e IDs repetidos no mesmo lote são rejeitados;
- o aparelho precisa estar cadastrado em uma sala do piloto;
- eventos podem ter no máximo 30 dias e tolerância de cinco minutos no futuro;
- o JSON rejeita campos desconhecidos, mídia, transcrição e texto livre;
- a sincronização usa HTTPS, salvo loopback de desenvolvimento, timeout de três segundos e nenhum
  retry oculto;
- falha mantém o evento pendente para a próxima inicialização, configuração ou participação;
- `observationCategory` descreve o sinal observado e não representa nota ou diagnóstico;
- escolhas de interpretação usam `OBSERVATION_RECORDED`, sem classificação binária de certo/errado.

Resposta atual:

```json
{"accepted": 1, "duplicates": 0}
```

O professor consulta o agregado de sua sala com token docente:

```http
GET /api/v1/pilot/classrooms/turma-1a/summary
X-Teacher-Token: <segredo operacional>
```

A secretaria consulta o agregado de todas as salas do piloto com uma credencial distinta:

```http
GET /api/v1/pilot/secretariat/summary
X-Secretary-Token: <segredo operacional diferente>
```

Os resumos contêm quantidades de participantes, sessões, participações, etapas concluídas, pedidos
de ajuda, respostas por voz e duração média. Não retornam `deviceId`, alias ou avatar. Cada leitura
docente ou da secretaria grava papel, escopo e horário na trilha de auditoria.

Limites ainda abertos:

- retenção de eventos detalhados ainda precisa de política e limpeza agendada;
- o token é compartilhado por papel no piloto; professor por turma, escola, rede, revogação e login
  institucional ainda não existem;
- há endpoint agregado, mas ainda não há painel web da secretaria;
- o envio é oportunista; WorkManager para retry periódico em segundo plano permanece evolução;
- os indicadores precisam de validação pedagógica antes de orientar decisões públicas.
