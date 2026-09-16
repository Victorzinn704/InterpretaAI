package br.gov.interpretaai.platform

import br.gov.interpretaai.domain.AssignedActivity
import br.gov.interpretaai.domain.ClassroomAssignment
import br.gov.interpretaai.domain.DrawingPrompt
import br.gov.interpretaai.domain.LearnerAvatars
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PilotAssignmentClientTest {
    private lateinit var server: MockWebServer

    @Before fun start() {
        server = MockWebServer()
        server.start()
    }

    @After fun stop() = server.shutdown()

    @Test fun fetchesOnlyAssignmentNewerThanLocalVersion() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(responseBody()))
        val client = PilotAssignmentClient(server.url("/").toString(), OkHttpClient())

        val result = client.fetch("tablet-001", "device-secret-123456", 4)
        val request = server.takeRequest()

        assertTrue(result is PilotSyncResult.Updated)
        result as PilotSyncResult.Updated
        assertEquals(5, result.version)
        assertEquals(AssignedActivity.DRAWING, result.assignment.activity)
        assertEquals(DrawingPrompt.TREE, result.assignment.drawingPrompt)
        assertEquals("pipa-07", result.assignment.learnerAlias)
        assertEquals("/api/v1/pilot/assignments/tablet-001?afterVersion=4", request.path)
        assertEquals("device-secret-123456", request.getHeader("X-Device-Token"))
        assertFalse(request.path!!.contains("secret"))
    }

    @Test fun noContentMeansNoChange() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))
        val client = PilotAssignmentClient(server.url("/").toString(), OkHttpClient())

        assertEquals(PilotSyncResult.NoChange, client.fetch("tablet-001", "token-123456789012", 5))
    }

    @Test fun derivesAliasWhenReadingResponseFromPreviousServerVersion() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(responseBody().replace(
            "\"learnerAlias\":\"pipa-07\",", ""
        )))
        val client = PilotAssignmentClient(server.url("/").toString(), OkHttpClient())

        val result = client.fetch("tablet-001", "device-secret-123456", 4)

        assertTrue(result is PilotSyncResult.Updated)
        result as PilotSyncResult.Updated
        assertEquals("pipa-01", result.assignment.learnerAlias)
    }

    @Test fun readsEveryAvatarAssignedToASharedTablet() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(responseBody().replace(
            "\"updatedAt\"", "\"members\":[{\"learnerAlias\":\"pipa-07\",\"avatarId\":\"pipa\"}," +
                "{\"learnerAlias\":\"sol-08\",\"avatarId\":\"sol\"}],\"updatedAt\""
        )))
        val client = PilotAssignmentClient(server.url("/").toString(), OkHttpClient())

        val result = client.fetch("tablet-001", "device-secret-123456", 4)

        assertTrue(result is PilotSyncResult.Updated)
        result as PilotSyncResult.Updated
        assertTrue(result.assignment.isSharedTablet)
        assertEquals(listOf("🪁", "☀️"), result.assignment.members.map { it.avatar.emoji })
        assertEquals(listOf("pipa-07", "sol-08"), result.assignment.members.map { it.learnerAlias })
    }

    @Test fun publishesClosedAssignmentWithoutIdentityField() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody(responseBody()))
        val client = PilotAssignmentClient(server.url("/").toString(), OkHttpClient())
        val assignment = ClassroomAssignment(
            "Turma 1A", LearnerAvatars.find("pipa"), AssignedActivity.DRAWING, DrawingPrompt.TREE,
            "pipa-07"
        )

        val result = client.publish("tablet-001", "teacher-secret-12345", assignment)
        val request = server.takeRequest()
        val body = request.body.readUtf8()

        assertTrue(result is PilotSyncResult.Updated)
        assertEquals("teacher-secret-12345", request.getHeader("X-Teacher-Token"))
        assertTrue(body.contains("\"avatarId\":\"pipa\""))
        assertTrue(body.contains("\"learnerAlias\":\"pipa-07\""))
        assertFalse(body.contains("studentName"))
        assertFalse(body.contains("transcript"))
    }

    private fun responseBody() = """
        {"deviceId":"tablet-001","version":5,"classroomLabel":"Turma 1A",
         "avatarId":"pipa","learnerAlias":"pipa-07","activity":"DRAWING","drawingPrompt":"TREE",
         "updatedAt":"2026-09-15T17:00:00Z"}
    """.trimIndent()
}
