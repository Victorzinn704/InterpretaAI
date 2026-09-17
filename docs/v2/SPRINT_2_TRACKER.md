# Sprint 2 — motor de histórias e cache Android

## Estado geral

`EM EXECUÇÃO`. Este documento separa o núcleo local já verificado da sincronização e da
renderização variável que ainda dependem das rotas de entrega v2.

| Entrega | Estado | Evidência | Próximo portão |
|---|---|---|---|
| Parser local de `LearningStoryPack` | IMPLEMENTADO LOCALMENTE | Kotlin bloqueia schema, app incompatível, mídia ausente, ciclo, apoio regressivo, palavra impossível, linguagem punitiva e procedência incompleta; exemplo oficial da maçã é aceito em teste | aprovação pedagógica da história e evolução conjunta do schema |
| Fonte local de pacote e catálogo | IMPLEMENTADO LOCALMENTE | Room versionado guarda manifesto imutável, estado, pin e inventário de variantes; schema `app/schemas/.../1.json` está rastreado | teste instrumentado de migração e cota real de armazenamento |
| Recursos privados por hash | IMPLEMENTADO LOCALMENTE | arquivos ficam em `filesDir`, só entram por bytes e SHA-256 exatos, são deduplicados e passam por troca atômica | teste em aparelho com falta de espaço e limpeza de cache |
| Prontidão da jornada | IMPLEMENTADO LOCALMENTE | só libera `READY_TO_START` após verificar todos os recursos da cena inicial; `FULLY_CACHED` espera todos os recursos obrigatórios | integrar a jornada Compose ao pacote preparado |
| Manifesto incremental e URLs curtas | PENDENTE | contrato v2 existe, mas API não expõe atribuições/recursos publicados ainda | implementar delivery, autorização do aparelho e URLs privadas |
| WorkManager/retry de rede | PENDENTE POR DEPENDÊNCIA | não foi adicionado ao APK sem uma rota de manifesto real | ativar junto do cliente de manifesto, com rede e backoff testados |
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
  --tests br.gov.interpretaai.platform.storycache.StoryPackCachePolicyTest
./gradlew :app:testDebugUnitTest
```

Os testes atuais são unitários. Auditoria no aparelho — 360×640, 412×915 e 800×1280dp, queda de
rede e reinstalação — continua obrigatória antes de afirmar que a jornada variável está entregue.
