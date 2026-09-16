# Contrato de sincronização pedagógica

## Implementado no servidor: atribuição anônima de piloto

O professor pode publicar uma das seis missões fechadas para um `deviceId` anônimo. O tablet
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

## Futuro: envio de eventos pedagógicos

Este contrato ainda não está implementado. Ele registra sinais para apoiar a observação docente sem
transportar áudio, imagem ou transcrição livre. Endpoint proposto: `POST /v1/learning-events:batch`.

```json
{
  "deviceId": "tablet-escola-001",
  "events": [
    {
      "eventId": "01J...",
      "schemaVersion": 1,
      "occurredAt": "2026-09-12T18:30:00Z",
      "schoolId": "esc-001",
      "classroomId": "turma-1a",
      "studentAlias": "aluno-7f2a",
      "activityId": "missao-letra-m",
      "type": "RESPONSE_SUBMITTED",
      "modality": "VOICE",
      "durationMs": 4200,
      "observationCategory": "TARGET_PHONEME_NOTICED"
    }
  ]
}
```

Regras:

- `eventId` é idempotente;
- `studentAlias` não contém nome, matrícula ou data de nascimento;
- `observationCategory` descreve o sinal observado e não representa nota ou diagnóstico;
- servidor rejeita campos desconhecidos contendo mídia ou texto livre;
- retenção de evento detalhado é curta; agregados têm prazo definido pelo controlador;
- professor enxerga suas turmas; direção sua escola; secretaria apenas sua rede;
- toda leitura administrativa sensível gera log de auditoria.
- escolhas de interpretação usam `OBSERVATION_RECORDED`, sem classificação binária de certo/errado;
- quebra-cabeças enviam apenas figura, grade, movimentos, duração, ajuda e conclusão.

Resposta:

```json
{
  "accepted": ["01J..."],
  "rejected": []
}
```
