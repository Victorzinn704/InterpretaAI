package br.gov.interpretaai

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.gov.interpretaai.ui.screens.PuzzleScreen
import br.gov.interpretaai.ui.theme.InterpretaTheme
import br.gov.interpretaai.domain.ResponseModality
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
            assertTrue(spoken.any { it.startsWith("Conseguiu! Você montou a bola.") })
        }
    }

    @Test
    fun twoByTwoPuzzleAlsoAcceptsDraggingPieces() {
        var result = ""
        compose.setContent {
            InterpretaTheme {
                PuzzleScreen(
                    speak = {},
                    onBack = {},
                    onHelp = {},
                    onCompleted = { subject, _, _, _ -> result = subject }
                )
            }
        }

        compose.onNodeWithText("MONTAR BOLA", substring = true).performClick()
        dragDown(1)
        dragDown(2)

        compose.onNodeWithText("Muito bem! Você montou a bola.").assertExists()
        compose.runOnIdle { assertEquals("bola", result) }
    }

    @Test
    fun guidedPuzzleUsesTheWordInAnInstructionBeforeGroupClosure() {
        var modality: ResponseModality? = null
        var finished = false
        compose.setContent {
            InterpretaTheme {
                PuzzleScreen(
                    speak = {},
                    onBack = {},
                    onHelp = {},
                    onCompleted = { _, _, _, _ -> },
                    guided = true,
                    onApplication = { modality = it },
                    onGuidedFinished = { finished = true }
                )
            }
        }

        swap(1, 4)
        swap(2, 4)
        swap(3, 4)
        compose.onNodeWithText("USAR NA HISTÓRIA", substring = true).performClick()
        compose.onNodeWithText("Davi está esperando sua pista.").assertExists()
        compose.onNodeWithText("PRECISO DE UMA PISTA", substring = true).performClick()
        compose.onNodeWithText("USAR A PISTA COM A LÉIA", substring = true).performClick()
        compose.onNodeWithText("CONTAR AO GRUPO", substring = true).performClick()
        compose.onNodeWithText("qual pista mostrou onde a bola estava?", substring = true).assertExists()
        compose.onNodeWithText("TERMINAMOS JUNTOS", substring = true).performClick()

        compose.runOnIdle {
            assertEquals(ResponseModality.TOUCH, modality)
            assertTrue(finished)
        }
    }

    @Test
    fun guidedChallengeUsesSixPiecesWithoutAddingAChildChoice() {
        var result = ""
        compose.setContent {
            InterpretaTheme {
                PuzzleScreen(
                    speak = {},
                    onBack = {},
                    onHelp = {},
                    onCompleted = { subject, level, _, _ -> result = "$subject:$level" },
                    guided = true,
                    challengeMode = true
                )
            }
        }

        compose.onNodeWithContentDescription("Peça na posição 1 de 6").assertExists()
        repeat(5) { index -> swapChallenge(index + 1, 6) }
        compose.onNodeWithText("Muito bem! Você montou a bola.").assertExists()
        compose.runOnIdle { assertEquals("bola:3 × 2", result) }
    }

    @Test
    fun ordinarySwapUsesSoundWithoutRepeatingSpokenSentence() {
        val spoken = mutableListOf<String>()
        compose.setContent {
            InterpretaTheme {
                PuzzleScreen(
                    speak = spoken::add,
                    onBack = {},
                    onHelp = {},
                    onCompleted = { _, _, _, _ -> },
                    guided = true
                )
            }
        }

        swap(1, 2)
        compose.runOnIdle {
            assertTrue(spoken.none { it == "As peças trocaram de lugar." })
        }
    }

    @Test
    fun applicationReengagesOnlyAfterInactivityAndSpeaksOnce() {
        val spoken = mutableListOf<String>()
        compose.setContent {
            InterpretaTheme {
                PuzzleScreen(
                    speak = spoken::add,
                    onBack = {},
                    onHelp = {},
                    onCompleted = { _, _, _, _ -> },
                    guided = true
                )
            }
        }
        swap(1, 4)
        swap(2, 4)
        swap(3, 4)
        compose.onNodeWithText("USAR NA HISTÓRIA", substring = true).performClick()
        compose.runOnIdle { spoken.clear() }
        compose.mainClock.autoAdvance = false

        compose.mainClock.advanceTimeBy(19_999)
        compose.onNodeWithText("DÊ UMA PISTA PARA DAVI").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText("DÊ UMA PISTA PARA DAVI", substring = true).assertExists()
        compose.mainClock.advanceTimeBy(20_000)
        compose.runOnIdle {
            assertEquals(1, spoken.count { it == "Ei, o Davi ainda espera sua pista. Onde ele deve procurar?" })
        }
        compose.mainClock.advanceTimeBy(20_000)
        compose.runOnIdle {
            assertEquals(1, spoken.count { it == "Ei, o Davi ainda espera sua pista. Onde ele deve procurar?" })
        }
    }

    private fun swap(first: Int, second: Int) {
        compose.onNodeWithContentDescription("Peça na posição $first de 4").performClick()
        compose.onNodeWithContentDescription("Peça na posição $second de 4").performClick()
    }

    private fun swapChallenge(first: Int, second: Int) {
        compose.onNodeWithContentDescription("Peça na posição $first de 6").performClick()
        compose.onNodeWithContentDescription("Peça na posição $second de 6").performClick()
    }

    private fun dragDown(position: Int) {
        compose.onNodeWithContentDescription("Peça na posição $position de 4").performTouchInput {
            swipe(center, androidx.compose.ui.geometry.Offset(center.x, center.y * 3f))
        }
    }
}
