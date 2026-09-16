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
            assertTrue(activity.eventId.matches(Regex("[a-z0-9-]{3,64}")))
        }
    }

    @Test fun secondThroughFifthYearPacksGiveEvidenceBasedFeedback() {
        ReadingMissionPack.entries.forEach { pack ->
            assertTrue(pack.carriesEvidence(0))
            assertTrue(pack.evidenceReply.isNotBlank())
            assertTrue(pack.reflectionReply.isNotBlank())
            assertTrue(pack.groupPrompt.endsWith("?"))
            assertTrue(pack.completion.title.isNotBlank())
            assertTrue(pack.completion.summary.isNotBlank())
            assertTrue(pack.completion.offScreenPrompt.contains("tablet descansa"))
            assertTrue(pack.completion.spokenCelebration.startsWith("Parabéns"))
        }
        assertTrue(ReadingMissionPack.STORY_SEQUENCE.replyFor(1).contains("depois"))
        assertTrue(ReadingMissionPack.CAUSE_AND_EFFECT.replyFor(1).contains("resultado"))
        assertTrue(ReadingMissionPack.FACT_OR_OPINION.replyFor(1).contains("opinião"))
        assertTrue(ReadingMissionPack.COMPARE_SOURCES.replyFor(1).contains("conferir"))
        assertEquals(4, AssignedActivity.entries.count { it.readingPack != null })
        assertEquals(4, AssignedActivity.entries.count { it.completesOnReadingClosure })
        assertEquals(8, AssignedActivity.entries.map { it.eventId }.distinct().size)
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

    @Test fun sharedTabletKeepsEveryAvatarButUsesACollectiveAssignment() {
        val assignment = ClassroomAssignment(
            "Turma 1A", LearnerAvatars.find("pipa"), AssignedActivity.STORY_SEQUENCE_2,
            DrawingPrompt.BALL, "pipa-07",
            listOf(
                AssignedLearner("pipa-07", LearnerAvatars.find("pipa")),
                AssignedLearner("sol-08", LearnerAvatars.find("sol"))
            )
        )

        assertTrue(assignment.isSharedTablet)
        assertEquals(listOf("pipa-07", "sol-08"), assignment.members.map { it.learnerAlias })
    }

    @Test fun sharedTabletRotatesVisibleRolesWithoutExposingAliases() {
        val members = listOf(
            AssignedLearner("pipa-07", LearnerAvatars.find("pipa")),
            AssignedLearner("sol-08", LearnerAvatars.find("sol")),
            AssignedLearner("estrela-09", LearnerAvatars.find("estrela"))
        )

        val observe = CollaborativeTurnPlanner.turn(members, CollaborativeMoment.OBSERVE)!!
        val respond = CollaborativeTurnPlanner.turn(members, CollaborativeMoment.RESPOND)!!
        val build = CollaborativeTurnPlanner.turn(members, CollaborativeMoment.BUILD)!!

        assertEquals("pipa", observe.lead.avatar.id)
        assertEquals("sol", respond.lead.avatar.id)
        assertEquals("estrela", build.lead.avatar.id)
        assertTrue(observe.visiblePrompt.contains("🪁"))
        assertTrue(observe.spokenPrompt.contains("Pipa"))
        members.forEach { member ->
            assertTrue(!observe.visiblePrompt.contains(member.learnerAlias))
            assertTrue(!observe.spokenPrompt.contains(member.learnerAlias))
        }
        assertEquals(null, CollaborativeTurnPlanner.turn(members.take(1), CollaborativeMoment.OBSERVE))
    }
}
