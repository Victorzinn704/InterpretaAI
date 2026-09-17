# Sprint 2 — motor de histórias e cache Android

## Estado geral

`EM EXECUÇÃO`. Este documento separa o núcleo local já verificado da sincronização v2 que já
entra no APK e da renderização variável que ainda não foi ligada às telas infantis.

| Entrega | Estado | Evidência | Próximo portão |
|---|---|---|---|
| Parser local de `LearningStoryPack` | IMPLEMENTADO LOCALMENTE | Kotlin bloqueia schema, app incompatível, mídia ausente, ciclo, apoio regressivo, palavra impossível, linguagem punitiva e procedência incompleta; exemplo oficial da maçã é aceito em teste | aprovação pedagógica da história e evolução conjunta do schema |
| Fonte local de pacote e catálogo | IMPLEMENTADO LOCALMENTE | Room versionado guarda manifesto imutável, estado, pin e inventário de variantes; schema `app/schemas/.../1.json` está rastreado | teste instrumentado de migração e cota real de armazenamento |
| Vínculo e rota privada de recursos | IMPLEMENTADO LOCALMENTE | a materialização exige uma derivação sanitizada da mesma escola que coincida com tipo/tamanho/SHA-256; `story_version_asset` congela esse vínculo e a rota v2 filtra dispositivo/atribuição/variante antes de abrir o objeto | downloader Android de variantes, falta de espaço e teste em aparelho |
| Prontidão da jornada | IMPLEMENTADO LOCALMENTE | só libera `READY_TO_START` após verificar todos os recursos da cena inicial; `FULLY_CACHED` espera todos os recursos obrigatórios | integrar a jornada Compose ao pacote preparado |
| Manifesto incremental e pacote privado | IMPLEMENTADO LOCALMENTE | uma professora vinculada atribui somente versão `PUBLISHED` à turma; o manifesto v2 filtra escola/turma/versão do app pela credencial do próprio aparelho, pagina por cursor, recalcula o SHA-256 persistido e entrega o JSON exato por rota autenticada com ETag | entregar variantes de mídia por adaptador privado e validar em aparelho real |
| Cliente Android do manifesto | IMPLEMENTADO LOCALMENTE | o cliente aceita somente a origem da credencial pareada, ignora `downloadUrl` do manifesto, baixa pela rota autenticada determinística e confere tamanho, ETag e SHA-256 antes de passar o JSON ao parser/cache | teste em rede real, pareamento na UI adulta e download de variantes conforme [ADR 003](adr/003-bound-story-assets.md) |
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
  --tests br.gov.interpretaai.platform.storycache.StoryPackDeliveryClientTest
./gradlew :app:testDebugUnitTest
```

Os testes atuais são unitários. Auditoria no aparelho — 360×640, 412×915 e 800×1280dp, queda de
rede e reinstalação — continua obrigatória antes de afirmar que a jornada variável está entregue.
