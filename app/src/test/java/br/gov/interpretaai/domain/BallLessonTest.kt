package br.gov.interpretaai.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BallLessonTest {
    @Test fun recognizesTargetAsAWholeWord() {
        listOf("bola", "A bola!", "eu acho que falta uma BOLA").forEach {
            assertEquals(BallAnswer.BALL, BallAnswerResolver.resolve(it))
        }
        assertEquals(BallAnswer.OTHER, BallAnswerResolver.resolve("bolacha"))
        assertEquals(BallAnswer.EMPTY, BallAnswerResolver.resolve("  "))
    }

    @Test fun guidanceStartsWithVoiceAndThenBecomesVisual() {
        assertEquals(PuzzleGuidanceCue.NONE, PuzzleGuidancePolicy.cue(14_999, false))
        assertEquals(PuzzleGuidanceCue.VOICE_ONCE, PuzzleGuidancePolicy.cue(15_000, false))
        assertEquals(PuzzleGuidanceCue.NONE, PuzzleGuidancePolicy.cue(20_000, true))
        assertEquals(PuzzleGuidanceCue.VISUAL, PuzzleGuidancePolicy.cue(30_000, true))
    }

    @Test fun recognizesTheTreeAsAContextClue() {
        listOf("atrás da árvore", "perto do tronco", "na ARVORE").forEach {
            assertEquals(BallClueAnswer.TREE, BallClueAnswerResolver.resolve(it))
        }
        assertEquals(BallClueAnswer.OTHER, BallClueAnswerResolver.resolve("na mochila"))
        assertEquals(BallClueAnswer.EMPTY, BallClueAnswerResolver.resolve(" "))
    }

    @Test fun recognizesAnInstructionThatConnectsObjectAndPlace() {
        assertEquals(
            BallInstruction.COMPLETE,
            BallInstructionResolver.resolve("Davi, procure a bola atrás da árvore")
        )
        assertEquals(BallInstruction.PARTIAL, BallInstructionResolver.resolve("procure a bola"))
        assertEquals(BallInstruction.OTHER, BallInstructionResolver.resolve("vamos brincar"))
        assertEquals(BallInstruction.EMPTY, BallInstructionResolver.resolve(""))
    }
}
