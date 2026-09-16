# Guia do Desenvolvedor — InterpretaAI

> Guia técnico introdutório baseado no código-fonte do repositório `InterpretaAI-main`.

---

## 1. Visão geral

O InterpretaAI possui dois módulos Gradle:

```text
InterpretaAI/
├── app/       → aplicativo Android em Kotlin + Jetpack Compose
└── server/    → servidor Java 17 + Spring Boot
```

O módulo Android é o aplicativo usado pela criança e pelo educador. O módulo `server` fornece
serviços opcionais de mediação por voz, IA e sincronização do piloto.

A arquitetura Android documentada no próprio projeto é um **MVVM leve**, com separação entre:

```text
UI
 ↓
AppViewModel
 ↓
Domain / Data / Platform
```

O servidor segue uma organização por API, núcleo de serviços e provedores:

```text
API
 ↓
Core
 ↓
Provider
```

A regra importante para novos desenvolvedores é:

> **A interface não deve conter as regras pedagógicas principais.**
> Quando uma regra puder ser independente de Android/Compose, ela deve preferencialmente ficar
> em `domain/`, onde também pode ser testada com testes unitários.

---

# 2. Estrutura principal do projeto

A estrutura relevante é:

```text
InterpretaAI-main/
│
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/br/gov/interpretaai/
│       │   │   ├── MainActivity.kt
│       │   │   ├── AppViewModel.kt
│       │   │   ├── InterpretaAiApplication.kt
│       │   │   │
│       │   │   ├── data/
│       │   │   ├── domain/
│       │   │   ├── platform/
│       │   │   └── ui/
│       │   │
│       │   └── res/
│       │
│       ├── test/              → testes unitários
│       └── androidTest/       → testes instrumentados/UI
│
├── server/
│   └── src/
│       ├── main/java/br/gov/interpretaai/server/
│       │   ├── api/
│       │   ├── core/
│       │   └── provider/
│       └── test/              → testes do servidor
│
├── docs/                      → documentação do projeto
├── tools/                     → scripts auxiliares
├── dist/                      → artefatos de entrega
├── build.gradle.kts
└── settings.gradle.kts
```

---

# 3. Aplicativo Android

## 3.1 `MainActivity.kt`

Arquivo:

```text
app/src/main/java/br/gov/interpretaai/MainActivity.kt
```

`MainActivity` é o ponto de entrada da aplicação Android.

Ela não implementa as telas diretamente. Sua função principal é **montar os componentes que a
aplicação precisa** e entregá-los para `InterpretaApp`.

O fluxo simplificado é:

```text
Android
  ↓
MainActivity
  ↓
AppViewModel
  ↓
InterpretaApp
  ↓
Screens
```

A `MainActivity` também configura:

- `KioskController`;
- estado do `AppViewModel`;
- `VoiceAssistant`;
- `InteractionSounds`;
- permissão do microfone;
- liberação dos recursos de voz e áudio;
- modo imersivo;
- modo de foco/quiosque.

### Permissão do microfone

O aplicativo declara `RECORD_AUDIO` no `AndroidManifest.xml`.

A `MainActivity` verifica se a permissão já existe:

```kotlin
ContextCompat.checkSelfPermission(...)
```

Caso não exista, solicita a permissão usando:

```kotlin
rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
)
```

Depois da autorização, o `VoiceAssistant` pode iniciar a escuta.

### Ciclo de vida

Em `onResume()` o aplicativo:

```text
entra no modo imersivo
        +
inicia o modo de foco quando aplicável
```

O controle do quiosque está separado em `platform/KioskController.kt`.

---

# 4. Estado e lógica de navegação

## 4.1 `AppViewModel.kt`

Arquivo:

```text
app/src/main/java/br/gov/interpretaai/AppViewModel.kt
```

Este é um dos arquivos mais importantes do aplicativo.

O `AppViewModel` mantém o estado da jornada e recebe ações vindas das telas.

A navegação é representada pelo enum:

```kotlin
enum class AppScreen {
    HOME,
    COMICS,
    PUZZLE,
    DRAWING,
    MINI_GAME,
    MISSION,
    INTERPRET,
    APPLY,
    CAMERA,
    TALK,
    COMPLETE,
    EDUCATOR
}
```

O estado geral é armazenado em `AppUiState`.

A tela atual é apenas uma parte desse estado. Também existem informações como:

- resposta falada;
- estado de escuta;
- estado de fala;
- mensagens;
- atividade atribuída;
- alunos/participantes;
- modalidade escolhida;
- progresso de atividades;
- métricas;
- configuração de estímulos reduzidos;
- modo desafio;
- informações de sincronização.

---

# 5. Como funciona a navegação

A aplicação não utiliza um sistema de rotas tradicional para essas telas.

Em vez disso:

```text
AppViewModel.state.screen
          ↓
InterpretaApp
          ↓
when (state.screen)
          ↓
Screen correspondente
```

Em `InterpretaApp.kt`, existe um `when` que seleciona a tela.

Exemplo conceitual:

```kotlin
when (state.screen) {
    AppScreen.HOME -> HomeScreen(...)
    AppScreen.COMICS -> ComicsScreen(...)
    AppScreen.PUZZLE -> PuzzleScreen(...)
    ...
}
```

Portanto, para entender uma nova tela, procure duas coisas:

1. o arquivo da própria tela em `ui/screens/`;
2. o trecho correspondente em `InterpretaApp.kt`.

---

# 6. Mapa das telas

As telas atualmente conectadas em `InterpretaApp.kt` são:

| Estado | Tela | Função |
|---|---|---|
| `HOME` | `HomeScreen` | Entrada da aplicação |
| `COMICS` | `ComicsScreen` | Jornada dos quadrinhos |
| `PUZZLE` | `PuzzleScreen` | Quebra-cabeça |
| `DRAWING` | `DrawingBoardScreen` | Quadro de desenho |
| `MINI_GAME` | `MiniGameScreen` | Miniatividades |
| `MISSION` | `MissionScreen` | Missão com som/letra |
| `INTERPRET` | `InterpretScreen` | Interpretação |
| `APPLY` | `ApplyScreen` | Escolha da forma de participação |
| `CAMERA` | `CameraMissionScreen` | Atividade com câmera |
| `TALK` | `TalkScreen` | Compartilhamento oral |
| `COMPLETE` | `CompleteScreen` | Encerramento |
| `EDUCATOR` | `EducatorScreen` | Área do educador |

A jornada principal pode ser entendida como:

```text
HOME
 │
 ├── COMICS
 │     └── PUZZLE
 │
 ├── DRAWING
 │
 ├── MINI_GAME
 │
 └── MISSION
        ↓
     INTERPRET
        ↓
      APPLY
       ├── CAMERA
       │     ↓
       │    TALK
       │
       └── TALK
             ↓
          COMPLETE
```

A atribuição de uma atividade também pode direcionar diretamente para diferentes jornadas.

Isso acontece em:

```kotlin
AppViewModel.startAssignedActivity()
```

---

# 7. Como uma ação da tela chega ao ViewModel

As telas recebem callbacks.

Por exemplo, uma tela pode receber:

```kotlin
onHelp = viewModel::helpRequested
```

Quando o usuário toca no botão de ajuda:

```text
Botão da UI
   ↓
callback
   ↓
viewModel.helpRequested()
   ↓
registra evento
   ↓
atualiza AppUiState
   ↓
Compose recompõe a tela
```

Esse padrão aparece em várias telas.

### Regra prática

Ao adicionar uma ação nova:

```text
Tela
 ↓
callback
 ↓
AppViewModel
 ↓
domain/data/platform, quando necessário
 ↓
novo estado
```

Evite colocar persistência, regras pedagógicas complexas ou chamadas de infraestrutura diretamente
dentro de uma `@Composable`.

---

# 8. `ui/`

Diretório:

```text
app/src/main/java/br/gov/interpretaai/ui/
```

Responsável pela apresentação.

Possui:

```text
ui/
├── InterpretaApp.kt
├── ComicComponents.kt
├── ComicPortrait.kt
├── screens/
└── theme/
```

## `ui/screens/`

Contém as telas individuais:

```text
AdvancedReadingMissionStage.kt
ApplyScreen.kt
CameraMissionScreen.kt
ComicsScreen.kt
CompleteScreen.kt
DrawingBoardScreen.kt
EducatorScreen.kt
HomeScreen.kt
InterpretScreen.kt
MiniGameScreen.kt
MissionScreen.kt
PuzzleScreen.kt
TalkScreen.kt
WordBuilding.kt
```

Ao criar ou alterar uma interface, normalmente esse é o primeiro diretório a consultar.

## `ui/theme/`

Contém:

```text
InterpretaTheme.kt
```

Responsável pelo tema visual usado pelo Compose.

---

# 9. `domain/`

Diretório:

```text
app/src/main/java/br/gov/interpretaai/domain/
```

Esta é a parte mais importante para as **regras que não dependem diretamente da interface Android**.

Arquivos atuais:

```text
BallLesson.kt
ClassroomModels.kt
ComicStories.kt
DrawingModels.kt
LearningModels.kt
MiniGameRules.kt
MissionEvaluator.kt
PuzzleGame.kt
ReadingMissionModels.kt
ReengagementPolicy.kt
```

## Exemplos de responsabilidades

### `BallLesson.kt`

Contém regras relacionadas às respostas da jornada da bola.

Existem enums como:

```kotlin
BallAnswer
BallClueAnswer
BallInstruction
PuzzleGuidanceCue
```

e objetos responsáveis por resolver respostas/pistas.

### `PuzzleGame.kt`

Define elementos do quebra-cabeça, como:

```kotlin
PuzzleSubject
PuzzleSize
```

e regras relacionadas ao jogo.

### `MissionEvaluator.kt`

Contém avaliação da resposta da missão.

Por exemplo, o `AppViewModel` utiliza:

```kotlin
MissionEvaluator.startsWithLetterM(text)
```

Assim, a regra de identificação do som/letra não fica implementada diretamente dentro da tela.

### `ReengagementPolicy.kt`

Contém regras para decidir formas de reengajamento:

```kotlin
ReengagementCue
```

### `LearningModels.kt`

Define estruturas relacionadas aos eventos de aprendizagem e métricas:

```text
EventType
ResponseModality
LearningEvent
MetricsSnapshot
```

---

# 10. Por que `domain/` possui tantos testes?

O projeto possui testes unitários específicos para as regras de domínio:

```text
app/src/test/java/br/gov/interpretaai/domain/

BallLessonTest.kt
ClassroomModelsTest.kt
ComicStoriesTest.kt
DrawingHistoryTest.kt
MiniGameRulesTest.kt
MissionEvaluatorTest.kt
PuzzleGameTest.kt
ReengagementPolicyTest.kt
```

Isso é importante porque permite testar regras sem precisar iniciar toda a interface Android.

Exemplo conceitual:

```text
Regra pedagógica
      ↓
domain/
      ↓
teste unitário
      ↓
resultado previsível
```

Ao criar uma regra nova e independente da UI, procure colocar essa regra no domínio e criar o teste
correspondente.

---

# 11. `data/`

Diretório:

```text
app/src/main/java/br/gov/interpretaai/data/
```

Atualmente contém:

```text
LocalMetricsRepository.kt
```

Ele é responsável pela persistência local das métricas/eventos.

O `AppViewModel` utiliza o repositório para registrar eventos como:

```text
SESSION_STARTED
SESSION_COMPLETED
RESPONSE_SUBMITTED
STAGE_COMPLETED
HELP_REQUESTED
OBSERVATION_RECORDED
```

A ideia documentada pelo projeto é manter a aplicação funcional offline e posteriormente sincronizar
os eventos com o servidor.

Fluxo simplificado:

```text
AppViewModel
     ↓
MetricsRepository
     ↓
persistência local
     ↓
sincronização
     ↓
API do piloto
```

---

# 12. `platform/`

Diretório:

```text
app/src/main/java/br/gov/interpretaai/platform/
```

Aqui ficam integrações específicas do Android ou de infraestrutura.

Arquivos:

```text
BootReceiver.kt
InteractionSounds.kt
InterpretaDeviceAdminReceiver.kt
KioskController.kt
LocalVisionRecognizer.kt
OfflineLeiaMediator.kt
PilotAssignmentClient.kt
PilotClassroomClient.kt
PilotLearningClient.kt
TabletCapabilityReport.kt
VoiceAssistant.kt
VoiceTurnClient.kt
```

## Exemplos

### `VoiceAssistant.kt`

Integra recursos de voz do Android.

### `VoiceTurnClient.kt`

Responsável pela comunicação do aplicativo com a API de turnos de voz.

### `OfflineLeiaMediator.kt`

Fornece uma alternativa local para a mediação quando o caminho remoto não está disponível.

Isso permite que o aplicativo tenha comportamento de fallback.

### `LocalVisionRecognizer.kt`

Usa recursos locais de visão/OCR.

### `KioskController.kt`

Controla funcionalidades relacionadas ao modo quiosque/foco do dispositivo.

### `PilotLearningClient.kt`

Comunica eventos de aprendizagem com o servidor do piloto.

### `PilotAssignmentClient.kt`

Consulta/publica atribuições de atividades.

### `PilotClassroomClient.kt`

Trabalha com informações de sala/turma no fluxo de piloto.

---

# 13. Fluxo de voz

O fluxo de voz pode ser visualizado assim:

```text
Criança
  ↓
UI
  ↓
listen(...)
  ↓
VoiceAssistant
  ↓
Android Speech Recognition
  ↓
AppViewModel
  ↓
VoiceTurnClient / OfflineLeiaMediator
  ↓
Servidor, quando necessário
```

A resposta falada retorna para a aplicação e pode ser reproduzida pelo `VoiceAssistant`.

No `MainActivity`, o objeto é criado com callbacks para:

```kotlin
onListeningChanged
onSpeakingChanged
onVoiceUnavailable
```

Isso permite que o estado da voz seja refletido no `AppUiState`.

---

# 14. Fluxo offline

Um princípio importante do projeto é que a navegação pedagógica não deve depender da IA remota.

De forma simplificada:

```text
                    ┌── servidor/IA
                    │
Entrada da criança ─┤
                    │
                    └── fallback local
                              ↓
                       jornada continua
```

A documentação de arquitetura explica que os conceitos esperados e a máquina de estados da jornada
são resolvidos de maneira determinística no Android.

Isso significa que uma alteração em uma regra essencial da jornada deve ser implementada no código
da aplicação, e não presumir que um modelo de IA sempre estará disponível.

---

# 15. Câmera e visão

O aplicativo possui:

```text
CameraMissionScreen.kt
LocalVisionRecognizer.kt
```

O `AndroidManifest.xml` declara:

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

A visão é realizada localmente usando dependências do ML Kit presentes em `app/build.gradle.kts`.

As dependências incluem:

```text
com.google.mlkit:text-recognition
com.google.mlkit:image-labeling
```

A atividade de câmera não deve ser confundida com um serviço de visão remoto. A arquitetura documentada
define o reconhecimento local como parte do MVP.

---

# 16. Métricas

O projeto não usa as métricas como uma nota escolar automática.

O modelo principal é:

```text
LearningEvent
      ↓
LocalMetricsRepository
      ↓
MetricsSnapshot
```

O `AppViewModel` registra eventos em vários momentos da jornada.

Exemplo:

```kotlin
repository.record(
    LearningEvent(
        type = EventType.SESSION_COMPLETED,
        ...
    )
)
```

As métricas devem ser interpretadas como sinais de participação/interação.

Antes de alterar esse sistema, consulte:

```text
docs/DATA_AND_PRIVACY.md
docs/ONLINE_OFFLINE_ARCHITECTURE.md
docs/SYNC_API_PROPOSAL.md
```

---

# 17. Atividades atribuídas

As atividades são representadas em:

```text
domain/ClassroomModels.kt
```

O enum `AssignedActivity` define atividades que podem ser atribuídas.

O método:

```kotlin
AppViewModel.startAssignedActivity()
```

decide qual fluxo iniciar.

Conceitualmente:

```text
Atividade atribuída
       ↓
startAssignedActivity()
       ↓
┌──────┼────────┬───────────┐
↓      ↓        ↓           ↓
Comic Puzzle Drawing   Mini Game / Mission
```

Isso é importante para quem for adicionar uma nova atividade.

Uma nova atividade provavelmente precisará ser refletida em mais de um ponto:

1. modelo da atividade;
2. ViewModel;
3. tela;
4. navegação em `InterpretaApp`;
5. eventos/métricas, se aplicável;
6. testes;
7. documentação.

Não adicione apenas uma tela e considere a funcionalidade completa.

---

# 18. Área do educador

Arquivo:

```text
ui/screens/EducatorScreen.kt
```

A área do educador recebe dados e callbacks do `AppViewModel`.

Entre os recursos conectados estão:

- métricas;
- status do dispositivo;
- relatório de capacidades do tablet;
- modo foco;
- estímulos reduzidos;
- modo desafio;
- atribuição de atividades;
- configuração do receptor do piloto;
- atualização de atribuições;
- publicação de atividades;
- sincronização.

A tela é uma camada de apresentação. As operações são delegadas ao ViewModel e aos componentes
de infraestrutura.

---

# 19. Servidor

O servidor fica em:

```text
server/
```

Tecnologias declaradas no `server/build.gradle.kts`:

```text
Java 17
Spring Boot
Spring Web
Spring Validation
Spring Actuator
JDBC
Flyway
H2
PostgreSQL
LangChain4j
Resilience4j
```

A estrutura é:

```text
server/src/main/java/br/gov/interpretaai/server/
├── api/
├── core/
└── provider/
```

---

# 20. `server/api/`

Contém os controllers HTTP.

Controllers atuais incluem:

```text
AiGatewayController
PilotAssignmentController
PilotClassroomController
PilotLearningController
VoiceTurnController
ApiExceptionHandler
```

Endpoints principais encontrados no código:

```text
POST /api/v1/voice-turn
POST /api/v1/voice-turn/stream

POST /api/v1/pilot/learning-events:batch

GET /api/v1/pilot/classrooms/{classroomId}/summary
GET /api/v1/pilot/secretariat/summary

PUT /api/v1/pilot/assignments/{deviceId}
GET /api/v1/pilot/assignments/{deviceId}

PUT /api/v1/pilot/classrooms/{classroomId}
GET /api/v1/pilot/classrooms/{classroomId}
POST /api/v1/pilot/classrooms/{classroomId}/assignments

POST /api/v1/gateway/warmup
GET /api/v1/gateway/status
```

Para modificar uma API, comece pelo controller correspondente e depois acompanhe o fluxo para
`core/`.

---

# 21. `server/core/`

Contém a lógica central do servidor.

Entre os componentes estão:

```text
AdaptiveConversationRouter
ConversationDeadline
ConversationPromptFactory
ConversationProvider
OperationalEventSink
OperationalOutboxStore
OperationalOutboxWorker
PedagogicalReplyContract
PilotAccess
PilotAssignmentStore
PilotClassroomService
PilotClassroomStore
PilotLearningService
PilotLearningStore
ReplySafety
RoutableConversationProvider
SafeFallbackConversationProvider
ScenePackCatalog
SessionMemory
SilentSpeechProvider
SpeechProvider
SpeechSynthesisService
VoiceTurnIdempotency
VoiceTurnRateLimiter
VoiceTurnService
VoiceTurnStore
WarmableConversationProvider
```

Uma forma simples de pensar:

```text
Controller
   ↓
Service / Core
   ↓
Provider / Store
```

Os controllers não devem concentrar toda a lógica do negócio.

---

# 22. `server/provider/`

Contém integrações com provedores externos ou motores de IA/voz.

Entre eles:

```text
GeminiConversationProvider
GoogleCloudSpeechProvider
KokoroSpeechProvider
NvidiaConversationProvider
NvidiaModelCatalog
OllamaConversationProvider
ProviderWarmupService
```

As interfaces em `core/`, como:

```text
ConversationProvider
SpeechProvider
```

ajudam a manter os provedores substituíveis.

Isso permite, por exemplo, trocar o provedor de conversa sem reescrever toda a jornada Android.

---

# 23. Banco e migrações

O servidor possui migrações Flyway em:

```text
server/src/main/resources/db/migration/
```

Atualmente existem migrações:

```text
V1__voice_turn_idempotency.sql
V2__operational_outbox.sql
V3__pilot_assignment.sql
V4__separate_learner_alias_from_avatar.sql
V5__pilot_classroom.sql
V6__expand_assignment_activity.sql
V7__pilot_learning_events.sql
V8__assignment_members.sql
V9__shared_classroom_tablets.sql
V10__group_event_attribution.sql
```

Quando uma alteração exige mudança persistente de banco, a estratégia do projeto é criar uma nova
migração Flyway em vez de editar uma migração já aplicada.

---

# 24. Testes

O projeto possui uma quantidade significativa de testes.

## Android — testes unitários

Local:

```text
app/src/test/
```

Existem testes para:

```text
domain/
platform/
```

Entre os testes:

```text
BallLessonTest
ClassroomModelsTest
ComicStoriesTest
DrawingHistoryTest
MiniGameRulesTest
MissionEvaluatorTest
PuzzleGameTest
ReengagementPolicyTest

PilotAssignmentClientTest
PilotClassroomClientTest
PilotLearningClientTest
TabletCapabilityReportTest
VoiceTurnClientTest
```

## Android — testes instrumentados/UI

Local:

```text
app/src/androidTest/
```

Existem testes como:

```text
ComicsFlowTest
DrawingBoardUiTest
EducatorWorkflowUiTest
LocalMetricsMigrationTest
MvpCriteriaGuardrailTest
PuzzleFlowTest
ReadingCompletionUiTest
SharedTabletUiTest
TabletDiagnosticUiTest
VisualEvidenceTest
```

## Servidor

Local:

```text
server/src/test/
```

Há testes de:

- controllers;
- segurança;
- rate limiting;
- idempotência;
- memória de sessão;
- contrato pedagógico;
- fallback;
- provedores;
- outbox;
- roteamento adaptativo;
- síntese de voz.

### Regra para novas alterações

Sempre que possível:

```text
alteração de regra
      ↓
teste unitário

alteração de UI/fluxo
      ↓
teste instrumentado quando fizer sentido

alteração de API
      ↓
teste do controller/serviço
```

---

# 25. Como executar o projeto Android

O projeto usa Gradle Wrapper.

No Windows:

```powershell
.\gradlew.bat
```

Para tarefas específicas, use o Gradle Wrapper, por exemplo:

```powershell
.\gradlew.bat :app:test
```

ou a tarefa Android correspondente à configuração que estiver sendo usada.

A configuração Android declarada no projeto inclui:

```text
compileSdk = 35
minSdk = 26
targetSdk = 35
Java = 17
Kotlin JVM target = 17
```

O `applicationId` é:

```text
br.gov.interpretaai
```

A versão encontrada no `app/build.gradle.kts` é:

```text
versionCode = 21
versionName = 0.21.0
```

---

# 26. Como executar o servidor local

O README do servidor documenta o fluxo principal:

```bash
./tools/start-local-mvp.sh
```

Para executar os testes:

```bash
./gradlew :server:test
```

O servidor local é documentado como:

```text
Spring      → 127.0.0.1:8088
Ollama      → 11434
Kokoro      → 8091
```

O projeto utiliza H2 persistente em desenvolvimento por padrão.

Para produção, o servidor pode ser configurado para PostgreSQL através das variáveis documentadas
no projeto.

Consulte:

```text
server/README.md
docs/LOCAL_MVP_SERVER.md
```

antes de alterar a configuração.

---

# 27. Variáveis e configuração

Evite colocar chaves de provedores diretamente no código ou no APK.

O próprio projeto estabelece a separação:

```text
Android
   ↓
API do projeto
   ↓
provedor externo
```

As credenciais ficam no ambiente do servidor.

Para estudar configurações específicas, consulte:

```text
server/src/main/resources/application.yml
server/README.md
docs/VOICE_API.md
docs/LOCAL_MVP_SERVER.md
```

---

# 28. Documentação existente

O projeto já possui documentação extensa.

Antes de escrever uma nova documentação, verifique:

```text
docs/
├── ARCHITECTURE.md
├── MVP_STATUS.md
├── DEMO_RUNBOOK.md
├── PEDAGOGICAL_SCOPE_1_TO_5.md
├── ONLINE_OFFLINE_ARCHITECTURE.md
├── VOICE_API.md
├── DATA_AND_PRIVACY.md
├── KIOSK.md
├── FINAL_MVP_AUDIT.md
├── FLUXO_PEDAGOGICO_FECHADO.md
├── NEW_GAMES_TABLET_MOBILE.md
├── LOCAL_MVP_SERVER.md
├── PILOT_CHECKLIST.md
└── SYNC_API_PROPOSAL.md
```

O arquivo `docs/README.md` funciona como índice da documentação.

Este documento possui um objetivo diferente:

> **não substituir a documentação existente, mas servir como porta de entrada técnica para
> desenvolvedores novos.**

---

# 29. Onde colocar cada tipo de alteração

| Quero alterar... | Procure primeiro |
|---|---|
| Tela/visual | `ui/screens/` |
| Componentes Compose reutilizáveis | `ui/` |
| Tema | `ui/theme/` |
| Navegação | `AppViewModel.kt` + `InterpretaApp.kt` |
| Estado da aplicação | `AppViewModel.kt` |
| Regra pedagógica pura | `domain/` |
| Regras do puzzle | `domain/PuzzleGame.kt` |
| Avaliação da missão | `domain/MissionEvaluator.kt` |
| Métricas | `domain/LearningModels.kt` + `data/LocalMetricsRepository.kt` |
| Voz Android | `platform/VoiceAssistant.kt` |
| Voz/API | `platform/VoiceTurnClient.kt` + `server/api/VoiceTurnController.java` |
| Fallback offline | `platform/OfflineLeiaMediator.kt` |
| Câmera/visão | `CameraMissionScreen.kt` + `platform/LocalVisionRecognizer.kt` |
| Modo quiosque | `platform/KioskController.kt` |
| Atribuição de atividade | `domain/ClassroomModels.kt` + ViewModel + clients |
| API do servidor | `server/api/` |
| Regra do servidor | `server/core/` |
| Integração com provedor | `server/provider/` |
| Banco do servidor | `server/src/main/resources/db/migration/` |
| Teste de regra | `app/src/test/` |
| Teste de UI Android | `app/src/androidTest/` |
| Teste de servidor | `server/src/test/` |
| Documentação técnica | `docs/` |

---

# 30. Exemplo: quero criar uma nova regra pedagógica

Suponha que uma nova atividade precise verificar se uma palavra contém determinado som.

Evite fazer:

```kotlin
@Composable
fun MinhaTela() {
    // toda a regra aqui
}
```

Uma abordagem alinhada ao projeto seria:

```text
1. Criar a regra em domain/
2. Criar testes unitários
3. Usar a regra no AppViewModel
4. Passar o resultado para a UI
5. Criar/alterar a tela
```

Fluxo:

```text
MinhaRegra.kt
     ↓
MinhaRegraTest.kt
     ↓
AppViewModel
     ↓
MinhaScreen.kt
```

Isso mantém a regra independente da interface.

---

# 31. Exemplo: quero criar uma nova tela

A sequência recomendada é:

### 1. Criar a tela

```text
ui/screens/NovaScreen.kt
```

### 2. Adicionar um estado

Em:

```text
AppViewModel.kt
```

por exemplo:

```kotlin
NOVA_TELA
```

dentro de `AppScreen`.

### 3. Adicionar a tela ao `InterpretaApp.kt`

```kotlin
AppScreen.NOVA_TELA -> NovaScreen(...)
```

### 4. Criar os callbacks necessários

Por exemplo:

```kotlin
onContinue
onBack
onHelp
```

### 5. Criar os métodos correspondentes no ViewModel

### 6. Registrar métricas, se a atividade exigir

### 7. Criar testes

### 8. Atualizar a documentação

---

# 32. Exemplo: quero criar uma nova API

No servidor:

```text
Controller
    ↓
Service/Core
    ↓
Store/Provider
```

Por exemplo:

```text
server/api/NovoController.java
          ↓
server/core/NovoService.java
          ↓
Store ou Provider
```

Depois:

```text
server/src/test/
```

deve receber os testes correspondentes.

Se houver alteração de banco:

```text
server/src/main/resources/db/migration/
```

deve receber uma nova migração.

---

# 33. Cuidados importantes

## Não colocar lógica complexa na UI

A UI deve principalmente:

```text
mostrar estado
+
emitir ações
```

## Não depender da IA para controlar a navegação

A jornada principal possui estados determinísticos.

## Não colocar credenciais no Android

Chaves de provedores devem permanecer no servidor/ambiente de execução.

## Não alterar migrações Flyway antigas

Crie uma nova versão.

## Não adicionar dependências sem verificar o projeto

Primeiro confira `app/build.gradle.kts` ou `server/build.gradle.kts`.

## Não esquecer os testes

Uma alteração aparentemente pequena pode afetar regras que já possuem cobertura.

---

# 34. Roteiro para entender o projeto pela primeira vez

Se você acabou de entrar no projeto, não tente ler todos os arquivos.

Siga esta ordem:

```text
1. README.md
       ↓
2. docs/ARCHITECTURE.md
       ↓
3. MainActivity.kt
       ↓
4. AppViewModel.kt
       ↓
5. InterpretaApp.kt
       ↓
6. uma Screen específica
       ↓
7. domain/
       ↓
8. data/
       ↓
9. platform/
       ↓
10. testes correspondentes
```

Depois, se precisar entender o servidor:

```text
server/README.md
       ↓
api/
       ↓
core/
       ↓
provider/
       ↓
server/src/test/
```

---

# 35. Exemplo de leitura de uma funcionalidade

Para entender a missão de som da letra M, por exemplo:

```text
MissionScreen.kt
      ↓
AppViewModel
      ↓
voiceAnswer(...)
      ↓
MissionEvaluator.startsWithLetterM(...)
      ↓
LearningEvent
      ↓
LocalMetricsRepository
      ↓
AppUiState
      ↓
UI atualizada
```

Esse tipo de rastreamento é uma das formas mais úteis de estudar o código.

Quando não entender uma função, pergunte:

> **Quem chama essa função?**

Depois:

> **O que essa função altera?**

E finalmente:

> **Para onde esse resultado vai?**

---

# 36. Como investigar uma funcionalidade existente

Use esta sequência:

```text
1. Procure o texto exibido na tela.
2. Descubra qual Screen o mostra.
3. Descubra quais callbacks a Screen recebe.
4. Descubra qual método do ViewModel é chamado.
5. Descubra quais classes o método utiliza.
6. Procure o teste da funcionalidade.
7. Consulte a documentação relacionada.
```

Por exemplo, se você encontrar:

```kotlin
viewModel::completePuzzle
```

procure:

```text
completePuzzle(
```

Depois veja:

```text
quem chama
o que registra
o que altera no estado
qual teste cobre a regra
```

---

# 37. Checklist para uma contribuição

Antes de abrir um Pull Request ou entregar uma alteração:

```text
[ ] Entendi onde a alteração pertence
[ ] Não dupliquei uma funcionalidade existente
[ ] Mantive as regras fora da UI quando possível
[ ] Atualizei o estado/navegação quando necessário
[ ] Adicionei ou atualizei testes
[ ] Verifiquei impacto nas métricas
[ ] Verifiquei impacto offline/online
[ ] Atualizei a documentação relacionada
[ ] Executei os testes relevantes
[ ] Revisei o diff antes de entregar
```

---

# 38. Resumo da arquitetura

Se precisar lembrar apenas de uma coisa:

```text
                    ┌──────────────────────┐
                    │    MainActivity      │
                    │ montagem Android     │
                    └──────────┬───────────┘
                               ↓
                    ┌──────────────────────┐
                    │    AppViewModel      │
                    │ estado + ações       │
                    └──────────┬───────────┘
                               ↓
                    ┌──────────────────────┐
                    │    InterpretaApp     │
                    │ seleção das telas    │
                    └──────────┬───────────┘
                               ↓
             ┌─────────────────┼─────────────────┐
             ↓                 ↓                 ↓
          Screens           Domain             Data
             │                 │                 │
             │                 │                 ↓
             │                 │          métricas locais
             │                 │
             │                 ↓
             │              regras
             │
             └─────────────────┬─────────────────┘
                               ↓
                           Platform
                    voz / câmera / kiosk /
                    sincronização / clientes
                               │
                               ↓
                            Server
                               │
                   ┌───────────┼───────────┐
                   ↓           ↓           ↓
                  API         Core       Provider
```

A ideia central é:

> **UI mostra e envia ações → ViewModel coordena → Domain decide regras → Data persiste →
> Platform integra recursos externos/Android → Server atende os serviços remotos.**

---

## 39. Referências internas

Para aprofundar cada área, consulte:

- `docs/ARCHITECTURE.md` — arquitetura geral e decisões técnicas.
- `docs/ONLINE_OFFLINE_ARCHITECTURE.md` — funcionamento online/offline.
- `docs/VOICE_API.md` — contrato da API de voz.
- `docs/DATA_AND_PRIVACY.md` — dados e privacidade.
- `docs/KIOSK.md` — modo quiosque.
- `docs/SYNC_API_PROPOSAL.md` — sincronização do piloto.
- `docs/LOCAL_MVP_SERVER.md` — servidor local.
- `docs/FLUXO_PEDAGOGICO_FECHADO.md` — fluxo pedagógico.
- `docs/MVP_STATUS.md` — estado do MVP.
- `docs/README.md` — índice completo da documentação.
- `server/README.md` — execução e configuração do servidor.
