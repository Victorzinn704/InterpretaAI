package br.gov.interpretaai.domain

enum class ReengagementCue { NONE, VISUAL, SPEAK_ONCE }

object ReengagementPolicy {
    const val VISUAL_AFTER_MS = 20_000L
    const val SPEAK_AFTER_MS = 40_000L

    fun cue(elapsedMs: Long, alreadySpoken: Boolean): ReengagementCue = when {
        elapsedMs < VISUAL_AFTER_MS -> ReengagementCue.NONE
        elapsedMs < SPEAK_AFTER_MS || alreadySpoken -> ReengagementCue.VISUAL
        else -> ReengagementCue.SPEAK_ONCE
    }
}
