package br.gov.interpretaai.platform

import br.gov.interpretaai.domain.AssignedActivity
import br.gov.interpretaai.domain.DrawingPrompt
import br.gov.interpretaai.domain.LearnerAvatars
import br.gov.interpretaai.domain.PilotRoomParticipant
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PilotClassroomClientTest {
    private lateinit var server: MockWebServer

    @Before fun start() {
        server = MockWebServer()
        server.start()
    }

    @After fun stop() = server.shutdown()

    @Test fun savesRosterThenPublishesMissionToEveryone() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"targetCount\":2}"))
        val client = PilotClassroomClient(loopbackUrl(), OkHttpClient())

        val result = client.saveAndPublish(
            "turma-1a", "Turma 1A", "teacher-secret-12345", participants(),
            AssignedActivity.DRAWING, DrawingPrompt.TREE
        )
        val save = server.takeRequest()
        val publish = server.takeRequest()

        assertEquals(PilotClassroomResult.Published(2), result)
        assertEquals("PUT", save.method)
        assertEquals("/api/v1/pilot/classrooms/turma-1a", save.path)
        assertTrue(save.body.readUtf8().contains("\"learnerAlias\":\"pipa-07\""))
        assertEquals("POST", publish.method)
        assertEquals("/api/v1/pilot/classrooms/turma-1a/assignments", publish.path)
        assertTrue(publish.body.readUtf8().contains("\"learnerAliases\":[]"))
        assertEquals("teacher-secret-12345", publish.getHeader("X-Teacher-Token"))
    }

    @Test fun doesNotPublishWhenRosterIsRejected() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(409))
        val client = PilotClassroomClient(loopbackUrl(), OkHttpClient())

        val result = client.saveAndPublish(
            "turma-1a", "Turma 1A", "teacher-secret-12345", participants(),
            AssignedActivity.COMIC, DrawingPrompt.BALL
        )

        assertEquals(PilotClassroomResult.Failed("Um tablet já pertence a outra sala."), result)
        assertEquals(1, server.requestCount)
    }

    @Test fun publishesOnlyTheTeacherSelectedGroup() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"targetCount\":1}"))
        val client = PilotClassroomClient(loopbackUrl(), OkHttpClient())

        val result = client.saveAndPublish(
            "turma-1a", "Turma 1A", "teacher-secret-12345", participants(),
            AssignedActivity.COMIC, DrawingPrompt.BALL, setOf("pipa-07")
        )
        server.takeRequest()
        val publish = server.takeRequest()

        assertEquals(PilotClassroomResult.Published(1), result)
        assertTrue(publish.body.readUtf8().contains("\"learnerAliases\":[\"pipa-07\"]"))
    }

    @Test fun rejectsUnknownGroupMemberBeforeNetwork() = runBlocking {
        val client = PilotClassroomClient(loopbackUrl(), OkHttpClient())

        val result = client.saveAndPublish(
            "turma-1a", "Turma 1A", "teacher-secret-12345", participants(),
            AssignedActivity.COMIC, DrawingPrompt.BALL, setOf("estrela-99")
        )

        assertEquals(PilotClassroomResult.Failed("Seleção da sala inválida."), result)
        assertEquals(0, server.requestCount)
    }

    @Test fun allowsTwoAvatarsToShareOneTablet() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"targetCount\":1}"))
        val client = PilotClassroomClient(loopbackUrl(), OkHttpClient())
        val duplicate = listOf(
            PilotRoomParticipant("pipa-07", LearnerAvatars.find("pipa"), "tablet-room-01"),
            PilotRoomParticipant("sol-08", LearnerAvatars.find("sol"), "tablet-room-01")
        )

        val result = client.saveAndPublish(
            "turma-1a", "Turma 1A", "teacher-secret-12345", duplicate,
            AssignedActivity.COMIC, DrawingPrompt.BALL
        )

        assertEquals(PilotClassroomResult.Published(1), result)
        val saveBody = server.takeRequest().body.readUtf8()
        server.takeRequest()
        assertEquals(2, Regex("tablet-room-01").findAll(saveBody).count())
    }

    @Test fun rejectsMoreThanFourAvatarsOnOneTabletBeforeNetwork() = runBlocking {
        val client = PilotClassroomClient(loopbackUrl(), OkHttpClient())
        val oversized = (1..5).map { slot ->
            PilotRoomParticipant("sol-0$slot", LearnerAvatars.find("sol"), "tablet-room-01")
        }

        val result = client.saveAndPublish(
            "turma-1a", "Turma 1A", "teacher-secret-12345", oversized,
            AssignedActivity.COMIC, DrawingPrompt.BALL
        )

        assertEquals(
            PilotClassroomResult.Failed("Cada tablet compartilhado aceita até quatro avatares."),
            result
        )
        assertEquals(0, server.requestCount)
    }

    private fun participants() = listOf(
        PilotRoomParticipant("pipa-07", LearnerAvatars.find("pipa"), "tablet-room-01"),
        PilotRoomParticipant("sol-08", LearnerAvatars.find("sol"), "tablet-room-02")
    )

    private fun loopbackUrl(): String = server.url("/").newBuilder()
        .host("127.0.0.1")
        .build()
        .toString()
}
