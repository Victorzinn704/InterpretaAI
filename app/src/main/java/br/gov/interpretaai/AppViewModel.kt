package br.gov.interpretaai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import br.gov.interpretaai.domain.EventType
import br.gov.interpretaai.domain.LearningEvent
import br.gov.interpretaai.domain.MetricsSnapshot
import br.gov.interpretaai.domain.MissionEvaluator
import br.gov.interpretaai.domain.ResponseModality
import br.gov.interpretaai.domain.BallAnswer
import br.gov.interpretaai.domain.BallAnswerResolver
import br.gov.interpretaai.domain.BallClueAnswer
import br.gov.interpretaai.domain.BallClueAnswerResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import br.gov.interpretaai.platform.VoiceTurnClient
import br.gov.interpretaai.platform.VoiceTurnResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen { HOME, COMICS, PUZZLE, MISSION, INTERPRET, APPLY, CAMERA, TALK, COMPLETE, EDUCATOR }

data class AppUiState(
    val screen: AppScreen = AppScreen.HOME,
    val isListening: Boolean = false,
    val spokenAnswer: String = "",
    val answerCorrect: Boolean? = null,
    val selectedPlace: String? = null,
    val selectedModality: ResponseModality? = null,
    val message: String? = null,
    val leiaReply: VoiceTurnResult? = null,
    val isLeiaResponding: Boolean = false,
    val isSpeaking: Boolean = false,
    val ballAnswer: BallAnswer? = null,
    val ballClueAnswer: BallClueAnswer? = null,
    val guidedPuzzle: Boolean = false,
    val completedBallJourney: Boolean = false,
    val reducedStimuli: Boolean = false,
    val challengeMode: Boolean = false,
    val metrics: MetricsSnapshot = MetricsSnapshot()
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as InterpretaAiApplication).metricsRepository
    private val preferences = application.getSharedPreferences("educator_settings", 0)
    private val voiceTurns = VoiceTurnClient()
    private val _state = MutableStateFlow(AppUiState(
        metrics = repository.snapshot(),
        reducedStimuli = preferences.getBoolean("reduced_stimuli", false),
        challengeMode = preferences.getBoolean("challenge_mode", false)
    ))
    val state: StateFlow<AppUiState> = _state
    private var responseStartedAt = 0L
    private var voiceSessionId = UUID.randomUUID().toString()
    private var voiceTurn = 0
    private var voiceTurnJob: Job? = null

    fun navigate(screen: AppScreen) {
        if (screen != AppScreen.COMICS) voiceTurnJob?.cancel()
        _state.update { it.copy(screen = screen, message = null, isLeiaResponding = false) }
    }

    fun startMission() {
        repository.record(LearningEvent(EventType.SESSION_STARTED))
        responseStartedAt = System.currentTimeMillis()
        _state.update {
            it.copy(
                screen = AppScreen.MISSION,
                spokenAnswer = "",
                answerCorrect = null,
                completedBallJourney = false,
                metrics = repository.snapshot()
            )
        }
    }

    fun startComic() {
        voiceTurnJob?.cancel()
        repository.record(LearningEvent(EventType.SESSION_STARTED, activity = COMIC_ACTIVITY))
        viewModelScope.launch(Dispatchers.IO) { voiceTurns.warmup() }
        voiceSessionId = UUID.randomUUID().toString()
        voiceTurn = 0
        _state.update { it.copy(
            screen = AppScreen.COMICS,
            message = null,
            leiaReply = null,
            ballAnswer = null,
            ballClueAnswer = null,
            guidedPuzzle = false,
            completedBallJourney = false,
            metrics = repository.snapshot()
        ) }
    }

    fun restartBallJourney() {
        voiceTurnJob?.cancel()
        voiceSessionId = UUID.randomUUID().toString()
        voiceTurn = 0
        _state.update { it.copy(
            spokenAnswer = "",
            leiaReply = null,
            ballAnswer = null,
            ballClueAnswer = null,
            message = null
        ) }
    }

    fun submitLeiaIdea(sceneId: String, text: String) {
        repository.record(
            LearningEvent(
                type = EventType.RESPONSE_SUBMITTED,
                activity = COMIC_ACTIVITY,
                value = "oral-contribution",
                modality = ResponseModality.VOICE
            )
        )
        if (sceneId == BALL_SCENE) {
            val answer = BallAnswerResolver.resolve(text)
            val reply = when (answer) {
                BallAnswer.BALL -> "Isso! Você percebeu que falta a bola. Agora vamos investigar onde ela pode estar."
                BallAnswer.OTHER -> "Eu ouvi a sua ideia. Escute o que Lia quer usar para brincar e tente mais uma vez."
                BallAnswer.EMPTY -> "Ainda não consegui ouvir. Você pode falar novamente ou tocar na figura da bola."
            }
            _state.update { it.copy(
                isListening = false,
                isLeiaResponding = false,
                spokenAnswer = text,
                ballAnswer = answer,
                leiaReply = VoiceTurnResult(replyText = reply),
                metrics = repository.snapshot()
            ) }
            return
        }
        if (sceneId == BALL_CLUE_SCENE) {
            val answer = BallClueAnswerResolver.resolve(text)
            val reply = when (answer) {
                BallClueAnswer.TREE -> "Boa investigação! Uma parte da bola aparece perto do tronco. Vamos procurar atrás da árvore."
                BallClueAnswer.OTHER -> "Sua ideia pode ser investigada. Observe a parte da bola que aparece perto do tronco e tente outra vez."
                BallClueAnswer.EMPTY -> "Ainda não consegui ouvir. Você pode falar novamente ou tocar na pista da árvore."
            }
            _state.update { it.copy(
                isListening = false,
                isLeiaResponding = false,
                spokenAnswer = text,
                ballClueAnswer = answer,
                leiaReply = VoiceTurnResult(replyText = reply),
                metrics = repository.snapshot()
            ) }
            return
        }
        voiceTurn = (voiceTurn + 1).coerceAtMost(3)
        val requestedSession = voiceSessionId
        val requestedTurn = voiceTurn
        val requestedReducedStimuli = _state.value.reducedStimuli
        voiceTurnJob?.cancel()
        _state.update { it.copy(isListening = false, isLeiaResponding = true, spokenAnswer = text, leiaReply = null) }
        voiceTurnJob = viewModelScope.launch(Dispatchers.IO) {
            val response = voiceTurns.send(
                requestedSession, sceneId, requestedTurn, text, requestedReducedStimuli
            )
            if (requestedSession == voiceSessionId && _state.value.screen == AppScreen.COMICS) {
                _state.update { it.copy(isLeiaResponding = false, leiaReply = response) }
            }
        }
    }

    fun chooseBallAnswer() {
        repository.record(
            LearningEvent(
                type = EventType.RESPONSE_SUBMITTED,
                activity = COMIC_ACTIVITY,
                value = "picture-contribution",
                modality = ResponseModality.TOUCH
            )
        )
        _state.update { it.copy(
            ballAnswer = BallAnswer.BALL,
            leiaReply = VoiceTurnResult(
                replyText = "Isso! Você percebeu que falta a bola. Agora vamos investigar onde ela pode estar.",
                degraded = false
            ),
            metrics = repository.snapshot()
        ) }
    }

    fun chooseBallClueAnswer() {
        repository.record(
            LearningEvent(
                type = EventType.RESPONSE_SUBMITTED,
                activity = COMIC_ACTIVITY,
                value = "tree-clue-contribution",
                modality = ResponseModality.TOUCH
            )
        )
        _state.update { it.copy(
            ballClueAnswer = BallClueAnswer.TREE,
            leiaReply = VoiceTurnResult(
                replyText = "Boa investigação! Uma parte da bola aparece perto do tronco. Vamos procurar atrás da árvore."
            ),
            metrics = repository.snapshot()
        ) }
    }

    fun chooseBallClueOther() {
        repository.record(
            LearningEvent(
                type = EventType.RESPONSE_SUBMITTED,
                activity = COMIC_ACTIVITY,
                value = "other-clue-contribution",
                modality = ResponseModality.TOUCH
            )
        )
        _state.update { it.copy(
            ballClueAnswer = BallClueAnswer.OTHER,
            leiaReply = VoiceTurnResult(
                replyText = "Essa é uma possibilidade. Compare a mochila com as marcas da imagem e investigue outra vez."
            ),
            metrics = repository.snapshot()
        ) }
    }

    fun startGuidedBallPuzzle() {
        repository.record(LearningEvent(EventType.STAGE_COMPLETED, activity = COMIC_ACTIVITY, value = "objeto-bola"))
        repository.record(LearningEvent(EventType.SESSION_STARTED, activity = PUZZLE_ACTIVITY))
        _state.update { it.copy(
            screen = AppScreen.PUZZLE,
            guidedPuzzle = true,
            message = null,
            metrics = repository.snapshot()
        ) }
    }

    fun setReducedStimuli(enabled: Boolean) {
        preferences.edit().putBoolean("reduced_stimuli", enabled).apply()
        _state.update { it.copy(reducedStimuli = enabled) }
    }

    fun setChallengeMode(enabled: Boolean) {
        preferences.edit().putBoolean("challenge_mode", enabled).apply()
        _state.update { it.copy(challengeMode = enabled) }
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
                modality = ResponseModality.TOUCH
            )
        )
        _state.update { it.copy(metrics = repository.snapshot()) }
    }

    fun startPuzzle() {
        repository.record(LearningEvent(EventType.SESSION_STARTED, activity = PUZZLE_ACTIVITY))
        _state.update { it.copy(screen = AppScreen.PUZZLE, guidedPuzzle = false, message = null, metrics = repository.snapshot()) }
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
                modality = ResponseModality.TOUCH
            )
        )
        _state.update { it.copy(metrics = repository.snapshot()) }
    }

    fun recordBallApplication(modality: ResponseModality) {
        repository.record(
            LearningEvent(
                type = EventType.STAGE_COMPLETED,
                activity = COMIC_ACTIVITY,
                value = "aplicou-pista-na-orientacao",
                modality = modality
            )
        )
        _state.update { it.copy(metrics = repository.snapshot()) }
    }

    fun completeGuidedBallLesson() {
        repository.record(
            LearningEvent(
                type = EventType.SESSION_COMPLETED,
                activity = COMIC_ACTIVITY,
                value = "percurso-bola-grupo",
                modality = ResponseModality.TOUCH
            )
        )
        _state.update { it.copy(
            screen = AppScreen.COMPLETE,
            guidedPuzzle = false,
            ballAnswer = null,
            ballClueAnswer = null,
            completedBallJourney = true,
            metrics = repository.snapshot()
        ) }
    }

    fun setListening(listening: Boolean) = _state.update { it.copy(isListening = listening) }
    fun setSpeaking(speaking: Boolean) = _state.update { it.copy(isSpeaking = speaking) }

    fun voiceAnswer(text: String) {
        val correct = MissionEvaluator.startsWithLetterM(text)
        repository.record(
            LearningEvent(
                type = EventType.RESPONSE_SUBMITTED,
                value = "phoneme_contribution",
                durationMs = (System.currentTimeMillis() - responseStartedAt).coerceAtLeast(0),
                modality = ResponseModality.VOICE
            )
        )
        if (correct) repository.record(LearningEvent(EventType.STAGE_COMPLETED, value = "fonema-m"))
        _state.update {
            it.copy(
                spokenAnswer = text,
                answerCorrect = correct,
                message = if (correct) {
                    "Você encontrou uma palavra com o som de M!"
                } else {
                    "Eu ouvi sua ideia. Vamos procurar outra palavra com o som de Mmmm."
                },
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
                modality = ResponseModality.TOUCH
            )
        )
        if (correct) repository.record(LearningEvent(EventType.STAGE_COMPLETED, value = "interpretacao"))
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
                modality = ResponseModality.CAMERA
            )
        )
        _state.update { it.copy(screen = AppScreen.TALK, metrics = repository.snapshot()) }
    }

    fun completeMission() {
        repository.record(LearningEvent(EventType.SESSION_COMPLETED))
        _state.update { it.copy(screen = AppScreen.COMPLETE, metrics = repository.snapshot()) }
    }

    fun clearMetrics() {
        repository.clear()
        _state.update { it.copy(metrics = repository.snapshot()) }
    }

    private companion object {
        const val BALL_SCENE = "comic-ball"
        const val BALL_CLUE_SCENE = "comic-ball-clue"
        const val COMIC_ACTIVITY = "gibi-bola-amigos"
        const val PUZZLE_ACTIVITY = "quebra-cabeca-palavras"
    }
}
