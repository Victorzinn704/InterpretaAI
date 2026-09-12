package br.gov.interpretaai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import br.gov.interpretaai.AppScreen
import br.gov.interpretaai.AppUiState
import br.gov.interpretaai.AppViewModel
import br.gov.interpretaai.platform.KioskController
import br.gov.interpretaai.ui.screens.ApplyScreen
import br.gov.interpretaai.ui.screens.CameraMissionScreen
import br.gov.interpretaai.ui.screens.CompleteScreen
import br.gov.interpretaai.ui.screens.EducatorScreen
import br.gov.interpretaai.ui.screens.HomeScreen
import br.gov.interpretaai.ui.screens.InterpretScreen
import br.gov.interpretaai.ui.screens.MissionScreen
import br.gov.interpretaai.ui.screens.PuzzleScreen
import br.gov.interpretaai.ui.screens.TalkScreen
import br.gov.interpretaai.ui.theme.ComicCream

@Composable
fun InterpretaApp(
    state: AppUiState,
    viewModel: AppViewModel,
    speak: (String) -> Unit,
    listen: () -> Unit,
    kiosk: KioskController
) {
    Scaffold(containerColor = ComicCream) { padding ->
        Box(Modifier.fillMaxSize().background(ComicCream).padding(padding)) {
            when (state.screen) {
                AppScreen.HOME -> HomeScreen(
                    onSchool = viewModel::startComic,
                    onEducator = { viewModel.navigate(AppScreen.EDUCATOR) },
                    onSpeak = { speak("Bem-vindo ao Interpreta AI! LEIA: Ler, Escrever, Interpretar e Aplicar. Entre no modo escola para ouvir histórias e brincar com os quadrinhos.") },
                    onFocus = kiosk::startFocusMode
                )
                AppScreen.COMICS -> br.gov.interpretaai.ui.screens.ComicsScreen(
                    speak = speak,
                    onBack = { viewModel.navigate(AppScreen.HOME) },
                    voiceMessage = state.message,
                    onPuzzle = viewModel::startPuzzle,
                    onMission = viewModel::startMission,
                    onSceneAnswered = viewModel::recordComicChoice,
                    onWordBuilt = viewModel::recordComicWord,
                    onCompleted = viewModel::completeComic
                )
                AppScreen.PUZZLE -> PuzzleScreen(
                    speak = speak,
                    onBack = { viewModel.navigate(AppScreen.COMICS) },
                    onHelp = viewModel::recordPuzzleHelp,
                    onCompleted = viewModel::completePuzzle
                )
                AppScreen.MISSION -> MissionScreen(
                    state = state,
                    onBack = { viewModel.navigate(AppScreen.HOME) },
                    onSpeak = { speak("Encontre e diga o nome de alguma coisa que comece com o som da letra M.") },
                    onListen = listen,
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
                    onSpeak = { speak("Escolha como responder. Você pode tirar foto, falar com a voz ou desenhar na tela.") },
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
                    onSpeak = { speak("Aponte a câmera somente para um objeto que comece com M e toque no botão amarelo.") }
                )
                AppScreen.TALK -> TalkScreen(
                    onBack = { viewModel.navigate(AppScreen.APPLY) },
                    onSpeak = { speak("Agora deixe o aparelho na mesa e conte ao colega qual palavra com M você descobriu.") },
                    onComplete = viewModel::completeMission
                )
                AppScreen.COMPLETE -> CompleteScreen(
                    onSpeak = { speak("Parabéns! Você completou a missão da letrinha M!") },
                    onHome = { viewModel.navigate(AppScreen.HOME) }
                )
                AppScreen.EDUCATOR -> EducatorScreen(
                    metrics = state.metrics,
                    isDeviceOwner = kiosk.isDeviceOwner,
                    hasDndAccess = kiosk.canControlDoNotDisturb,
                    onBack = { viewModel.navigate(AppScreen.HOME) },
                    onRequestDnd = kiosk::requestDoNotDisturbAccess,
                    onStartFocus = kiosk::startFocusMode,
                    onStopFocus = kiosk::stopFocusMode,
                    onClearMetrics = viewModel::clearMetrics
                )
            }
        }
    }
}
