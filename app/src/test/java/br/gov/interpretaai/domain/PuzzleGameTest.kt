package br.gov.interpretaai.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleGameTest {
    @Test
    fun initialBoardsAreValidAndUnsolved() {
        listOf(PuzzleSize.EASY, PuzzleSize.CHALLENGE).forEach { size ->
            val tiles = PuzzleGame.initialTiles(size)
            assertEquals((0 until size.pieceCount).toSet(), tiles.toSet())
            assertFalse(PuzzleGame.isComplete(tiles))
        }
    }

    @Test
    fun swappingSelectedPositionsCanCompleteTheEasyBoard() {
        var tiles = PuzzleGame.initialTiles(PuzzleSize.EASY, round = 1)
        tiles = PuzzleGame.swap(tiles, 0, 2)
        tiles = PuzzleGame.swap(tiles, 1, 3)
        assertTrue(PuzzleGame.isComplete(tiles))
    }
}
