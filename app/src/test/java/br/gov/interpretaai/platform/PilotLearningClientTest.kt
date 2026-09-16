package br.gov.interpretaai.platform

import br.gov.interpretaai.domain.EventType
import br.gov.interpretaai.domain.LearningEvent
import br.gov.interpretaai.domain.ResponseModality
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PilotLearningClientTest {
    private lateinit var server: MockWebServer

    @Before fun start() {
        server = MockWebServer()
        server.start()
    }

    @After fun stop() = server.shutdown()

    @Test fun sendsClosedEventWithoutAliasFreeTextOrClassroomName() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"accepted\":1,\"duplicates\":0}"))
        val client = PilotLearningClient(server.url("/").toString(), OkHttpClient())
        val event = LearningEvent(
            eventId = "event-response-0001",
            type = EventType.RESPONSE_SUBMITTED,
            childAlias = "pipa-07",
            classroom = "Turma 1A",
            activity = "comic-ball",
            value = "a fala completa nunca deve sair",
            durationMs = 1_200,
            modality = ResponseModality.VOICE,
            occurredAt = 1_789_499_200_000
        )

        assertTrue(client.send("tablet-room-01", "device-secret-123456", listOf(event)))
        val request = server.takeRequest()
        val body = request.body.readUtf8()

        assertTrue(body.contains("\"eventId\":\"event-response-0001\""))
        assertTrue(body.contains("\"observationCategory\":\"ORAL_EXPRESSION\""))
        assertTrue(body.contains("\"durationMs\":1200"))
        assertFalse(body.contains("pipa-07"))
        assertFalse(body.contains("Turma 1A"))
        assertFalse(body.contains("a fala completa"))
        assertFalse(body.contains("value"))
    }

    @Test fun refusesInsecureRemoteEndpointBeforeSending() = runBlocking {
        val client = PilotLearningClient("http://example.org", OkHttpClient())

        assertFalse(client.send(
            "tablet-room-01", "device-secret-123456",
            listOf(LearningEvent(type = EventType.SESSION_STARTED))
        ))
    }
}
