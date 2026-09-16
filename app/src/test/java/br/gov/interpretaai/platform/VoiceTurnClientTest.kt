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

        val ack = progress.first() as VoiceTurnProgress.Ack
        assertEquals(1, ack.serverElapsedMs)
        assertTrue(ack.clientElapsedMs >= 0)
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

    @Test fun sendsAdultConfiguredDeviceTokenWithoutPuttingItInTheBody() = runBlocking {
        server.enqueue(MockResponse()
            .setHeader("Content-Type", "application/x-ndjson")
            .setBody(streamBody()))
        val client = VoiceTurnClient(
            server.url("/").toString(), testHttp(), deviceToken = { "device-secret-123456" }
        )

        client.send("session", "scene", 1, "Uma bola", false)
        val request = server.takeRequest()

        assertEquals("device-secret-123456", request.getHeader("X-Device-Token"))
        assertFalse(request.body.readUtf8().contains("device-secret"))
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

        client.send("session", "scene", 2, "Perto da árvore", false)
        assertEquals("/api/v1/voice-turn", server.takeRequest().path)
        assertEquals(3, server.requestCount)
    }

    @Test fun totalBudgetIncludesTransportRetries() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.NO_RESPONSE))
        val started = System.nanoTime()
        val client = VoiceTurnClient(server.url("/").toString(), testHttp(), totalBudgetMs = 150)

        val result = client.send("session", "scene", 1, "Uma bola", false)
        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)

        assertTrue(result.degraded)
        assertTrue("elapsed=$elapsedMs", elapsedMs < 600)
        assertEquals(1, server.requestCount)
    }

    @Test fun totalBudgetKeepsValidatedTextWhenAudioStalls() = runBlocking {
        val prefix = """
            {"protocolVersion":1,"type":"ACK","serverElapsedMs":1,"response":null}
            {"protocolVersion":1,"type":"FINAL_TEXT","serverElapsedMs":35,"response":{"replyText":"Sua pista já vale!","speaker":"LEIA_FEMALE","audioBase64":"","audioMimeType":"","visualReaction":"CURIOUS","nextAction":"SPEAK_AGAIN","observationCategory":"CONTEXT_REASONING","degraded":false}}
        """.trimIndent() + "\n"
        server.enqueue(MockResponse()
            .setHeader("Content-Type", "application/x-ndjson")
            .setChunkedBody(prefix + " ".repeat(8_000), 256)
            .throttleBody(512, 100, TimeUnit.MILLISECONDS))
        val client = VoiceTurnClient(server.url("/").toString(), testHttp(), totalBudgetMs = 250)

        val result = client.send("session", "gallery-1", 1, "Uma bola", false)

        assertEquals("Sua pista já vale!", result.replyText)
        assertTrue(result.degraded)
        assertFalse(result.audioPending)
        assertEquals(1, server.requestCount)
    }

    @Test fun validatedTextUsesLocalVoiceAfterShortAudioGrace() = runBlocking {
        val prefix = """
            {"protocolVersion":1,"type":"ACK","serverElapsedMs":1,"response":null}
            {"protocolVersion":1,"type":"FINAL_TEXT","serverElapsedMs":35,"response":{"replyText":"Sua pista já vale!","speaker":"LEIA_FEMALE","audioBase64":"","audioMimeType":"","visualReaction":"CURIOUS","nextAction":"SPEAK_AGAIN","observationCategory":"CONTEXT_REASONING","degraded":false}}
        """.trimIndent() + "\n"
        server.enqueue(MockResponse()
            .setHeader("Content-Type", "application/x-ndjson")
            .setChunkedBody(prefix + " ".repeat(8_000), 256)
            .throttleBody(512, 100, TimeUnit.MILLISECONDS))
        val client = VoiceTurnClient(
            server.url("/").toString(), testHttp(), totalBudgetMs = 3_000, audioGraceMs = 90
        )
        val started = System.nanoTime()

        val result = client.send("session", "gallery-1", 1, "Uma bola", false)
        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)

        assertEquals("Sua pista já vale!", result.replyText)
        assertTrue(result.degraded)
        assertFalse(result.audioPending)
        assertTrue("elapsed=$elapsedMs", elapsedMs < 900)
        assertEquals(1, server.requestCount)
    }

    @Test fun keepsValidatedTextWhenConnectionEndsBeforeAudio() = runBlocking {
        server.enqueue(MockResponse()
            .setHeader("Content-Type", "application/x-ndjson")
            .setBody("""
                {"type":"ACK","response":null}
                {"type":"FINAL_TEXT","response":{"replyText":"Sua pista já vale!","speaker":"LEIA_FEMALE","audioBase64":"","audioMimeType":"","visualReaction":"CURIOUS","nextAction":"SPEAK_AGAIN","observationCategory":"CONTEXT_REASONING","degraded":false}}
            """.trimIndent() + "\n"))
        val client = VoiceTurnClient(server.url("/").toString(), testHttp())

        val result = client.send("session", "gallery-1", 1, "Procurar juntos", false)

        assertEquals("Sua pista já vale!", result.replyText)
        assertTrue(result.degraded)
        assertFalse(result.audioPending)
        assertEquals(1, server.requestCount)
    }

    @Test fun offlineModeKeepsSceneSpecificPedagogicalPrompt() = runBlocking {
        val client = VoiceTurnClient("", testHttp())

        val result = client.send("session", "gallery-5", 1, "Ônibus", false)

        assertTrue(result.replyText.contains("Davi"))
        assertTrue(result.replyText.contains("chegou"))
        assertTrue(result.degraded)
        assertEquals(0, server.requestCount)
    }

    private fun testHttp() = OkHttpClient.Builder()
        .retryOnConnectionFailure(false)
        .build()

    private fun streamBody() = """
        {"protocolVersion":1,"type":"ACK","serverElapsedMs":1,"response":null}
        {"protocolVersion":1,"type":"FINAL_TEXT","serverElapsedMs":35,"response":{"replyText":"Sua ideia ajudou!","speaker":"LEIA_FEMALE","audioBase64":"","audioMimeType":"","visualReaction":"ENCOURAGE","nextAction":"SPEAK_AGAIN","observationCategory":"ORAL_EXPRESSION","degraded":false}}
        {"protocolVersion":1,"type":"COMPLETE","serverElapsedMs":80,"response":{"replyText":"Sua ideia ajudou!","speaker":"LEIA_FEMALE","audioBase64":"YXVkaW8=","audioMimeType":"audio/ogg","visualReaction":"ENCOURAGE","nextAction":"SPEAK_AGAIN","observationCategory":"ORAL_EXPRESSION","degraded":false}}
    """.trimIndent() + "\n"

    private fun completeBody() = """
        {"replyText":"Sua ideia ajudou!","speaker":"LEIA_FEMALE","audioBase64":"YXVkaW8=","audioMimeType":"audio/ogg","visualReaction":"ENCOURAGE","nextAction":"SPEAK_AGAIN","observationCategory":"ORAL_EXPRESSION","degraded":false}
    """.trimIndent()
}
