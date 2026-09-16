package br.gov.interpretaai.domain

/** Três desafios fechados. A sequência orienta a atividade, não vira nota da criança. */
object MiniGameRules {
    val numberPath = listOf(1, 2, 3, 4, 5)
    val houseDots = listOf(1, 2, 3, 4, 5)
    val ballLetters = listOf('B', 'O', 'L', 'A')

    fun acceptsNumber(progress: Int, tapped: Int, dots: Boolean): Boolean {
        val path = if (dots) houseDots else numberPath
        return path.getOrNull(progress) == tapped
    }

    fun acceptsLetter(progress: Int, tapped: Char): Boolean = ballLetters.getOrNull(progress) == tapped
}
