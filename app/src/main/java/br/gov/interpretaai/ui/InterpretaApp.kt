package br.gov.interpretaai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import br.gov.interpretaai.AppScreen
import br.gov.interpretaai.AppUiState
import br.gov.interpretaai.AppViewModel
import br.gov.interpretaai.platform.KioskController
import br.gov.interpretaai.platform.SoundCue
import androidx.compose.runtime.CompositionLocalProvider
import br.gov.interpretaai.ui.screens.ApplyScreen
import br.gov.interpretaai.ui.screens.CameraMissionScreen
import br.gov.interpretaai.ui.screens.CompleteScreen
import br.gov.interpretaai.ui.screens.EducatorScreen
import br.gov.interpretaai.ui.screens.DrawingBoardScreen
import br.gov.interpretaai.ui.screens.HomeScreen
import br.gov.interpretaai.ui.screens.InterpretScreen
import br.gov.interpretaai.ui.screens.MissionScreen
import br.gov.interpretaai.ui.screens.PuzzleScreen
import br.gov.interpretaai.ui.screens.TalkScreen
import br.gov.interpretaai.ui.theme.ComicCream
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun InterpretaApp(
    state: AppUiState,
    viewModel: AppViewModel,
    speak: (String) -> Unit,
    playAudio: (ByteArray, String, () -> Unit) -> Unit,
    playSound: (SoundCue) -> Unit,
    listen: ((String) -> Unit) -> Unit,
    kiosk: KioskController
) {
    LaunchedEffect(state.screen, state.syncDeviceId) {
        if (state.screen != AppScreen.HOME || state.syncDeviceId.isBlank()) return@LaunchedEffect
        while (isActive) {
            viewModel.refreshPilotAssignment()
            delay(15_000)
        }
    }
    CompositionLocalProvider(LocalSoundEffect provides playSound) {
    Scaffold(containerColor = ComicCream) { padding ->
        Box(Modifier.fillMaxSize().background(ComicCream).padding(padding)) {
            when (state.screen) {
                AppScreen.HOME -> HomeScreen(
                    onSchool = viewModel::startAssignedActivity,
                    classroomLabel = state.classroomLabel,
                    learners = state.assignedLearners,
                    assignedActivity = state.assignedActivity,
                    onEducator = { viewModel.navigate(AppScreen.EDUCATOR) },
                    onSpeak = { speak("Bem-vindo ao Interpreta AI! LEIA significa Ler, Entender, Interpretar e Aprender. Entre no modo escola para ouvir histórias e ajudar os personagens.") },
                    onFocus = kiosk::startFocusMode
                )
                AppScreen.DRAWING -> DrawingBoardScreen(
                    prompt = state.drawingPrompt,
                    speak = speak,
                    onBack = { viewModel.navigate(AppScreen.HOME) },
                    onComplete = viewModel::completeDrawing
                )
                AppScreen.COMICS -> br.gov.interpretaai.ui.screens.ComicsScreen(
                    assignedActivity = state.assignedActivity,
                    speak = speak,
                    playAudio = playAudio,
                    listen = { sceneId -> listen { text -> viewModel.submitLeiaIdea(sceneId, text) } },
                    isListening = state.isListening,
                    isResponding = state.isLeiaResponding,
                    isSpeaking = state.isSpeaking,
                    leiaReply = state.leiaReply,
                    reducedStimuli = state.reducedStimuli,
                    onBack = { viewModel.navigate(AppScreen.HOME) },
                    voiceMessage = state.message,
                    onPuzzle = viewModel::startPuzzle,
                    onGuidedPuzzle = viewModel::startGuidedBallPuzzle,
                    ballAnswer = state.ballAnswer,
                    ballClueAnswer = state.ballClueAnswer,
                    onBallAnswer = viewModel::chooseBallAnswer,
                    onBallClueAnswer = viewModel::chooseBallClueAnswer,
                    onBallClueOther = viewModel::chooseBallClueOther,
                    onBallJourneyStarted = viewModel::restartBallJourney,
                    onMission = viewModel::startMission,
                    onSceneAnswered = viewModel::recordComicChoice,
                    onWordBuilt = viewModel::recordComicWord,
                    onCompleted = viewModel::completeComic
                )
                AppScreen.PUZZLE -> PuzzleScreen(
                    speak = speak,
                    guided = state.guidedPuzzle,
                    challengeMode = state.challengeMode,
                    listen = listen,
                    voiceBusy = state.isSpeaking,
                    listening = state.isListening,
                    voiceMessage = state.message,
                    reducedStimuli = state.reducedStimuli,
                    onBack = { viewModel.navigate(AppScreen.COMICS) },
                    onHelp = viewModel::recordPuzzleHelp,
                    onCompleted = viewModel::completePuzzle,
                    onApplication = viewModel::recordBallApplication,
                    onGuidedFinished = viewModel::completeGuidedBallLesson
                )
                AppScreen.MISSION -> MissionScreen(
                    state = state,
                    onBack = { viewModel.navigate(AppScreen.HOME) },
                    onSpeak = { speak("Encontre e diga o nome de alguma coisa que comece com o som da letra M.") },
                    onListen = { listen(viewModel::voiceAnswer) },
                    onContinue = { viewModel.navigate(AppScreen.INTERPRET) },
                    onHelp = viewModel::helpRequested
                )
                AppScreen.INTERPRET -> InterpretScreen(
                    state = state,
                    onBack = { viewModel.navigate(AppScreen.MISSION) },
                    onSpeak = { speak("João quer comprar uma maçã fresquinha. Para onde ele deve ir?") },
                    onChoose = viewModel::choosePlace,
                    onContinue = { viewModel.navigate(AppScreen.APPLY) }
                )
                AppScreen.APPLY -> ApplyScreen(
                    state = state,
                    onBack = { viewModel.navigate(AppScreen.INTERPRET) },
                    onSpeak = { speak("Escolha como participar. Você pode usar a câmera ou contar sua descoberta para a dupla.") },
                    onChoose = viewModel::chooseModality,
                    onContinue = {
                        if (state.selectedModality == br.gov.interpretaai.domain.ResponseModality.CAMERA) viewModel.navigate(AppScreen.CAMERA)
                        else viewModel.navigate(AppScreen.TALK)
                    },
                    onHelp = viewModel::helpRequested
                )
                AppScreen.CAMERA -> CameraMissionScreen(
                    onBack = { viewModel.navigate(AppScreen.APPLY) },
                    onCaptured = viewModel::cameraCaptured,
                    speak = speak
                )
                AppScreen.TALK -> TalkScreen(
                    onBack = { viewModel.navigate(AppScreen.APPLY) },
                    onSpeak = { speak("Agora deixe o aparelho na mesa e conte ao colega qual palavra com M você descobriu.") },
                    onComplete = viewModel::completeMission
                )
                AppScreen.COMPLETE -> CompleteScreen(
                    completion = state.assignedActivity.readingPack?.completion,
                    title = when {
                        state.completedDrawing -> "SEU DESENHO GANHOU VIDA!"
                        state.completedBallJourney -> "VOCÊ RESOLVEU O MISTÉRIO!"
                        else -> "VOCÊ AJUDOU A LEIA!"
                    },
                    summary = if (state.completedDrawing) {
                        "Você imaginou, traçou e explicou a sua criação."
                    } else if (state.completedBallJourney) {
                        "Você ouviu, encontrou pistas, explicou e usou a palavra."
                    } else {
                        "Você ouviu, falou, pensou e aplicou."
                    },
                    groupPrompt = if (state.completedDrawing) {
                        "Mostre o desenho e conte como você pensou."
                    } else if (state.completedBallJourney) {
                        "Conte ao colega qual pista ajudou a encontrar a bola."
                    } else {
                        "Conte ao colega qual ideia ajudou a história."
                    },
                    onSpeak = {
                        speak(
                            state.assignedActivity.readingPack?.completion?.spokenCelebration
                                ?: if (state.completedDrawing) {
                                "Que criação legal! Mostre para a turma e conte como você pensou no desenho."
                            } else if (state.completedBallJourney) {
                                "Você resolveu o Mistério da Bola! Encontrou a pista e ajudou Davi a procurar atrás da árvore."
                            } else {
                                "Parabéns! Você completou a missão da letrinha M!"
                            }
                        )
                    },
                    onHome = { viewModel.navigate(AppScreen.HOME) }
                )
                AppScreen.EDUCATOR -> EducatorScreen(
                    metrics = state.metrics,
                    isDeviceOwner = kiosk.isDeviceOwner,
                    hasDndAccess = kiosk.canControlDoNotDisturb,
                    tabletReport = kiosk.tabletCapabilityReport(),
                    onCopyTabletReport = kiosk::copyTabletCapabilityReport,
                    onBack = { viewModel.navigate(AppScreen.HOME) },
                    onRequestDnd = kiosk::requestDoNotDisturbAccess,
                    onStartFocus = kiosk::startFocusMode,
                    onStopFocus = kiosk::stopFocusMode,
                    onClearMetrics = viewModel::clearMetrics,
                    reducedStimuli = state.reducedStimuli,
                    onReducedStimuliChange = viewModel::setReducedStimuli,
                    challengeMode = state.challengeMode,
                    onChallengeModeChange = viewModel::setChallengeMode,
                    drawingPrompt = state.drawingPrompt,
                    classroomLabel = state.classroomLabel,
                    learnerAlias = state.learnerAlias,
                    activeAvatar = state.activeAvatar,
                    assignedActivity = state.assignedActivity,
                    syncDeviceId = state.syncDeviceId,
                    syncStatus = state.syncStatus,
                    roomSyncStatus = state.roomSyncStatus,
                    isSyncing = state.isSyncing,
                    onPublishAssignment = viewModel::publishAssignment,
                    onConfigurePilotReceiver = viewModel::configurePilotReceiver,
                    onRefreshPilotAssignment = viewModel::refreshPilotAssignment,
                    onPublishRemoteAssignment = viewModel::publishRemoteAssignment,
                    onPublishRoomAssignment = viewModel::publishRoomAssignment
                )
            }
        }
    }
    }
}
