package br.gov.interpretaai.domain

enum class EventType {
    SESSION_STARTED,
    PROMPT_HEARD,
    OBSERVATION_RECORDED,
    RESPONSE_SUBMITTED,
    STAGE_COMPLETED,
    HELP_REQUESTED,
    SESSION_COMPLETED
}

enum class ResponseModality { VOICE, TOUCH, CAMERA, DRAWING, NONE }

data class LearningEvent(
    val type: EventType,
    val childAlias: String = "aluno-demo",
    val classroom: String = "Turma 1A",
    val activity: String = "missao-letra-m",
    val value: String? = null,
    val durationMs: Long? = null,
    val success: Boolean? = null,
    val modality: ResponseModality = ResponseModality.NONE,
    val occurredAt: Long = System.currentTimeMillis()
)

data class MetricsSnapshot(
    val sessions: Int = 0,
    val attempts: Int = 0,
    val correctAttempts: Int = 0,
    val completedStages: Int = 0,
    val helpRequests: Int = 0,
    val voiceResponses: Int = 0,
    val comicObservations: Int = 0,
    val comicCyclesCompleted: Int = 0,
    val puzzlesCompleted: Int = 0,
    val averagePuzzleMs: Long = 0,
    val averageResponseMs: Long = 0
) {
    val accuracyPercent: Int
        get() = if (attempts == 0) 0 else (correctAttempts * 100 / attempts)
}

interface MetricsRepository {
    fun record(event: LearningEvent)
    fun snapshot(): MetricsSnapshot
    fun clear()
}
