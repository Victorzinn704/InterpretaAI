package br.gov.interpretaai.domain

enum class PuzzleSubject(val spokenWord: String, val icon: String) {
    BALL("bola", "⚽"),
    BANANA("banana", "🍌"),
    APPLE("maçã", "🍎")
}

data class PuzzleSize(val columns: Int, val rows: Int) {
    val pieceCount: Int get() = columns * rows
    val label: String get() = "$columns × $rows"

    companion object {
        val EASY = PuzzleSize(2, 2)
        val CHALLENGE = PuzzleSize(3, 2)
    }
}

object PuzzleGame {
    fun initialTiles(size: PuzzleSize, round: Int = 0): List<Int> {
        val pieces = (0 until size.pieceCount).toList()
        val shift = round.mod(size.pieceCount - 1) + 1
        return pieces.drop(shift) + pieces.take(shift)
    }

    fun swap(tiles: List<Int>, first: Int, second: Int): List<Int> =
        tiles.toMutableList().also {
            val saved = it[first]
            it[first] = it[second]
            it[second] = saved
        }

    fun isComplete(tiles: List<Int>): Boolean =
        tiles.indices.all { tiles[it] == it }
}
