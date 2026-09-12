package br.gov.interpretaai.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComicStoriesTest {
    @Test
    fun everySceneCanBeNarratedAndContributesToTheGroupPath() {
        assertEquals(5, ComicStories.scenes.size)

        ComicStories.scenes.forEach { scene ->
            assertTrue(scene.dialogue.size >= 2)
            assertTrue(scene.narration.contains(scene.title))
            assertEquals(2, scene.choices.size)
            assertTrue(scene.choices.all { it.pathSummary.isNotBlank() })
        }
    }

    @Test
    fun interpretationChoicesDoNotLabelAChildAsWrong() {
        ComicStories.scenes
            .flatMap { it.choices }
            .forEach { choice ->
                assertTrue(!choice.reply.contains("errado", ignoreCase = true))
            }
    }
}
