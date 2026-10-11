# Primeira contribuição: encontre o caminho no código

Este roteiro ajuda a localizar uma mudança pequena no InterpretaAI. Comece pelo
[produto](../README.md), pelo [índice por público](README.md) e pelo
[fluxo de contribuição](../CONTRIBUTING.md). Consulte a documentação específica da área antes
de alterar comportamento; este mapa não substitui os contratos nem o estado do MVP.

## Uma ação da tela até o estado

As telas recebem callbacks. Em [InterpretaApp.kt](../app/src/main/java/br/gov/interpretaai/ui/InterpretaApp.kt),
por exemplo, uma ação de ajuda é ligada a `onHelp = viewModel::helpRequested`:

```text
Botão da UI
   ↓
callback
   ↓
AppViewModel.helpRequested()
   ↓
registra evento
   ↓
atualiza AppUiState
   ↓
Compose recompõe a tela
```

Ao adicionar uma ação nova, siga `tela → callback → AppViewModel → domain/data/platform → novo estado`.
Evite colocar persistência, regras pedagógicas complexas ou chamadas de infraestrutura diretamente
dentro de uma `@Composable`. Na jornada de pacotes preparados, procure também `AppScreen.STORY_PACK`,
`StoryPackScreen` e `recordPreparedStoryHelp`; ela possui callbacks próprios.

## Onde colocar cada tipo de alteração

Os caminhos Android abaixo começam em `app/src/main/java/br/gov/interpretaai/`.
Os caminhos Java começam em `server/src/main/java/br/gov/interpretaai/server/`.

| Quero alterar… | Procure primeiro |
|---|---|
| Tela/visual infantil | [ui/screens/](../app/src/main/java/br/gov/interpretaai/ui/screens/) |
| Componentes Compose reutilizáveis | [ui/](../app/src/main/java/br/gov/interpretaai/ui/) |
| Tema | [ui/theme/](../app/src/main/java/br/gov/interpretaai/ui/theme/) |
| Navegação e estado da aplicação | [AppViewModel.kt](../app/src/main/java/br/gov/interpretaai/AppViewModel.kt) e [InterpretaApp.kt](../app/src/main/java/br/gov/interpretaai/ui/InterpretaApp.kt) |
| Regra pedagógica pura | [domain/](../app/src/main/java/br/gov/interpretaai/domain/) |
| Métricas | [LearningModels.kt](../app/src/main/java/br/gov/interpretaai/domain/LearningModels.kt) e [LocalMetricsRepository.kt](../app/src/main/java/br/gov/interpretaai/data/LocalMetricsRepository.kt) |
| Voz Android | [VoiceAssistant.kt](../app/src/main/java/br/gov/interpretaai/platform/VoiceAssistant.kt) |
| Fallback offline | [OfflineLeiaMediator.kt](../app/src/main/java/br/gov/interpretaai/platform/OfflineLeiaMediator.kt) |
| Pacote de história e execução no APK | [LearningStoryPack.kt](../app/src/main/java/br/gov/interpretaai/domain/LearningStoryPack.kt) e [StoryPackScreen.kt](../app/src/main/java/br/gov/interpretaai/ui/screens/StoryPackScreen.kt) |
| Interface web da professora | [recursos do Studio](../server/src/main/resources/static/studio/) e [studio/](../server/src/main/java/br/gov/interpretaai/server/studio/) |
| API do servidor | [api/](../server/src/main/java/br/gov/interpretaai/server/api/) e o serviço de domínio chamado pelo controller |
| Preparação de rascunhos | [authoring/](../server/src/main/java/br/gov/interpretaai/server/authoring/) |
| Validação, aprovação e publicação de histórias | [story/](../server/src/main/java/br/gov/interpretaai/server/story/) |
| Atribuições e entrega ao aparelho | [delivery/](../server/src/main/java/br/gov/interpretaai/server/delivery/) |
| Identidade adulta e autorização | [identity/](../server/src/main/java/br/gov/interpretaai/server/identity/) |
| Credencial e pareamento do tablet | [device/](../server/src/main/java/br/gov/interpretaai/server/device/) |
| Turma e sessão temporária | [classroom/](../server/src/main/java/br/gov/interpretaai/server/classroom/) |
| Integração com provedor | [provider/](../server/src/main/java/br/gov/interpretaai/server/provider/) |
| Banco do servidor | [migrações SQL](../server/src/main/resources/db/migration/) e [migrações Java](../server/src/main/java/db/migration/) |
| Teste de regra Android | [app/src/test/](../app/src/test/) |
| Teste de UI Android | [app/src/androidTest/](../app/src/androidTest/) |
| Teste de servidor | [server/src/test/](../server/src/test/) |

## Roteiro para entender uma funcionalidade

Se você acabou de entrar no projeto, não tente ler todos os arquivos. Escolha um comportamento e
siga esta sequência:

1. Procure o texto exibido na tela e descubra qual `Screen` o mostra.
2. Descubra quais callbacks a `Screen` recebe e qual método do ViewModel é chamado.
3. Descubra quais classes o método utiliza e o que altera no estado.
4. Procure o teste da funcionalidade.
5. Consulte a documentação relacionada e siga o resultado até a interface.

No Android, a leitura inicial é
[MainActivity](../app/src/main/java/br/gov/interpretaai/MainActivity.kt) →
[AppViewModel](../app/src/main/java/br/gov/interpretaai/AppViewModel.kt) →
[InterpretaApp](../app/src/main/java/br/gov/interpretaai/ui/InterpretaApp.kt) → uma tela → regra ou
serviço utilizado → testes. Ao investigar uma função, pergunte: quem chama, o que altera e para
onde o resultado vai?

Para conteúdo preparado, leia o [fluxo de integração de dados](v2/DATA_INTEGRATION_FLOW.md):
autoria → revisão adulta → publicação da versão imutável → atribuição → manifesto do tablet →
cache verificado → execução. “Publicada”, “enviada” e “pronta no aparelho” têm confirmações
distintas. Consulte também o [contrato de histórias](v2/contracts/learning-story-pack.schema.json)
e o [modelo de autorização](v2/AUTHORIZATION_MODEL.md) ao atravessar essas fronteiras.

O Estúdio web autenticado é o Espaço da Professora. O APK executa a jornada infantil e contém uma
configuração adulta protegida do dispositivo; veja [os dois espaços](TEACHER_STUDENT_SPACES.md).
Não use a antiga tela do educador como ponto de entrada para uma nova funcionalidade do Estúdio.

## Antes de abrir o PR

- Confira se a mudança pertence à área escolhida e se já existe uma implementação equivalente.
- Preserve o funcionamento offline e registre participação sem nota, ranking ou diagnóstico.
- Leia os testes próximos à regra alterada e execute as verificações proporcionais à mudança em
  [Qualidade de engenharia](QUALITY_ENGINEERING.md).
- Para documentação, confira os caminhos e exemplos contra o código, sem fixar uma versão de APK
  ou uma última migração que ficará desatualizada na próxima entrega.
- Revise o diff, descreva a validação realmente executada e siga issue, branch e PR definidos em
  [CONTRIBUTING.md](../CONTRIBUTING.md).

## Origem deste roteiro

Adaptado do guia técnico de [@kayofabio no PR #1](https://github.com/Victorzinn704/InterpretaAI/pull/1),
commit [`cb68993`](https://github.com/Victorzinn704/InterpretaAI/commit/cb68993be2b996f838ddc14f8b962eee6b91811f),
especialmente as seções 7, 29, 34 e 36. Foram preservados o percurso por callbacks, parte do mapa
de alterações e o roteiro de investigação; o mapa foi atualizado para incluir pacotes de histórias,
Estúdio, autoria, entrega, identidade e migrações Java. O PR original permaneceu fechado sem merge;
este documento é uma adaptação posterior e não indica revisão desta versão pelo autor original.
