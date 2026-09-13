package br.gov.interpretaai

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import br.gov.interpretaai.domain.ComicStories
import br.gov.interpretaai.ui.screens.ComicsScreen
import br.gov.interpretaai.ui.theme.InterpretaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ComicsFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun narratedStoryRequiresParticipationAndRealWordAssembly() {
        val spoken = mutableListOf<String>()
        val choices = mutableListOf<Pair<Int, Int>>()
        var wordBuilt = 0
        var completedPath = ""
        compose.setContent {
            InterpretaTheme {
                ComicsScreen(
                    speak = spoken::add,
                    onBack = {},
                    onSceneAnswered = { scene, choice -> choices += scene to choice },
                    onWordBuilt = { wordBuilt++ },
                    onCompleted = { completedPath = it }
                )
            }
        }
        tap("A BOLA E OS AMIGOS", substring = true)
        ComicStories.scenes.forEachIndexed { index, scene ->
            val next = if (index == ComicStories.scenes.lastIndex) "MONTAR NOSSO BILHETE" else "PRÓXIMO QUADRINHO"
            tap("EU OBSERVEI", substring = true)
            tap(scene.choices.first().label, substring = true)
            tap(next, substring = true)
        }
        compose.onNodeWithText("CONTINUAR COM A TURMA", substring = true).assertIsNotEnabled()
        listOf("A", "L", "B", "O").forEach { tap(it) }
        compose.onNodeWithText("CONTINUAR COM A TURMA", substring = true).assertIsNotEnabled()
        tap("RECOMEÇAR")
        listOf("B", "O", "L", "A").forEach { tap(it) }
        tap("CONTINUAR COM A TURMA", substring = true)
        compose.onNodeWithText("APRENDER • Nossa história").assertExists()
        tap("CONCLUÍMOS COM A TURMA", substring = true)
        compose.runOnIdle {
            assertTrue(spoken.any { it.startsWith("Oi! Eu sou a LEIA") })
            ComicStories.scenes.drop(1).forEach { scene -> assertTrue(spoken.contains(scene.narration)) }
            assertTrue(spoken.any { it.startsWith("Você montou bola!") })
            assertTrue(choices == ComicStories.scenes.indices.map { it to 0 })
            assertTrue(wordBuilt == 1)
            assertTrue(completedPath.contains("procurar a bola"))
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
