package br.gov.interpretaai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import br.gov.interpretaai.domain.EventType
import br.gov.interpretaai.domain.LearningEvent
import br.gov.interpretaai.domain.MetricsSnapshot
import br.gov.interpretaai.domain.MissionEvaluator
import br.gov.interpretaai.domain.ResponseModality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class AppScreen { HOME, COMICS, PUZZLE, MISSION, INTERPRET, APPLY, CAMERA, TALK, COMPLETE, EDUCATOR }

data class AppUiState(
    val screen: AppScreen = AppScreen.HOME,
    val isListening: Boolean = false,
    val spokenAnswer: String = "",
    val answerCorrect: Boolean? = null,
    val selectedPlace: String? = null,
    val selectedModality: ResponseModality? = null,
    val message: String? = null,
    val metrics: MetricsSnapshot = MetricsSnapshot()
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as InterpretaAiApplication).metricsRepository
    private val _state = MutableStateFlow(AppUiState(metrics = repository.snapshot()))
    val state: StateFlow<AppUiState> = _state
    private var responseStartedAt = 0L

    fun navigate(screen: AppScreen) {
        _state.update { it.copy(screen = screen, message = null) }
    }

    fun startMission() {
        repository.record(LearningEvent(EventType.SESSION_STARTED))
        responseStartedAt = System.currentTimeMillis()
        _state.update {
            it.copy(
                screen = AppScreen.MISSION,
                spokenAnswer = "",
                answerCorrect = null,
                metrics = repository.snapshot()
            )
        }
    }

    fun startComic() {
        repository.record(LearningEvent(EventType.SESSION_STARTED, activity = COMIC_ACTIVITY))
        _state.update { it.copy(screen = AppScreen.COMICS, message = null, metrics = repository.snapshot()) }
    }

    fun recordComicChoice(sceneIndex: Int, choiceIndex: Int) {
        repository.record(
            LearningEvent(
                type = EventType.OBSERVATION_RECORDED,
                activity = COMIC_ACTIVITY,
                value = "scene_${sceneIndex + 1}:choice_${choiceIndex + 1}",
                modality = ResponseModality.TOUCH
            )
        )
        _state.update { it.copy(metrics = repository.snapshot()) }
    }

    fun recordComicWord() {
        repository.record(
            LearningEvent(
                type = EventType.STAGE_COMPLETED,
                activity = COMIC_ACTIVITY,
                value = "escrever-bola",
                success = true,
                modality = ResponseModality.TOUCH
            )
        )
        _state.update { it.copy(metrics = repository.snapshot()) }
    }

    fun completeComic(path: String) {
        repository.record(
            LearningEvent(
                type = EventType.SESSION_COMPLETED,
                activity = COMIC_ACTIVITY,
                value = path,
                success = true,
                modality = ResponseModality.TOUCH
            )
        )
        _state.update { it.copy(metrics = repository.snapshot()) }
    }

    fun startPuzzle() {
        repository.record(LearningEvent(EventType.SESSION_STARTED, activity = PUZZLE_ACTIVITY))
        _state.update { it.copy(screen = AppScreen.PUZZLE, message = null, metrics = repository.snapshot()) }
    }

    fun recordPuzzleHelp() {
        repository.record(LearningEvent(EventType.HELP_REQUESTED, activity = PUZZLE_ACTIVITY, value = "visual-hint"))
        _state.update { it.copy(metrics = repository.snapshot()) }
    }

    fun completePuzzle(subject: String, level: String, moves: Int, durationMs: Long) {
        repository.record(
            LearningEvent(
                type = EventType.SESSION_COMPLETED,
                activity = PUZZLE_ACTIVITY,
                value = "$subject:$level:$moves-movimentos",
                durationMs = durationMs,
                success = true,
                modality = ResponseModality.TOUCH
            )
        )
        _state.update { it.copy(metrics = repository.snapshot()) }
    }

    fun setListening(listening: Boolean) = _state.update { it.copy(isListening = listening) }

    fun voiceAnswer(text: String) {
        val correct = MissionEvaluator.startsWithLetterM(text)
        repository.record(
            LearningEvent(
                type = EventType.RESPONSE_SUBMITTED,
                value = if (correct) "starts_with_target_phoneme" else "target_not_detected",
                durationMs = (System.currentTimeMillis() - responseStartedAt).coerceAtLeast(0),
                success = correct,
                modality = ResponseModality.VOICE
            )
        )
        if (correct) repository.record(LearningEvent(EventType.STAGE_COMPLETED, value = "fonema-m", success = true))
        _state.update {
            it.copy(
                spokenAnswer = text,
                answerCorrect = correct,
                message = if (correct) "Muito bem! Você encontrou um som de M." else "Quase! Tente uma palavra que comece com M.",
                metrics = repository.snapshot()
            )
        }
    }

    fun speechError(message: String) = _state.update { it.copy(isListening = false, message = message) }

    fun choosePlace(place: String) {
        val correct = place == "Mercado"
        repository.record(
            LearningEvent(
                type = EventType.RESPONSE_SUBMITTED,
                value = place,
                success = correct,
                modality = ResponseModality.TOUCH
            )
        )
        if (correct) repository.record(LearningEvent(EventType.STAGE_COMPLETED, value = "interpretacao", success = true))
        _state.update { it.copy(selectedPlace = place, metrics = repository.snapshot()) }
    }

    fun chooseModality(modality: ResponseModality) {
        _state.update { it.copy(selectedModality = modality) }
    }

    fun helpRequested() {
        repository.record(LearningEvent(EventType.HELP_REQUESTED))
        _state.update { it.copy(message = "Ajuda enviada ao professor.", metrics = repository.snapshot()) }
    }

    fun cameraCaptured() {
        repository.record(
            LearningEvent(
                type = EventType.STAGE_COMPLETED,
                value = "registro-camera-local",
                success = true,
                modality = ResponseModality.CAMERA
            )
        )
        _state.update { it.copy(screen = AppScreen.TALK, metrics = repository.snapshot()) }
    }

    fun completeMission() {
        repository.record(LearningEvent(EventType.SESSION_COMPLETED, success = true))
        _state.update { it.copy(screen = AppScreen.COMPLETE, metrics = repository.snapshot()) }
    }

    fun clearMetrics() {
        repository.clear()
        _state.update { it.copy(metrics = repository.snapshot()) }
    }

    private companion object {
        const val COMIC_ACTIVITY = "gibi-bola-amigos"
        const val PUZZLE_ACTIVITY = "quebra-cabeca-palavras"
    }
}
