# Sprint 2 — motor de histórias e cache Android

## Estado geral

`EM EXECUÇÃO`. Este documento separa o núcleo local já verificado do fluxo ponta a ponta com
publicação real, pareamento na interface adulta e aparelhos escolares ainda pendentes.

| Entrega | Estado | Evidência | Próximo portão |
|---|---|---|---|
| Parser local de `LearningStoryPack` | IMPLEMENTADO LOCALMENTE | Kotlin bloqueia schema, app incompatível, mídia ausente, ciclo, apoio regressivo, palavra impossível, linguagem punitiva e procedência incompleta; exemplo oficial da maçã é aceito em teste | aprovação pedagógica da história e evolução conjunta do schema |
| Fonte local de pacote, catálogo e sessão | IMPLEMENTADO LOCALMENTE | Room v3 guarda pacote imutável, variantes, atribuição por aparelho/prioridade/expiração e sessão ativa; migração 1→2→3 passou em teste instrumentado | cota real de armazenamento e retenção do cache |
| Vínculo, rota e preparo de recursos | IMPLEMENTADO LOCALMENTE | a materialização exige derivação sanitizada da mesma escola com tipo/tamanho/SHA-256 idênticos; Android baixa uma variante selecionada por vez, verifica cabeçalhos/bytes/hash e usa troca atômica antes de avançar o cursor | teste em aparelho com falta de espaço, rede fraca e reconexão |
| Prontidão da jornada | IMPLEMENTADO LOCALMENTE | a Home só mostra atribuição do aparelho vigente e `FULLY_CACHED`; antes de abrir, revalida arquivos e hashes; sessão fixa pacote/nó, permanece acessível se a atribuição expirar durante o uso e retoma a etapa após recriação | persistir progresso parcial dentro de puzzle/palavra e medir reinício real do processo |
| Manifesto incremental e pacote privado | IMPLEMENTADO LOCALMENTE | uma professora vinculada atribui somente versão `PUBLISHED` à turma; o manifesto v2 filtra escola/turma/versão do app pela credencial do próprio aparelho, pagina por cursor, recalcula o SHA-256 persistido e entrega JSON e variantes pela atribuição autenticada | validar em aparelho real e substituir o armazenamento local pelo adaptador OCI sem alterar o contrato |
| Cliente Android de entrega | IMPLEMENTADO LOCALMENTE | o cliente aceita somente a origem da credencial pareada, ignora `downloadUrl`, baixa JSON e variantes pelas rotas determinísticas, limita 8 MiB por variante/24 MiB por pacote e só avança cursor após cache íntegro | teste em rede real, pareamento na UI adulta e auditoria de armazenamento conforme [ADR 003](adr/003-bound-story-assets.md) |
| WorkManager/retry de rede | IMPLEMENTADO LOCALMENTE | há sincronização imediata única e periódica a cada seis horas, ambas condicionadas à rede; somente falha transitória pede backoff exponencial persistente | validar comportamento de reconexão, bateria e reinicialização em aparelho |
| Renderer `COMIC`, `PUZZLE`, `WORD_BUILDER`, dupla e fim por dados | IMPLEMENTADO LOCALMENTE | a Home abre pacote preparado; o mesmo renderer inicia BOLA e percorre MAÇÃ por quadrinho, puzzle por toque/arraste, letras, conversa em dupla e fim; MAÇÃ passou em 360×640, 412×915 e 800×1280; [capturas](../../output/screenshots/story-pack/) | exercitar publicação/download reais, testar áudio/microfone e revisar com professora/crianças |

## Invariantes já cobertas localmente

- conteúdo remoto é dado: não executa código, navegação, URL arbitrária ou novo componente;
- um `packId` não pode mudar os bytes e uma versão `storyId + version` não pode apontar para outro
  pacote;
- o tablet não inicia uma cena porque o servidor disse que ela está pronta: ele confere o arquivo
  local e seu hash;
- tablet usa variante `TABLET` quando disponível e cai para `PHONE` sem impedir a atividade;
- conteúdo parcialmente preparado fica fora da Home infantil; o início exige todos os recursos
  selecionados do pacote, para que a experiência permaneça inteira offline;
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

Além dos testes unitários, `StoryPackCacheMigrationTest` e `StoryPackJourneyUiTest` passaram no
emulador API 35 em 360×640, 412×915 e 800×1280dp. As capturas foram inspecionadas e corrigidas
para não cortar a ação inferior. Queda de rede, reinstalação, pareamento/publicação reais e teste
em dispositivos escolares continuam obrigatórios antes de afirmar entrega ponta a ponta.
