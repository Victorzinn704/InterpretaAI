package br.gov.interpretaai

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.gov.interpretaai.domain.ComicStories
import br.gov.interpretaai.domain.BallAnswer
import br.gov.interpretaai.domain.BallClueAnswer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import br.gov.interpretaai.platform.VoiceTurnResult
import br.gov.interpretaai.ui.screens.ComicsScreen
import br.gov.interpretaai.ui.theme.InterpretaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ComicsFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun ballStoryConnectsObjectClueAndGuidedPuzzle() {
        val spoken = mutableListOf<String>()
        var answer by mutableStateOf<BallAnswer?>(null)
        var clue by mutableStateOf<BallClueAnswer?>(null)
        var guidedPuzzleStarted = false
        compose.setContent {
            InterpretaTheme {
                ComicsScreen(
                    speak = spoken::add,
                    onBack = {},
                    ballAnswer = answer,
                    ballClueAnswer = clue,
                    leiaReply = when {
                        clue != null -> VoiceTurnResult("Boa investigação! Vamos procurar atrás da árvore.")
                        answer != null -> VoiceTurnResult("Isso! Você percebeu que falta a bola.")
                        else -> null
                    },
                    onBallAnswer = { answer = BallAnswer.BALL },
                    onBallClueAnswer = { clue = BallClueAnswer.TREE },
                    onGuidedPuzzle = { guidedPuzzleStarted = true }
                )
            }
        }
        tap("EU OBSERVEI", substring = true)
        compose.onNodeWithText("O que está faltando para Lia brincar?").assertExists()
        tap("BOLA", substring = true)
        tap("SEGUIR AS PISTAS", substring = true)
        compose.onNodeWithText("Onde Davi deve procurar primeiro?", substring = true).assertExists()
        tap("ATRÁS DA ÁRVORE", substring = true)
        tap("MONTAR A BOLA", substring = true)
        compose.runOnIdle {
            assertTrue(spoken.any { it.startsWith("Oi! Eu sou a LEIA") })
            assertTrue(spoken.contains("Bola"))
            assertTrue(guidedPuzzleStarted)
        }
    }

    private fun tap(text: String, substring: Boolean = false) {
        compose.onNodeWithText(text, substring = substring).assertIsDisplayed().performClick()
    }

    @Test fun expressiveSceneCanBeChosenWithoutReplayingStory() {
        compose.setContent { InterpretaTheme { ComicsScreen({}, {}) } }
        tap("←")
        tap("CENAS", substring = true)
        tap("Locomoção", substring = true)
        compose.onNodeWithText("Cada um chega de um jeito").assertExists()
        tap("EU OBSERVEI", substring = true)
        tap("De bicicleta", substring = true)
        compose.onNodeWithText(ComicStories.scenes.last().choices.last().reply).assertExists()
        tap("ESCOLHER OUTRA CENA", substring = true)
        tap("Raiva", substring = true)
        compose.onNodeWithText("Um chute e uma conversa").assertExists()
    }
}
