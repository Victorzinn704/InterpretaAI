package br.gov.interpretaai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import br.gov.interpretaai.domain.EventType
import br.gov.interpretaai.domain.LearningEvent
import br.gov.interpretaai.domain.MetricsSnapshot
import br.gov.interpretaai.domain.MetricsRepository
import br.gov.interpretaai.domain.MissionEvaluator
import br.gov.interpretaai.domain.ResponseModality
import br.gov.interpretaai.domain.BallAnswer
import br.gov.interpretaai.domain.BallAnswerResolver
import br.gov.interpretaai.domain.BallClueAnswer
import br.gov.interpretaai.domain.BallClueAnswerResolver
import br.gov.interpretaai.domain.DrawingPrompt
import br.gov.interpretaai.domain.AssignedActivity
import br.gov.interpretaai.domain.ClassroomAssignment
import br.gov.interpretaai.domain.LearnerAvatar
import br.gov.interpretaai.domain.LearnerAvatars
import br.gov.interpretaai.domain.PilotRoomParticipant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import androidx.lifecycle.viewModelScope
import br.gov.interpretaai.platform.VoiceTurnClient
import br.gov.interpretaai.platform.VoiceTurnProgress
import br.gov.interpretaai.platform.VoiceTurnResult
import br.gov.interpretaai.platform.PilotAssignmentClient
import br.gov.interpretaai.platform.PilotSyncResult
import br.gov.interpretaai.platform.PilotClassroomClient
import br.gov.interpretaai.platform.PilotClassroomResult
import br.gov.interpretaai.platform.PilotLearningClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen { HOME, COMICS, PUZZLE, DRAWING, MISSION, INTERPRET, APPLY, CAMERA, TALK, COMPLETE, EDUCATOR }

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
    val completedDrawing: Boolean = false,
    val reducedStimuli: Boolean = false,
    val challengeMode: Boolean = false,
    val drawingPrompt: DrawingPrompt = DrawingPrompt.BALL,
    val classroomLabel: String = "Turma 1A",
    val learnerAlias: String = "sol-01",
    val activeAvatar: LearnerAvatar = LearnerAvatars.available.first(),
    val assignedActivity: AssignedActivity = AssignedActivity.COMIC,
    val syncDeviceId: String = "",
    val syncVersion: Long = 0,
    val syncStatus: String = "Sincronização online não configurada.",
    val roomSyncStatus: String = "Nenhuma missão enviada para uma sala.",
    val isSyncing: Boolean = false,
    val metrics: MetricsSnapshot = MetricsSnapshot()
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("educator_settings", 0)
    private val rawRepository = (application as InterpretaAiApplication).metricsRepository
    private var eventClassroom = preferences.getString("classroom_label", "Turma 1A") ?: "Turma 1A"
    private var eventAvatar = LearnerAvatars.find(preferences.getString("avatar_id", null)).id
    private var eventLearnerAlias = preferences.getString("learner_alias", null) ?: "$eventAvatar-01"
    private val repository = object : MetricsRepository {
        override fun record(event: LearningEvent) {
            rawRepository.record(event.copy(childAlias = eventLearnerAlias, classroom = eventClassroom))
            requestLearningEventSync()
        }
        override fun snapshot() = rawRepository.snapshot()
        override fun pending(limit: Int) = rawRepository.pending(limit)
        override fun markSynced(eventIds: List<String>) = rawRepository.markSynced(eventIds)
        override fun clear() = rawRepository.clear()
    }
    private val voiceTurns = VoiceTurnClient(deviceToken = {
        preferences.getString("sync_device_token", "").orEmpty()
    })
    private val pilotAssignments = PilotAssignmentClient()
    private val pilotClassrooms = PilotClassroomClient()
    private val pilotLearning = PilotLearningClient()
    private val _state = MutableStateFlow(AppUiState(
        metrics = repository.snapshot(),
        reducedStimuli = preferences.getBoolean("reduced_stimuli", false),
        challengeMode = preferences.getBoolean("challenge_mode", false),
        classroomLabel = preferences.getString("classroom_label", "Turma 1A") ?: "Turma 1A",
        learnerAlias = preferences.getString("learner_alias", null)
            ?: "${LearnerAvatars.find(preferences.getString("avatar_id", null)).id}-01",
        activeAvatar = LearnerAvatars.find(preferences.getString("avatar_id", null)),
        assignedActivity = runCatching {
            AssignedActivity.valueOf(preferences.getString("assigned_activity", "COMIC")!!)
        }.getOrDefault(AssignedActivity.COMIC),
        drawingPrompt = runCatching {
            DrawingPrompt.valueOf(preferences.getString("drawing_prompt", "BALL")!!)
        }.getOrDefault(DrawingPrompt.BALL),
        syncDeviceId = preferences.getString("sync_device_id", "") ?: "",
        syncVersion = preferences.getLong("sync_assignment_version", 0),
        syncStatus = if (preferences.getString("sync_device_token", "").isNullOrBlank()) {
            "Sincronização online não configurada."
        } else {
            "Tablet configurado; buscando novas atividades."
        }
    ))
    val state: StateFlow<AppUiState> = _state
    private var responseStartedAt = 0L
    private var voiceSessionId = UUID.randomUUID().toString()
    private var voiceTurn = 0
    private var voiceTurnJob: Job? = null
    private var assignmentSyncJob: Job? = null
    private var learningSyncJob: Job? = null

    init {
        // Compra tempo de aquecimento enquanto a criança ainda está na tela inicial.
        viewModelScope.launch(Dispatchers.IO) { voiceTurns.warmup() }
        refreshPilotAssignment()
        requestLearningEventSync()
    }

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
                completedDrawing = false,
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
            completedDrawing = false,
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
            ) { progress ->
                if (progress is VoiceTurnProgress.FinalText
                    && requestedSession == voiceSessionId
                    && _state.value.screen == AppScreen.COMICS) {
                    _state.update { it.copy(leiaReply = progress.value, isLeiaResponding = true) }
                }
            }
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

    fun setDrawingPrompt(prompt: DrawingPrompt) = _state.update { it.copy(drawingPrompt = prompt) }

    fun publishAssignment(assignment: ClassroomAssignment) {
        saveAssignment(assignment, "Atividade aplicada neste tablet.")
    }

    fun configurePilotReceiver(deviceId: String, deviceToken: String) {
        val normalizedId = deviceId.trim()
        if (!normalizedId.matches(Regex("[a-zA-Z0-9_-]{6,64}")) || deviceToken.length < 16) {
            _state.update { it.copy(syncStatus = "Use um ID válido e token com pelo menos 16 caracteres.") }
            return
        }
        val deviceChanged = normalizedId != _state.value.syncDeviceId
        preferences.edit()
            .putString("sync_device_id", normalizedId)
            .putString("sync_device_token", deviceToken)
            .apply()
        if (deviceChanged) preferences.edit().putLong("sync_assignment_version", 0).apply()
        _state.update { it.copy(
            syncDeviceId = normalizedId,
            syncVersion = if (deviceChanged) 0 else it.syncVersion,
            syncStatus = "Tablet configurado; buscando novas atividades."
        ) }
        viewModelScope.launch(Dispatchers.IO) { voiceTurns.warmup() }
        refreshPilotAssignment()
        requestLearningEventSync()
    }

    fun refreshPilotAssignment() {
        if (assignmentSyncJob?.isActive == true) return
        val deviceId = preferences.getString("sync_device_id", "").orEmpty()
        val deviceToken = preferences.getString("sync_device_token", "").orEmpty()
        if (deviceId.isBlank() || deviceToken.isBlank()) return
        val currentVersion = preferences.getLong("sync_assignment_version", 0)
        _state.update { it.copy(isSyncing = true, syncStatus = "Buscando atividade…") }
        assignmentSyncJob = viewModelScope.launch(Dispatchers.IO) {
            when (val result = pilotAssignments.fetch(deviceId, deviceToken, currentVersion)) {
                is PilotSyncResult.Updated -> {
                    preferences.edit().putLong("sync_assignment_version", result.version).apply()
                    saveAssignment(result.assignment, "Nova atividade recebida do professor.")
                    _state.update { it.copy(
                        syncVersion = result.version,
                        syncStatus = "Atividade ${result.version} recebida.",
                        isSyncing = false
                    ) }
                }
                PilotSyncResult.NoChange -> _state.update { it.copy(
                    syncStatus = "Tablet atualizado • versão $currentVersion.",
                    isSyncing = false
                ) }
                is PilotSyncResult.Failed -> _state.update { it.copy(
                    syncStatus = result.message,
                    isSyncing = false
                ) }
            }
        }
    }

    private fun requestLearningEventSync() {
        if (learningSyncJob?.isActive == true) return
        val deviceId = preferences.getString("sync_device_id", "").orEmpty()
        val deviceToken = preferences.getString("sync_device_token", "").orEmpty()
        if (deviceId.isBlank() || deviceToken.isBlank()) return
        learningSyncJob = viewModelScope.launch(Dispatchers.IO) {
            repeat(4) {
                val pending = repository.pending(50)
                if (pending.isEmpty()) return@launch
                if (!pilotLearning.send(deviceId, deviceToken, pending)) return@launch
                repository.markSynced(pending.map { it.eventId })
            }
        }
    }

    fun publishRemoteAssignment(
        targetDeviceId: String,
        teacherToken: String,
        assignment: ClassroomAssignment
    ) {
        if (assignmentSyncJob?.isActive == true) return
        _state.update { it.copy(isSyncing = true, syncStatus = "Enviando atividade…") }
        assignmentSyncJob = viewModelScope.launch(Dispatchers.IO) {
            when (val result = pilotAssignments.publish(targetDeviceId.trim(), teacherToken, assignment)) {
                is PilotSyncResult.Updated -> _state.update { it.copy(
                    syncStatus = "Atividade ${result.version} enviada para ${result.assignment.avatar.label}.",
                    isSyncing = false
                ) }
                PilotSyncResult.NoChange -> _state.update { it.copy(
                    syncStatus = "Nenhuma alteração enviada.", isSyncing = false
                ) }
                is PilotSyncResult.Failed -> _state.update { it.copy(
                    syncStatus = result.message, isSyncing = false
                ) }
            }
        }
    }

    fun publishRoomAssignment(
        classroomId: String,
        teacherToken: String,
        participants: List<PilotRoomParticipant>,
        targetAliases: Set<String>,
        assignment: ClassroomAssignment
    ) {
        if (assignmentSyncJob?.isActive == true) return
        _state.update { it.copy(isSyncing = true, roomSyncStatus = "Enviando missão para a sala…") }
        assignmentSyncJob = viewModelScope.launch(Dispatchers.IO) {
            val result = pilotClassrooms.saveAndPublish(
                classroomId.trim(), assignment.classroomLabel, teacherToken, participants,
                assignment.activity, assignment.drawingPrompt, targetAliases
            )
            _state.update { current -> when (result) {
                is PilotClassroomResult.Published -> current.copy(
                    isSyncing = false,
                    roomSyncStatus = "Missão enviada para ${result.targetCount} tablet(s)."
                )
                is PilotClassroomResult.Failed -> current.copy(
                    isSyncing = false,
                    roomSyncStatus = result.message
                )
            } }
        }
    }

    private fun saveAssignment(assignment: ClassroomAssignment, feedback: String) {
        eventClassroom = assignment.classroomLabel
        eventAvatar = assignment.avatar.id
        eventLearnerAlias = assignment.learnerAlias
        preferences.edit()
            .putString("classroom_label", assignment.classroomLabel)
            .putString("avatar_id", assignment.avatar.id)
            .putString("learner_alias", assignment.learnerAlias)
            .putString("assigned_activity", assignment.activity.name)
            .putString("drawing_prompt", assignment.drawingPrompt.name)
            .apply()
        _state.update { it.copy(
            classroomLabel = assignment.classroomLabel,
            learnerAlias = assignment.learnerAlias,
            activeAvatar = assignment.avatar,
            assignedActivity = assignment.activity,
            drawingPrompt = assignment.drawingPrompt,
            message = feedback
        ) }
    }

    fun startAssignedActivity() = when (_state.value.assignedActivity) {
        AssignedActivity.COMIC -> startComic()
        AssignedActivity.PUZZLE -> startPuzzle()
        AssignedActivity.DRAWING -> startDrawing()
        AssignedActivity.SOUND_M -> startMission()
        AssignedActivity.STORY_SEQUENCE_2,
        AssignedActivity.CAUSE_AND_EFFECT_3,
        AssignedActivity.FACT_OR_OPINION_4,
        AssignedActivity.COMPARE_SOURCES_5 -> startComic()
    }

    fun startDrawing() {
        repository.record(LearningEvent(EventType.SESSION_STARTED, activity = "quadro-criativo"))
        _state.update { it.copy(screen = AppScreen.DRAWING, completedDrawing = false, metrics = repository.snapshot()) }
    }

    fun completeDrawing() {
        repository.record(LearningEvent(
            EventType.SESSION_COMPLETED,
            activity = "quadro-criativo",
            value = _state.value.drawingPrompt.name.lowercase(),
            modality = ResponseModality.DRAWING
        ))
        _state.update { it.copy(screen = AppScreen.COMPLETE, completedDrawing = true, metrics = repository.snapshot()) }
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
