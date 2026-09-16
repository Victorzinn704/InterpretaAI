package br.gov.interpretaai.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassroomModelsTest {
    @Test fun everyPublishableMissionExplainsItsPedagogicalPurpose() {
        AssignedActivity.entries.forEach { activity ->
            assertTrue(activity.supportRange.isNotBlank())
            assertTrue(activity.pedagogicalFocus.isNotBlank())
            assertTrue(activity.teacherEvidence.isNotBlank())
            assertTrue(activity.bnccReferences.startsWith("EF"))
        }
    }

    @Test fun secondThroughFifthYearPacksGiveEvidenceBasedFeedback() {
        ReadingMissionPack.entries.forEach { pack ->
            assertTrue(pack.carriesEvidence(0))
            assertTrue(pack.evidenceReply.isNotBlank())
            assertTrue(pack.reflectionReply.isNotBlank())
            assertTrue(pack.groupPrompt.endsWith("?"))
        }
        assertTrue(ReadingMissionPack.STORY_SEQUENCE.replyFor(1).contains("depois"))
        assertTrue(ReadingMissionPack.CAUSE_AND_EFFECT.replyFor(1).contains("resultado"))
        assertTrue(ReadingMissionPack.FACT_OR_OPINION.replyFor(1).contains("opinião"))
        assertTrue(ReadingMissionPack.COMPARE_SOURCES.replyFor(1).contains("conferir"))
        assertEquals(4, AssignedActivity.entries.count { it.readingPack != null })
    }

    @Test fun unknownAvatarFallsBackWithoutExposingIdentity() {
        assertEquals("sol", LearnerAvatars.find("nome-real-invalido").id)
    }

    @Test fun roomParticipantKeepsAvatarAliasAndDeviceConsistent() {
        assertEquals(
            "pipa-07",
            PilotRoomParticipant("pipa-07", LearnerAvatars.find("pipa"), "tablet-room-01").learnerAlias
        )
        assertThrows(IllegalArgumentException::class.java) {
            PilotRoomParticipant("sol-07", LearnerAvatars.find("pipa"), "tablet-room-01")
        }
        assertThrows(IllegalArgumentException::class.java) {
            PilotRoomParticipant("pipa-07", LearnerAvatars.find("pipa"), "x")
        }
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
