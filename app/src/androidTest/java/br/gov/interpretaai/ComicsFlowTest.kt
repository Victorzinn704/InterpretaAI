package br.gov.interpretaai

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.gov.interpretaai.domain.ComicStories
import br.gov.interpretaai.domain.BallAnswer
import br.gov.interpretaai.domain.BallClueAnswer
import br.gov.interpretaai.domain.AssignedActivity
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
                        clue == BallClueAnswer.TREE -> VoiceTurnResult("Boa pista! Olha só: a bola aparece atrás da árvore.")
                        clue == BallClueAnswer.OTHER -> VoiceTurnResult("Pode ser. Mas o que aparece pertinho do tronco?")
                        answer != null -> VoiceTurnResult("Isso, é a bola! Quer descobrir onde ela foi parar?")
                        else -> null
                    },
                    onBallAnswer = { answer = BallAnswer.BALL },
                    onBallClueAnswer = { clue = BallClueAnswer.TREE },
                    onBallClueOther = { clue = BallClueAnswer.OTHER },
                    onGuidedPuzzle = { guidedPuzzleStarted = true }
                )
            }
        }
        compose.onNodeWithTag("ball-missing-focus").performClick()
        compose.runOnIdle {
            assertTrue(spoken.any { it.contains("marca redonda") })
        }
        tap("EU OBSERVEI", substring = true)
        compose.onNodeWithText("O que está faltando para Lia brincar?").assertExists()
        compose.onNodeWithText("BOLA").assertDoesNotExist()
        tap("RESPONDER COM FIGURA", substring = true)
        tap("BOLA", substring = true)
        tap("SEGUIR AS PISTAS", substring = true)
        compose.onNodeWithText("INTERPRETAR • SIGA AS MARCAS").assertIsDisplayed()
        compose.onNodeWithTag("ball-trail-focus").performClick()
        compose.runOnIdle {
            assertTrue(spoken.any { it.contains("marcas molhadas chegam") })
        }
        compose.onNodeWithText("Onde Davi deve procurar?", substring = true).assertExists()
        compose.onNodeWithText("ÁRVORE", substring = true).assertDoesNotExist()
        tap("RESPONDER COM FIGURAS", substring = true)
        tap("MOCHILA", substring = true)
        compose.onNodeWithText("MONTAR A BOLA", substring = true).assertDoesNotExist()
        compose.onNodeWithText("pertinho do tronco", substring = true).assertIsDisplayed()
        compose.onNodeWithText("ÁRVORE", substring = true).assertIsDisplayed()
        tap("ÁRVORE", substring = true)
        tap("MONTAR A BOLA", substring = true)
        compose.runOnIdle {
            assertTrue(spoken.any { it.startsWith("Oi! Eu sou a LÉIA") && it.contains("Alfa") })
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

    @Test fun reducedStimuliKeepsTheVisualClueTouchableWithoutAutomaticPulse() {
        val spoken = mutableListOf<String>()
        compose.setContent {
            InterpretaTheme {
                ComicsScreen(
                    speak = spoken::add,
                    onBack = {},
                    reducedStimuli = true
                )
            }
        }

        compose.onNodeWithTag("ball-missing-focus")
            .assertHasClickAction()
            .performClick()
        compose.runOnIdle {
            assertTrue(spoken.any { it.contains("marca redonda") })
        }
    }

    @Test fun respondingStateKeepsAVisibleAttentionCue() {
        compose.setContent {
            InterpretaTheme {
                ComicsScreen(speak = {}, onBack = {}, isResponding = true)
            }
        }
        tap("EU OBSERVEI", substring = true)
        compose.onNodeWithText("ESTOU JUNTANDO AS PISTAS", substring = true).assertIsDisplayed()
        compose.onNodeWithText("LÉIA ESTÁ PENSANDO", substring = true).assertIsDisplayed()
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

        compose.onNodeWithText("LÉIA VAI FALAR COM VOCÊ").assertIsDisplayed()
        compose.onNodeWithText("Sua observação ajudou a história!").assertIsDisplayed()
    }

    @Test fun fourthYearPackMovesFromFactToGroupExplanation() {
        var completedPath = ""
        var completionCount = 0
        compose.setContent {
            InterpretaTheme {
                ComicsScreen(
                    assignedActivity = AssignedActivity.FACT_OR_OPINION_4,
                    speak = {},
                    onBack = {},
                    onCompleted = { completedPath = it; completionCount++ }
                )
            }
        }

        compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
        tap("JÁ OBSERVEI", substring = true)
        compose.onNodeWithText("pode ser conferida", substring = true).assertIsDisplayed()
        tap("A QUADRA MOLHOU", substring = true)
        compose.onNodeWithText("Horário, lugar", substring = true).assertIsDisplayed()
        tap("CONTEI AO GRUPO", substring = true)
        compose.onNodeWithText("MISSÃO CONCLUÍDA", substring = true).assertIsNotEnabled()
        compose.onNodeWithText("MISSÃO CONCLUÍDA", substring = true).performClick()
        compose.runOnIdle {
            assertTrue(completedPath.startsWith("fact_or_opinion"))
            assertTrue(completionCount == 1)
        }
    }

    @Test fun fifthYearPackAsksForEvidenceBetweenSources() {
        compose.setContent {
            InterpretaTheme {
                ComicsScreen(
                    assignedActivity = AssignedActivity.COMPARE_SOURCES_5,
                    speak = {},
                    onBack = {}
                )
            }
        }

        compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
        compose.onNodeWithText("dezoito estudantes", substring = true).assertIsDisplayed()
        tap("JÁ OBSERVEI", substring = true)
        tap("MENSAGEM", substring = true)
        compose.onNodeWithText("não mostra como conferir", substring = true).assertIsDisplayed()
    }

    @Test fun secondYearPackOrdersActionsAndReturnsToTheGroup() {
        var completedPath = ""
        compose.setContent {
            InterpretaTheme {
                ComicsScreen(
                    assignedActivity = AssignedActivity.STORY_SEQUENCE_2,
                    speak = {},
                    onBack = {},
                    onCompleted = { completedPath = it }
                )
            }
        }

        compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
        tap("JÁ OBSERVEI", substring = true)
        tap("PREPARAR O VASO", substring = true)
        compose.onNodeWithText("Primeiro Lia preparou", substring = true).assertIsDisplayed()
        tap("CONTEI AO GRUPO", substring = true)
        compose.runOnIdle { assertTrue(completedPath.startsWith("story_sequence")) }
    }

    @Test fun thirdYearPackRelatesCauseAndConsequence() {
        compose.setContent {
            InterpretaTheme {
                ComicsScreen(
                    assignedActivity = AssignedActivity.CAUSE_AND_EFFECT_3,
                    speak = {},
                    onBack = {}
                )
            }
        }

        compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
        tap("JÁ OBSERVEI", substring = true)
        tap("A TURMA MUDOU", substring = true)
        compose.onNodeWithText("foi o resultado", substring = true).assertIsDisplayed()
    }
}
