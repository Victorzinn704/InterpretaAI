package br.gov.interpretaai.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ClassroomModelsTest {
    @Test fun unknownAvatarFallsBackWithoutExposingIdentity() {
        assertEquals("sol", LearnerAvatars.find("nome-real-invalido").id)
    }

    @Test fun classroomLabelIsBounded() {
        assertThrows(IllegalArgumentException::class.java) {
            ClassroomAssignment(
                "x".repeat(31), LearnerAvatars.find("sol"), AssignedActivity.COMIC,
                DrawingPrompt.BALL, "sol-01"
            )
        }
    }

    @Test fun learnerAliasIsPseudonymousAndBounded() {
        assertThrows(IllegalArgumentException::class.java) {
            ClassroomAssignment(
                "Turma 1A", LearnerAvatars.find("sol"), AssignedActivity.COMIC,
                DrawingPrompt.BALL, "ana-07"
            )
        }
        assertEquals(
            "sol-07",
            ClassroomAssignment(
                "Turma 1A", LearnerAvatars.find("sol"), AssignedActivity.COMIC,
                DrawingPrompt.BALL, "sol-07"
            ).learnerAlias
        )
        assertThrows(IllegalArgumentException::class.java) {
            ClassroomAssignment(
                "Turma 1A", LearnerAvatars.find("sol"), AssignedActivity.COMIC,
                DrawingPrompt.BALL, "pipa-07"
            )
        }
    }
}
