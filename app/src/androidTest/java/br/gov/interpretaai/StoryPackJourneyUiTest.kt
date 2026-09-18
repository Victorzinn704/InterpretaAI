package br.gov.interpretaai

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.platform.app.InstrumentationRegistry
import br.gov.interpretaai.domain.ComicStoryNode
import br.gov.interpretaai.domain.EndStoryNode
import br.gov.interpretaai.domain.GroupHandoffStoryNode
import br.gov.interpretaai.domain.LearningStoryPack
import br.gov.interpretaai.domain.PuzzleStoryNode
import br.gov.interpretaai.domain.StoryDialogueLine
import br.gov.interpretaai.domain.WordBuilderStoryNode
import br.gov.interpretaai.platform.storycache.PreparedAssignedStory
import br.gov.interpretaai.ui.screens.StoryPackScreen
import br.gov.interpretaai.ui.theme.InterpretaTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StoryPackJourneyUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun comicPuzzleWordAndGroupAreOneOfflineJourney() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val image = File(context.cacheDir, "story-journey-test-apple.jpg")
        context.resources.openRawResource(R.drawable.puzzle_apple).use { source ->
            image.outputStream().use(source::copyTo)
        }
        val stages = mutableListOf<String>()
        var completed = false
        compose.setContent {
            InterpretaTheme {
                StoryPackScreen(
                    story = story(image), speak = {}, listen = {},
                    isListening = false, isSpeaking = false, reducedStimuli = true,
                    onBack = {}, onHelpRequested = {}, onVoiceContribution = {},
                    onStageCompleted = { node, _, _ -> stages += node },
                    onCompleted = { completed = true }
                )
            }
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithContentDescription("Quadrinho da maçã").fetchSemanticsNodes().isNotEmpty()
        }
        capture("storypack-1-gibi")
        assertFullyWithinScreen("PENSAR E CONTINUAR")
        compose.onNodeWithText("PENSAR E CONTINUAR", substring = true).assertIsDisplayed().performClick()

        val board = compose.onNodeWithTag("story-puzzle-board")
        board.assertIsDisplayed()
        capture("storypack-2-puzzle")
        assertFullyWithinScreen("OUVIR PISTA")
        fun swap(first: Int, second: Int) {
            listOf(first, second).forEach { position ->
                board.performTouchInput {
                    click(Offset((position % 2 + .5f) * width / 2,
                        (position / 2 + .5f) * height / 2))
                }
            }
        }
        board.performTouchInput {
            swipe(Offset(width * .25f, height * .25f),
                Offset(width * .75f, height * .75f), 400)
        }
        swap(1, 3)
        swap(2, 3)
        compose.onNodeWithText("CONTINUAR A HISTÓRIA", substring = true)
            .assertIsDisplayed().performClick()

        capture("storypack-3-palavra")
        assertFullyWithinScreen("OUVIR O SOM")
        listOf("M", "A", "Ç", "Ã").forEach { letter ->
            compose.onNodeWithText(letter).assertIsDisplayed().performClick()
        }
        compose.onNodeWithText("CONTINUAR A HISTÓRIA", substring = true)
            .assertIsDisplayed().performClick()
        capture("storypack-4-dupla")
        assertFullyWithinScreen("CONTINUAR DEPOIS DA CONVERSA")
        compose.onNodeWithText("CONTINUAR DEPOIS DA CONVERSA", substring = true)
            .assertIsDisplayed().performClick()
        capture("storypack-5-fim")
        assertFullyWithinScreen("VOLTAR AO INÍCIO")
        compose.onNodeWithText("VOLTAR AO INÍCIO", substring = true).assertIsDisplayed().performClick()

        compose.runOnIdle {
            assertEquals(listOf("cena", "puzzle", "palavra", "dupla"), stages)
            assertTrue(completed)
        }
        image.delete()
    }

    @Test fun resumesAtThePersistedNodeWithoutReplayingTheComic() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val image = File(context.cacheDir, "story-resume-test-apple.jpg")
        context.resources.openRawResource(R.drawable.puzzle_apple).use { source ->
            image.outputStream().use(source::copyTo)
        }
        compose.setContent {
            InterpretaTheme {
                StoryPackScreen(
                    story = story(image, resumeNodeId = "palavra"), speak = {}, listen = {},
                    isListening = false, isSpeaking = false, reducedStimuli = true,
                    onBack = {}, onHelpRequested = {}, onVoiceContribution = {},
                    onStageCompleted = { _, _, _ -> }, onCompleted = {}
                )
            }
        }

        compose.onNodeWithText("INTERPRETAR • FORME A PALAVRA").assertIsDisplayed()
        assertTrue(compose.onAllNodesWithText("Vamos procurar a fruta?")
            .fetchSemanticsNodes().isEmpty())
        image.delete()
    }

    @Test fun theSameRendererStartsABallStoryFromData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val image = File(context.cacheDir, "story-journey-test-ball.jpg")
        context.resources.openRawResource(R.drawable.puzzle_ball).use { source ->
            image.outputStream().use(source::copyTo)
        }
        compose.setContent {
            InterpretaTheme {
                StoryPackScreen(
                    story = ballStory(image), speak = {}, listen = {},
                    isListening = false, isSpeaking = false, reducedStimuli = true,
                    onBack = {}, onHelpRequested = {}, onVoiceContribution = {},
                    onStageCompleted = { _, _, _ -> }, onCompleted = {}
                )
            }
        }

        compose.onNodeWithText("A bola no recreio").assertIsDisplayed()
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("A bola sumiu perto da árvore.")
                .fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithText("PENSAR E CONTINUAR", substring = true)
                    .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("A bola sumiu perto da árvore.").assertExists()
        assertFullyWithinScreen("PENSAR E CONTINUAR")
        compose.onNodeWithText("PENSAR E CONTINUAR", substring = true).performClick()
        compose.onNodeWithText("Monte a bola.").assertIsDisplayed()
        compose.onNodeWithTag("story-puzzle-board").assertIsDisplayed()
        image.delete()
    }

    private fun capture(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "$name-${bitmap.width}x${bitmap.height}.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun assertFullyWithinScreen(label: String) {
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithText(label, substring = true)
            .fetchSemanticsNode().boundsInRoot
        assertTrue("$label foi cortado: $button fora de $root",
            button.top >= root.top && button.bottom <= root.bottom)
    }

    private fun story(image: File, resumeNodeId: String = "cena"): PreparedAssignedStory {
        val objective = listOf("ler_maca")
        val nodes = listOf(
            ComicStoryNode("cena", objective, emptyList(), "quadrinho", "Quadrinho da maçã",
                listOf(StoryDialogueLine("LEIA_TEACHER", "Vamos procurar a fruta?", null)),
                "Qual fruta apareceu?", "puzzle"),
            PuzzleStoryNode("puzzle", objective, emptyList(), "fruta", "2x2",
                setOf("TAP_SWAP", "DRAG"), "Monte a maçã.", "Você montou a maçã!", "palavra"),
            WordBuilderStoryNode("palavra", objective, emptyList(), "fruta", "MAÇÃ",
                listOf("M", "A", "Ç", "Ã", "B", "O"), listOf("MA", "ÇÃ"), "eme",
                "mmm, maçã", "Forme MAÇÃ.", "Você formou MAÇÃ.", "dupla"),
            GroupHandoffStoryNode("dupla", objective, emptyList(),
                "Conte à dupla como descobriu a fruta.", "fim"),
            EndStoryNode("fim", objective, emptyList(), "Você ajudou a LÉIA!")
        )
        val pack = LearningStoryPack("1.0", "pack_test_maca", "story_test_maca", 1, 21,
            "A maçã da LÉIA", "LEIA", objective, "cena", nodes, emptyList(),
            48, true, true, true, emptyList())
        return PreparedAssignedStory("assignment_test_maca", pack,
            mapOf("quadrinho" to image, "fruta" to image), resumeNodeId)
    }

    private fun ballStory(image: File): PreparedAssignedStory {
        val objective = listOf("inferir_bola")
        val nodes = listOf(
            ComicStoryNode("cena", objective, emptyList(), "quadrinho", "Quadrinho da bola",
                listOf(StoryDialogueLine("LEIA_TEACHER",
                    "A bola sumiu perto da árvore.", null)),
                "O que precisamos encontrar?", "puzzle"),
            PuzzleStoryNode("puzzle", objective, emptyList(), "objeto", "2x2",
                setOf("TAP_SWAP", "DRAG"), "Monte a bola.", "Você montou a bola!", "palavra"),
            WordBuilderStoryNode("palavra", objective, emptyList(), "objeto", "BOLA",
                listOf("B", "O", "L", "A", "M", "P"), listOf("BO", "LA"), "bê",
                "b, b, bola", "Forme BOLA.", "Você formou BOLA.", "dupla"),
            GroupHandoffStoryNode("dupla", objective, emptyList(),
                "Conte à dupla qual pista ajudou a encontrar a bola.", "fim"),
            EndStoryNode("fim", objective, emptyList(), "Você ajudou a encontrar a bola!")
        )
        val pack = LearningStoryPack("1.0", "pack_test_bola", "story_test_bola", 1, 21,
            "A bola no recreio", "LEIA", objective, "cena", nodes, emptyList(),
            48, true, true, true, emptyList())
        return PreparedAssignedStory("assignment_test_bola", pack,
            mapOf("quadrinho" to image, "objeto" to image))
    }
}
