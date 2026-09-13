package br.gov.interpretaai.domain

import java.text.Normalizer

enum class BallAnswer { BALL, OTHER, EMPTY }

/** Regra local e previsível: a conversa pode acolher, mas nunca controla a navegação. */
object BallAnswerResolver {
    fun resolve(transcript: String): BallAnswer {
        val normalized = Normalizer.normalize(transcript, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .lowercase()
            .replace("[^a-z0-9 ]".toRegex(), " ")
            .trim()
            .replace("\\s+".toRegex(), " ")
        if (normalized.isBlank()) return BallAnswer.EMPTY
        return if (normalized.split(' ').contains("bola")) BallAnswer.BALL else BallAnswer.OTHER
    }
}

enum class PuzzleGuidanceCue { NONE, VOICE_ONCE, VISUAL }

/** Ajuda progressiva: primeiro uma fala curta; depois, indicação visual persistente. */
object PuzzleGuidancePolicy {
    const val VOICE_AFTER_MS = 15_000L
    const val VISUAL_AFTER_MS = 30_000L

    fun cue(elapsedMs: Long, voiceAlreadyPlayed: Boolean): PuzzleGuidanceCue = when {
        elapsedMs >= VISUAL_AFTER_MS -> PuzzleGuidanceCue.VISUAL
        elapsedMs >= VOICE_AFTER_MS && !voiceAlreadyPlayed -> PuzzleGuidanceCue.VOICE_ONCE
        else -> PuzzleGuidanceCue.NONE
    }
}
