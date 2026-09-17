package br.gov.interpretaai.platform.storycache

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
import java.security.MessageDigest

class StoryPackDeliveryClientTest {
    private lateinit var server: MockWebServer

    @Before fun start() {
        server = MockWebServer()
        server.start()
    }

    @After fun stop() = server.shutdown()

    @Test fun downloadsOnlyThroughAuthenticatedSameOriginRouteAndVerifiesPack() = runBlocking {
        val rawPack = "{\"packId\":\"pack_bola_001\"}"
        val hash = sha256(rawPack)
        server.enqueue(MockResponse().setResponseCode(200).setBody(manifest(rawPack, hash)))
        server.enqueue(MockResponse().setResponseCode(200).setHeader("ETag", "\"$hash\"")
            .setBody(rawPack))
        val credential = credential()

        val result = StoryPackDeliveryClient(OkHttpClient(), appVersion = 21)
            .fetchPage(credential, "d1.0")

        assertTrue(result is StoryPackDeliveryResult.Page)
        result as StoryPackDeliveryResult.Page
        assertEquals("d1.1", result.nextCursor)
        assertEquals(listOf("assignment_bola_001"), result.packs.map { it.assignmentId })
        assertEquals(rawPack, result.packs.single().rawJson)

        val manifestRequest = server.takeRequest()
        assertEquals("/api/v2/devices/device_demo_001/manifest?after=d1.0", manifestRequest.path)
        assertEquals("Bearer ${credential.deviceToken}", manifestRequest.getHeader("Authorization"))
        val packRequest = server.takeRequest()
        assertEquals(
            "/api/v2/devices/device_demo_001/assignments/assignment_bola_001/pack",
            packRequest.path
        )
        assertEquals("Bearer ${credential.deviceToken}", packRequest.getHeader("Authorization"))
    }

    @Test fun ignoresManifestDownloadUrlInsteadOfFollowingAnotherOrigin() = runBlocking {
        val rawPack = "{\"packId\":\"pack_bola_001\"}"
        val hash = sha256(rawPack)
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            manifest(rawPack, hash).replace(
                "\"bytes\"", "\"downloadUrl\":\"https://not-authorized.example/pack\",\"bytes\""
            )
        ))
        server.enqueue(MockResponse().setResponseCode(200).setHeader("ETag", "\"$hash\"")
            .setBody(rawPack))

        val result = StoryPackDeliveryClient(OkHttpClient(), appVersion = 21)
            .fetchPage(credential(), null)

        assertTrue(result is StoryPackDeliveryResult.Page)
        server.takeRequest()
        assertEquals(
            "/api/v2/devices/device_demo_001/assignments/assignment_bola_001/pack",
            server.takeRequest().path
        )
    }

    @Test fun rejectsPackWhenEtagOrHashDoesNotMatchManifest() = runBlocking {
        val rawPack = "{\"packId\":\"pack_bola_001\"}"
        val hash = sha256(rawPack)
        server.enqueue(MockResponse().setResponseCode(200).setBody(manifest(rawPack, hash)))
        server.enqueue(MockResponse().setResponseCode(200).setHeader("ETag", "\"${"0".repeat(64)}\"")
            .setBody(rawPack))

        val result = StoryPackDeliveryClient(OkHttpClient(), appVersion = 21)
            .fetchPage(credential(), null)

        assertEquals(StoryPackDeliveryResult.Blocked("pack_integrity_invalid"), result)
    }

    @Test fun asksWorkManagerToRetryWhenManifestIsTemporarilyUnavailable() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))

        val result = StoryPackDeliveryClient(OkHttpClient(), appVersion = 21)
            .fetchPage(credential(), null)

        assertEquals(StoryPackDeliveryResult.RetryableFailure, result)
    }

    @Test fun permitsOnlyHttpsOrAnExactLoopbackHostForTheTemporaryLocalServer() {
        val paired = credential()

        assertTrue(paired.valid())
        assertFalse(paired.copy(baseUrl = "http://localhost.evil.example").valid())
        assertFalse(paired.copy(baseUrl = "http://127.0.0.12").valid())
        assertFalse(paired.copy(baseUrl = "http://example.test").valid())
        assertTrue(paired.copy(baseUrl = "https://api.example.test").valid())
    }

    private fun credential() = PairedDeviceCredential(
        baseUrl = server.url("/").toString().trimEnd('/'),
        deviceId = "device_demo_001",
        deviceToken = "dvc.device_demo_001.${"a".repeat(64)}"
    )

    private fun manifest(rawPack: String, hash: String): String = """
        {"nextCursor":"d1.1","items":[
          {"assignmentId":"assignment_bola_001","storyId":"story_bola_001",
           "storyVersion":1,"minAppVersion":1,"packSha256":"$hash",
           "bytes":${rawPack.toByteArray(Charsets.UTF_8).size}}
        ]}
    """.trimIndent()

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
