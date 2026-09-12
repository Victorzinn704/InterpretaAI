package br.gov.interpretaai

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.gov.interpretaai.ui.screens.PuzzleScreen
import br.gov.interpretaai.ui.theme.InterpretaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PuzzleFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun twoByTwoPuzzleCompletesByTouchingPairs() {
        val spoken = mutableListOf<String>()
        var result = ""
        var moves = 0
        compose.setContent {
            InterpretaTheme {
                PuzzleScreen(
                    speak = spoken::add,
                    onBack = {},
                    onHelp = {},
                    onCompleted = { subject, level, moveCount, _ ->
                        result = "$subject:$level"
                        moves = moveCount
                    }
                )
            }
        }

        compose.onNodeWithText("MONTAR BOLA", substring = true).performClick()
        swap(1, 3)
        swap(2, 4)

        compose.onNodeWithText("Muito bem! Você montou a bola.").assertExists()
        compose.runOnIdle {
            assertEquals("bola:2 × 2", result)
            assertEquals(2, moves)
            assertTrue(spoken.any { it.startsWith("Parabéns!") })
        }
    }

    private fun swap(first: Int, second: Int) {
        compose.onNodeWithContentDescription("Peça na posição $first de 4").performClick()
        compose.onNodeWithContentDescription("Peça na posição $second de 4").performClick()
    }
}
