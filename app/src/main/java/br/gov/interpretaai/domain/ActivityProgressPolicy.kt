package br.gov.interpretaai.domain

/** A pedagogical handoff, never a verdict about the child. */
enum class AssistedAdvanceReason(val metricValue: String) {
    TIME_LIMIT("time_limit"),
    ATTEMPT_LIMIT("attempt_limit")
}

object ActivityProgressPolicy {
    const val ACTIVE_TIME_LIMIT_MS = 3 * 60 * 1_000L
    const val UNSUCCESSFUL_ATTEMPT_LIMIT = 3

    fun reason(
        activeElapsedMs: Long,
        unsuccessfulAttempts: Int,
        hasCheckableAnswer: Boolean
    ): AssistedAdvanceReason? = when {
        hasCheckableAnswer && unsuccessfulAttempts >= UNSUCCESSFUL_ATTEMPT_LIMIT ->
            AssistedAdvanceReason.ATTEMPT_LIMIT
        activeElapsedMs >= ACTIVE_TIME_LIMIT_MS -> AssistedAdvanceReason.TIME_LIMIT
        else -> null
    }
}
