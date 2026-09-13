package br.gov.interpretaai

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.gov.interpretaai.domain.ComicStories
import br.gov.interpretaai.domain.BallAnswer
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

    @Test fun ballStoryConnectsUnderstandingDirectlyToGuidedPuzzle() {
        val spoken = mutableListOf<String>()
        var answer by mutableStateOf<BallAnswer?>(null)
        var guidedPuzzleStarted = false
        compose.setContent {
            InterpretaTheme {
                ComicsScreen(
                    speak = spoken::add,
                    onBack = {},
                    ballAnswer = answer,
                    leiaReply = answer?.let {
                        VoiceTurnResult("Isso! Você percebeu que falta a bola. Vamos montá-la para ajudar Lia?")
                    },
                    onBallAnswer = { answer = BallAnswer.BALL },
                    onGuidedPuzzle = { guidedPuzzleStarted = true }
                )
            }
        }
        tap("A BOLA E OS AMIGOS", substring = true)
        tap("EU OBSERVEI", substring = true)
        compose.onNodeWithText("O que está faltando para Lia brincar?").assertExists()
        tap("BOLA", substring = true)
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
