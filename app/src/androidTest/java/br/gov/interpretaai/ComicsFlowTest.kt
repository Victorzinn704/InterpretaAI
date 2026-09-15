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
                        clue == BallClueAnswer.TREE -> VoiceTurnResult("Boa investigação! Vamos procurar atrás da árvore.")
                        clue == BallClueAnswer.OTHER -> VoiceTurnResult("Essa é uma possibilidade. Compare a mochila com as marcas da imagem e investigue outra vez.")
                        answer != null -> VoiceTurnResult("Isso! Você percebeu que falta a bola.")
                        else -> null
                    },
                    onBallAnswer = { answer = BallAnswer.BALL },
                    onBallClueAnswer = { clue = BallClueAnswer.TREE },
                    onBallClueOther = { clue = BallClueAnswer.OTHER },
                    onGuidedPuzzle = { guidedPuzzleStarted = true }
                )
            }
        }
        tap("EU OBSERVEI", substring = true)
        compose.onNodeWithText("O que está faltando para Lia brincar?").assertExists()
        tap("BOLA", substring = true)
        tap("SEGUIR AS PISTAS", substring = true)
        compose.onNodeWithText("Onde ele deve procurar primeiro?", substring = true).assertExists()
        compose.onNodeWithText("ÁRVORE", substring = true).assertDoesNotExist()
        tap("RESPONDER COM FIGURAS", substring = true)
        tap("MOCHILA", substring = true)
        compose.onNodeWithText("MONTAR A BOLA", substring = true).assertDoesNotExist()
        compose.onNodeWithText("investigue outra vez", substring = true).assertIsDisplayed()
        compose.onNodeWithText("ÁRVORE", substring = true).assertIsDisplayed()
        tap("ÁRVORE", substring = true)
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

    @Test fun respondingStateKeepsAVisibleAttentionCue() {
        compose.setContent {
            InterpretaTheme {
                ComicsScreen(speak = {}, onBack = {}, isResponding = true)
            }
        }
        tap("EU OBSERVEI", substring = true)
        compose.onNodeWithText("ESTOU JUNTANDO AS PISTAS", substring = true).assertIsDisplayed()
        compose.onNodeWithText("LEIA ESTÁ PENSANDO", substring = true).assertIsDisplayed()
    }

    @Test fun listeningStateUsesAUsefulCueAndReducedStimuliRemovesIt() {
        var reduced by mutableStateOf(false)
        compose.setContent {
            InterpretaTheme {
                ComicsScreen(
                    speak = {}, onBack = {}, isListening = true, reducedStimuli = reduced
                )
            }
        }
        tap("EU OBSERVEI", substring = true)
        compose.onNodeWithText("ESTOU OUVINDO SUA IDEIA", substring = true).assertIsDisplayed()

        compose.runOnIdle { reduced = true }
        compose.onNodeWithText("ESTOU OUVINDO SUA IDEIA", substring = true).assertDoesNotExist()
    }

    @Test fun validatedTextAppearsWhileFriendlyVoiceIsStillBeingPrepared() {
        compose.setContent {
            InterpretaTheme {
                ComicsScreen(
                    speak = {},
                    onBack = {},
                    isResponding = true,
                    leiaReply = VoiceTurnResult(
                        replyText = "Sua observação ajudou a história!",
                        audioPending = true
                    )
                )
            }
        }
        tap("←")
        tap("CENAS", substring = true)
        tap("Locomoção", substring = true)
        tap("EU OBSERVEI", substring = true)

        compose.onNodeWithText("LEIA • PREPARANDO A VOZ").assertIsDisplayed()
        compose.onNodeWithText("Sua observação ajudou a história!").assertIsDisplayed()
    }
}
