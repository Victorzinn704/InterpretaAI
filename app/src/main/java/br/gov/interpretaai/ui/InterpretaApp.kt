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
import br.gov.interpretaai.ui.screens.MiniGameScreen
import br.gov.interpretaai.ui.screens.PuzzleScreen
import br.gov.interpretaai.ui.screens.TalkScreen
import br.gov.interpretaai.ui.screens.StoryPackScreen
import br.gov.interpretaai.ui.theme.ComicCream
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import br.gov.interpretaai.domain.CollaborativeMoment
import br.gov.interpretaai.domain.CollaborativeTurnPlanner

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
    fun groupSpoken(base: String, moment: CollaborativeMoment): String {
        val turn = CollaborativeTurnPlanner.turn(state.assignedLearners, moment)
        return turn?.let { "$base ${it.spokenPrompt}" } ?: base
    }
    LaunchedEffect(state.screen, state.syncDeviceId) {
        if (state.screen != AppScreen.HOME || state.syncDeviceId.isBlank()) return@LaunchedEffect
        while (isActive) {
            viewModel.refreshPilotAssignment()
            viewModel.refreshPreparedStory()
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
                    readyStoryTitle = state.availableStory?.title,
                    onEducator = { viewModel.navigate(AppScreen.EDUCATOR) },
                    onSpeak = { speak("Bem-vindo ao Interpreta AI! LEIA significa Ler, Entender, Interpretar e Aprender. Entre no modo escola para ouvir histórias e ajudar os personagens.") },
                    onFocus = kiosk::startFocusMode
                )
                AppScreen.DRAWING -> DrawingBoardScreen(
                    prompt = state.drawingPrompt,
                    learners = state.assignedLearners,
                    speak = speak,
                    onBack = { viewModel.navigate(AppScreen.HOME) },
                    onComplete = viewModel::completeDrawing
                )
                AppScreen.MINI_GAME -> MiniGameScreen(
                    activity = state.assignedActivity,
                    speak = speak,
                    voiceBusy = state.isSpeaking,
                    reducedStimuli = state.reducedStimuli,
                    onBack = { viewModel.navigate(AppScreen.HOME) },
                    onHelp = viewModel::recordMiniGameHelp,
                    onComplete = viewModel::completeMiniGame
                )
                AppScreen.COMICS -> br.gov.interpretaai.ui.screens.ComicsScreen(
                    assignedActivity = state.assignedActivity,
                    learners = state.assignedLearners,
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
                    learners = state.assignedLearners,
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
                    learners = state.assignedLearners,
                    onBack = { viewModel.navigate(AppScreen.HOME) },
                    onSpeak = { speak(groupSpoken(
                        "Encontre e diga o nome de alguma coisa que comece com o som da letra M.",
                        CollaborativeMoment.RESPOND
                    )) },
                    onListen = { listen(viewModel::voiceAnswer) },
                    onContinue = { viewModel.navigate(AppScreen.INTERPRET) },
                    onHelp = viewModel::helpRequested
                )
                AppScreen.INTERPRET -> InterpretScreen(
                    state = state,
                    learners = state.assignedLearners,
                    onBack = { viewModel.navigate(AppScreen.MISSION) },
                    onSpeak = { speak(groupSpoken(
                        "João quer comprar uma maçã fresquinha. Para onde ele deve ir?",
                        CollaborativeMoment.RESPOND
                    )) },
                    onChoose = viewModel::choosePlace,
                    onContinue = { viewModel.navigate(AppScreen.APPLY) }
                )
                AppScreen.APPLY -> ApplyScreen(
                    state = state,
                    learners = state.assignedLearners,
                    onBack = { viewModel.navigate(AppScreen.INTERPRET) },
                    onSpeak = { speak(groupSpoken(
                        "Escolha como participar. Você pode usar a câmera ou contar sua descoberta para a dupla.",
                        CollaborativeMoment.OBSERVE
                    )) },
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
                    speak = speak,
                    learners = state.assignedLearners
                )
                AppScreen.TALK -> TalkScreen(
                    onBack = { viewModel.navigate(AppScreen.APPLY) },
                    learners = state.assignedLearners,
                    onSpeak = { speak(groupSpoken(
                        "Agora deixe o aparelho na mesa e conte ao colega qual palavra com M você descobriu.",
                        CollaborativeMoment.SHARE
                    )) },
                    onComplete = viewModel::completeMission
                )
                AppScreen.COMPLETE -> CompleteScreen(
                    completion = state.assignedActivity.readingPack?.completion,
                    learners = state.assignedLearners,
                    title = when {
                        state.completedMiniGame -> "VOCÊ AJUDOU A LEIA!"
                        state.completedDrawing -> "SEU DESENHO GANHOU VIDA!"
                        state.completedBallJourney -> "VOCÊ RESOLVEU O MISTÉRIO!"
                        else -> "VOCÊ AJUDOU A LEIA!"
                    },
                    summary = if (state.completedMiniGame) {
                        when (state.assignedActivity) {
                            br.gov.interpretaai.domain.AssignedActivity.NUMBER_PATH -> "Você encontrou a ordem dos números e explicou o caminho."
                            br.gov.interpretaai.domain.AssignedActivity.CONNECT_DOTS -> "Você ligou os pontos e descobriu uma casa."
                            else -> "Você observou a imagem, organizou letras e formou BOLA."
                        }
                    } else if (state.completedDrawing) {
                        "Você imaginou, traçou e explicou a sua criação."
                    } else if (state.completedBallJourney) {
                        "Você ouviu, encontrou pistas, explicou e usou a palavra."
                    } else {
                        "Você ouviu, falou, pensou e aplicou."
                    },
                    groupPrompt = if (state.completedMiniGame) {
                        "Agora o tablet descansa. Conte ao colega como você descobriu."
                    } else if (state.completedDrawing) {
                        "Mostre o desenho e conte como você pensou."
                    } else if (state.completedBallJourney) {
                        "Conte ao colega qual pista ajudou a encontrar a bola."
                    } else {
                        "Conte ao colega qual ideia ajudou a história."
                    },
                    onSpeak = {
                        val base = state.assignedActivity.readingPack?.completion?.spokenCelebration
                                ?: if (state.completedMiniGame) {
                                "Você ajudou a LEIA! Agora conte ao colega como descobriu."
                            } else if (state.completedDrawing) {
                                "Que criação legal! Mostre para a turma e conte como você pensou no desenho."
                            } else if (state.completedBallJourney) {
                                "Você resolveu o Mistério da Bola! Encontrou a pista e ajudou Davi a procurar atrás da árvore."
                            } else {
                                "Parabéns! Você completou a missão da letrinha M!"
                            }
                        speak(groupSpoken(base, CollaborativeMoment.SHARE))
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
                AppScreen.STORY_PACK -> state.preparedStory?.let { story ->
                    StoryPackScreen(
                        story = story,
                        speak = speak,
                        listen = listen,
                        isListening = state.isListening,
                        isSpeaking = state.isSpeaking,
                        reducedStimuli = state.reducedStimuli,
                        onBack = { viewModel.navigate(AppScreen.HOME) },
                        onHelpRequested = viewModel::recordPreparedStoryHelp,
                        onVoiceContribution = viewModel::recordPreparedStoryVoice,
                        onStageCompleted = viewModel::recordPreparedStoryStage,
                        onCompleted = viewModel::completePreparedStory
                    )
                }
            }
        }
    }
    }
}
