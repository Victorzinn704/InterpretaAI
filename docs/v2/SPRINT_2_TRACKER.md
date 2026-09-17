# Sprint 2 — motor de histórias e cache Android

## Estado geral

`EM EXECUÇÃO`. Este documento separa o núcleo local já verificado da sincronização v2 que já
entra no APK e da renderização variável que ainda não foi ligada às telas infantis.

| Entrega | Estado | Evidência | Próximo portão |
|---|---|---|---|
| Parser local de `LearningStoryPack` | IMPLEMENTADO LOCALMENTE | Kotlin bloqueia schema, app incompatível, mídia ausente, ciclo, apoio regressivo, palavra impossível, linguagem punitiva e procedência incompleta; exemplo oficial da maçã é aceito em teste | aprovação pedagógica da história e evolução conjunta do schema |
| Fonte local de pacote e catálogo | IMPLEMENTADO LOCALMENTE | Room versionado guarda manifesto imutável, estado, pin e inventário de variantes; schema `app/schemas/.../1.json` está rastreado | teste instrumentado de migração e cota real de armazenamento |
| Vínculo, rota e preparo de recursos | IMPLEMENTADO LOCALMENTE | a materialização exige derivação sanitizada da mesma escola com tipo/tamanho/SHA-256 idênticos; Android baixa uma variante selecionada por vez, verifica cabeçalhos/bytes/hash e usa troca atômica antes de avançar o cursor | teste em aparelho com falta de espaço, rede fraca e reconexão |
| Prontidão da jornada | IMPLEMENTADO LOCALMENTE | só libera `READY_TO_START` após verificar todos os recursos da cena inicial; `FULLY_CACHED` espera todos os recursos obrigatórios | integrar a jornada Compose ao pacote preparado |
| Manifesto incremental e pacote privado | IMPLEMENTADO LOCALMENTE | uma professora vinculada atribui somente versão `PUBLISHED` à turma; o manifesto v2 filtra escola/turma/versão do app pela credencial do próprio aparelho, pagina por cursor, recalcula o SHA-256 persistido e entrega JSON e variantes pela atribuição autenticada | validar em aparelho real e substituir o armazenamento local pelo adaptador OCI sem alterar o contrato |
| Cliente Android de entrega | IMPLEMENTADO LOCALMENTE | o cliente aceita somente a origem da credencial pareada, ignora `downloadUrl`, baixa JSON e variantes pelas rotas determinísticas, limita 8 MiB por variante/24 MiB por pacote e só avança cursor após cache íntegro | teste em rede real, pareamento na UI adulta e auditoria de armazenamento conforme [ADR 003](adr/003-bound-story-assets.md) |
| WorkManager/retry de rede | IMPLEMENTADO LOCALMENTE | há sincronização imediata única e periódica a cada seis horas, ambas condicionadas à rede; somente falha transitória pede backoff exponencial persistente | validar comportamento de reconexão, bateria e reinicialização em aparelho |
| Renderer `COMIC`, `PUZZLE` e `WORD_BUILDER` por dados | PENDENTE | telas do MVP permanecem fixas e testadas; o parser ainda não altera UI | migrar uma história de ponta a ponta sem perda visual/funcional |

## Invariantes já cobertas localmente

- conteúdo remoto é dado: não executa código, navegação, URL arbitrária ou novo componente;
- um `packId` não pode mudar os bytes e uma versão `storyId + version` não pode apontar para outro
  pacote;
- o tablet não inicia uma cena porque o servidor disse que ela está pronta: ele confere o arquivo
  local e seu hash;
- tablet usa variante `TABLET` quando disponível e cai para `PHONE` sem impedir a atividade;
- conteúdo parcialmente preparado não interrompe a primeira decisão infantil se ela já estiver
  íntegra;
- pacotes incompatíveis ou com narrativa/pista proibida permanecem bloqueados fora da UI infantil.

## Comandos de evidência

```bash
./gradlew :app:testDebugUnitTest \
  --tests br.gov.interpretaai.domain.LearningStoryPackParserTest \
  --tests br.gov.interpretaai.platform.storycache.StoryPackCachePolicyTest \
  --tests br.gov.interpretaai.platform.storycache.StoryPackDeliveryClientTest \
  --tests br.gov.interpretaai.platform.storycache.StoryPackSyncCoordinatorTest
./gradlew :app:testDebugUnitTest
```

Os testes atuais são unitários. Auditoria no aparelho — 360×640, 412×915 e 800×1280dp, queda de
rede e reinstalação — continua obrigatória antes de afirmar que a jornada variável está entregue.
