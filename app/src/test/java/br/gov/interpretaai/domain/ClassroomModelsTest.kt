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
            ClassroomAssignment("x".repeat(31), LearnerAvatars.find("sol"), AssignedActivity.COMIC, DrawingPrompt.BALL)
        }
    }
}
