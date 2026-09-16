# Relatórios e evidências de aprendizagem

## Objetivo

Dar à professora sinais úteis para intervir e planejar, mantendo separadas as evidências produzidas
no aplicativo, as observações humanas, os estados técnicos e as sugestões de IA.

## Problema do modelo atual

O MVP registra sessão, participação, modalidade, duração, ajuda e conclusão. Esses dados demonstram
uso, mas não carregam história/versão/cena/objetivo/contexto suficientes para uma leitura pedagógica
confiável. Além disso, resolver o participante somente no momento da sincronização pode atribuir um
evento antigo ao vínculo atual do aparelho.

## Contexto imutável da sessão

Ao iniciar uma atividade, o Android cria `LearningSessionContext` e persiste:

| Campo | Uso |
|---|---|
| `sessionId` | liga todos os eventos da jornada |
| `assignmentId` | identifica a atribuição docente |
| `storyId` e `storyVersion` | preserva o conteúdo realmente visto |
| `classroomId` | escopo histórico da turma |
| `participantScope` | `INDIVIDUAL` ou `GROUP` |
| `learnerAlias` | presente somente no escopo individual |
| `groupMemberCount` | presente somente no escopo coletivo |
| `deviceId` | diagnóstico e idempotência, não identidade da criança |
| `startedAt` | ordenação e retenção |

O servidor valida esse contexto no início e devolve um recibo assinado/identificador opaco. Eventos
posteriores referenciam a sessão; uma mudança de turma ou dispositivo não reescreve o passado.

## Evento v2

Campos mínimos:

```text
eventId, sessionId, storyNodeId, interactionId?, objectiveId,
eventType, modality, supportLevel, durationMs?, occurredAt,
clientSequence, appVersion, packVersion
```

`supportLevel` registra `NONE`, `VOICE_REPEAT`, `VISUAL_CUE` ou `CHOICE_REVEALED`. Ele não significa
erro. `clientSequence` ajuda a detectar lacunas sem guardar fala ou resposta livre.

Eventos fechados iniciais:

- `SESSION_STARTED`, `NODE_PRESENTED`, `PROMPT_REPLAYED`;
- `RESPONSE_OFFERED`, `SUPPORT_REQUESTED`, `SUPPORT_PRESENTED`;
- `INTERACTION_COMPLETED`, `NODE_COMPLETED`;
- `SESSION_PAUSED`, `SESSION_RESUMED`, `SESSION_COMPLETED`;
- `HANDOFF_TO_GROUP`;
- `CONTENT_WAITING_FOR_NETWORK`, exclusivamente técnico.

Eventos técnicos ficam em tabela/série separada para não parecerem comportamento infantil.

## Observação docente

`TeacherObservation` contém:

- autora e data;
- turma e, opcionalmente, participante;
- sessão, objetivo ou período relacionado;
- categoria aprovada;
- texto da professora;
- visibilidade (`PRIVATE`, `SCHOOL_TEAM`);
- histórico de edição.

O sistema nunca altera o texto original. Um resumo assistido é armazenado como outro objeto com
estado `SUGGESTED`, `ACCEPTED`, `EDITED` ou `DISMISSED`.

## Camadas do relatório

| Camada | Exemplo | Pode concluir |
|---|---|---|
| Estado técnico | 3 eventos aguardam rede | saúde da sincronização |
| Participação | pediu repetição em 2 de 5 cenas | como participou naquela amostra |
| Evidência contextual | explicou a pista por voz após apoio visual | fato registrado/observado |
| Síntese docente | retomar sequência com imagens em dupla | decisão da professora |
| Sugestão assistida | experimentar narrativa com duas cenas | proposta revisável |

Tempo alto, pausa, ajuda e resposta diferente nunca viram isoladamente “dificuldade”, “abandono” ou
“erro”. O relatório sempre mostra denominador, período e quantidade de sessões.

## Visões

### Aula e turma

- participantes/aparelhos com sessão iniciada;
- progresso por nó da história;
- pedidos e tipos de apoio;
- modalidades usadas;
- atividades coletivas;
- sincronização pendente;
- observações rápidas da professora.

### Criança

- objetivos e histórias vivenciados;
- evidências por data e contexto;
- modalidades e apoios experimentados;
- observações docentes;
- retomadas planejadas e resultados observados posteriormente.

Somente eventos de `participantScope=INDIVIDUAL` entram nessa visão. O pseudônimo do tablet é ligado
à identidade institucional apenas no cofre docente autorizado, fora do app infantil.

### Escola e secretaria

- cobertura por escola/turma e período;
- disponibilidade dos aparelhos e sincronização;
- histórias aplicadas e sessões concluídas;
- uso de modalidades e apoios de forma agregada;
- lacunas de implantação que pedem suporte.

Sem ranking de criança ou professor. Células de grupos pequenos podem ser suprimidas na exportação.

## Assistente de relatórios

O assistente recebe somente agregados autorizados e evidências selecionadas para a pergunta. Ele
devolve JSON estruturado com:

- `summary`;
- `evidenceRefs`;
- `limitations`;
- `suggestedNextSteps`;
- `requiresTeacherReview=true`.

Cada frase factual precisa apontar para uma referência de evidência. O backend rejeita referências
ausentes e linguagem diagnóstica. Nenhum relatório individual entra no RAG de autoria.

## Retenção e auditoria

- eventos: política definida pela rede, com expurgo automatizado;
- mídia infantil bruta: não coletada no relatório;
- observações docentes: retenção institucional e histórico de edição;
- sugestões descartadas: retenção curta para auditoria de qualidade;
- leituras e exportações: auditadas por usuário, escopo, finalidade e horário.

## Critérios de aceite

1. Sincronização atrasada preserva o contexto original da sessão.
2. Evento duplicado não altera agregados.
3. Grupo compartilhado não cria evidência individual falsa.
4. Toda síntese mostra período, amostra, evidências e limitações.
5. Professora consegue corrigir/descartar uma sugestão sem alterar o evento original.
6. Falha de rede aparece como estado técnico, não como comportamento da criança.

