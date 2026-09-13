package br.gov.interpretaai.domain

import java.text.Normalizer

enum class BallAnswer { BALL, OTHER, EMPTY }

enum class BallClueAnswer { TREE, OTHER, EMPTY }

enum class BallInstruction { COMPLETE, PARTIAL, OTHER, EMPTY }

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

/** Identifica a pista sem enviar ou persistir a fala da criança. */
object BallClueAnswerResolver {
    fun resolve(transcript: String): BallClueAnswer {
        val words = normalizedWords(transcript)
        if (words.isEmpty()) return BallClueAnswer.EMPTY
        return if (words.any { it == "arvore" || it == "tronco" }) {
            BallClueAnswer.TREE
        } else {
            BallClueAnswer.OTHER
        }
    }
}

/** Verifica se a orientação usa o objeto e a pista; é apoio de fluxo, nunca nota. */
object BallInstructionResolver {
    fun resolve(transcript: String): BallInstruction {
        val words = normalizedWords(transcript)
        if (words.isEmpty()) return BallInstruction.EMPTY
        val hasBall = "bola" in words
        val hasPlace = words.any { it == "arvore" || it == "tronco" }
        return when {
            hasBall && hasPlace -> BallInstruction.COMPLETE
            hasBall || hasPlace -> BallInstruction.PARTIAL
            else -> BallInstruction.OTHER
        }
    }
}

private fun normalizedWords(text: String): List<String> = Normalizer.normalize(text, Normalizer.Form.NFD)
    .replace("\\p{M}+".toRegex(), "")
    .lowercase()
    .replace("[^a-z0-9 ]".toRegex(), " ")
    .trim()
    .split("\\s+".toRegex())
    .filter(String::isNotBlank)

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
