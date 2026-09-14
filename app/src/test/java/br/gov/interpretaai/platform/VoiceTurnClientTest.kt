package br.gov.interpretaai.platform

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import java.util.concurrent.TimeUnit

class VoiceTurnClientTest {
    private lateinit var server: MockWebServer

    @Before fun start() {
        server = MockWebServer()
        server.start()
    }

    @After fun stop() {
        server.shutdown()
    }

    @Test fun exposesValidatedTextBeforeCompleteAudio() = runBlocking {
        server.enqueue(MockResponse()
            .setHeader("Content-Type", "application/x-ndjson")
            .setBody(streamBody()))
        val progress = mutableListOf<VoiceTurnProgress>()
        val client = VoiceTurnClient(server.url("/").toString(), testHttp())

        val result = client.send("session", "scene", 1, "Uma bola", false, progress::add)

        assertTrue(progress.first() is VoiceTurnProgress.Ack)
        val preview = (progress[1] as VoiceTurnProgress.FinalText).value
        assertTrue(preview.audioPending)
        assertEquals("Sua ideia ajudou!", preview.replyText)
        assertFalse(result.audioPending)
        assertEquals("audio", String(result.audio!!))
        assertEquals("/api/v1/voice-turn/stream", server.takeRequest().path)
    }

    @Test fun transientRetryReusesTheSameIdempotencyKey() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse()
            .setHeader("Content-Type", "application/x-ndjson")
            .setBody(streamBody()))
        val client = VoiceTurnClient(server.url("/").toString(), testHttp())

        val result = client.send("session", "scene", 1, "Uma bola", false)
        val first = server.takeRequest()
        val second = server.takeRequest()

        assertEquals("Sua ideia ajudou!", result.replyText)
        assertEquals(first.getHeader("Idempotency-Key"), second.getHeader("Idempotency-Key"))
    }

    @Test fun cancellationClosesTheInFlightStreamWithoutRetry() = runBlocking {
        server.enqueue(MockResponse()
            .setHeader("Content-Type", "application/x-ndjson")
            .setBody(streamBody())
            .setBodyDelay(5, TimeUnit.SECONDS))
        val client = VoiceTurnClient(server.url("/").toString(), testHttp())

        val job = launch { client.send("session", "scene", 1, "Uma bola", false) }
        server.takeRequest(1, TimeUnit.SECONDS)
        delay(30)
        job.cancelAndJoin()

        assertTrue(job.isCancelled)
        assertEquals(1, server.requestCount)
    }

    @Test fun fallsBackToLegacyContractWhenServerDoesNotSupportStreaming() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))
        server.enqueue(MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(completeBody()))
        val client = VoiceTurnClient(server.url("/").toString(), testHttp())

        val result = client.send("session", "scene", 1, "Uma bola", false)
        val streamRequest = server.takeRequest()
        val legacyRequest = server.takeRequest()

        assertEquals("Sua ideia ajudou!", result.replyText)
        assertEquals("/api/v1/voice-turn/stream", streamRequest.path)
        assertEquals("/api/v1/voice-turn", legacyRequest.path)
        assertEquals(
            streamRequest.getHeader("Idempotency-Key"),
            legacyRequest.getHeader("Idempotency-Key")
        )
    }

    private fun testHttp() = OkHttpClient.Builder()
        .retryOnConnectionFailure(false)
        .build()

    private fun streamBody() = """
        {"type":"ACK","response":null}
        {"type":"FINAL_TEXT","response":{"replyText":"Sua ideia ajudou!","speaker":"LEIA_FEMALE","audioBase64":"","audioMimeType":"","visualReaction":"ENCOURAGE","nextAction":"SPEAK_AGAIN","observationCategory":"ORAL_EXPRESSION","degraded":false}}
        {"type":"COMPLETE","response":{"replyText":"Sua ideia ajudou!","speaker":"LEIA_FEMALE","audioBase64":"YXVkaW8=","audioMimeType":"audio/ogg","visualReaction":"ENCOURAGE","nextAction":"SPEAK_AGAIN","observationCategory":"ORAL_EXPRESSION","degraded":false}}
    """.trimIndent() + "\n"

    private fun completeBody() = """
        {"replyText":"Sua ideia ajudou!","speaker":"LEIA_FEMALE","audioBase64":"YXVkaW8=","audioMimeType":"audio/ogg","visualReaction":"ENCOURAGE","nextAction":"SPEAK_AGAIN","observationCategory":"ORAL_EXPRESSION","degraded":false}
    """.trimIndent()
}
