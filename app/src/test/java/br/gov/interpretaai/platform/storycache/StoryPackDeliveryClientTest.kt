package br.gov.interpretaai.platform.storycache

import br.gov.interpretaai.domain.StoryAssetRole
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
        assertEquals(80, result.packs.single().priority)
        assertEquals(1_789_603_200_000L, result.packs.single().expiresAtMs)

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

    @Test fun downloadsOnlyTheDeclaredVariantAndVerifiesItsTransportMetadata() = runBlocking {
        val bytes = "apple-png".toByteArray(Charsets.UTF_8)
        val hash = sha256(String(bytes, Charsets.UTF_8))
        server.enqueue(MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "image/png")
            .setHeader("ETag", "\"$hash\"")
            .setBody(okio.Buffer().write(bytes)))

        val result = StoryPackDeliveryClient(OkHttpClient(), appVersion = 21).downloadAsset(
            credential(), "assignment_bola_001", StoryPackAssetDownload(
                "maca_objeto", StoryAssetRole.PHONE, "image/png", bytes.size.toLong(), hash
            )
        )

        assertTrue(result is StoryAssetDeliveryResult.Downloaded)
        assertTrue((result as StoryAssetDeliveryResult.Downloaded).bytes.contentEquals(bytes))
        val request = server.takeRequest()
        assertEquals(
            "/api/v2/devices/device_demo_001/assignments/assignment_bola_001/assets/maca_objeto/PHONE",
            request.path
        )
    }

    @Test fun blocksADeclaredVariantWhenTheServerMetadataDoesNotMatch() = runBlocking {
        val bytes = "apple-png".toByteArray(Charsets.UTF_8)
        val hash = sha256(String(bytes, Charsets.UTF_8))
        server.enqueue(MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "image/webp")
            .setHeader("ETag", "\"$hash\"")
            .setBody(okio.Buffer().write(bytes)))

        val result = StoryPackDeliveryClient(OkHttpClient(), appVersion = 21).downloadAsset(
            credential(), "assignment_bola_001", StoryPackAssetDownload(
                "maca_objeto", StoryAssetRole.PHONE, "image/png", bytes.size.toLong(), hash
            )
        )

        assertEquals(StoryAssetDeliveryResult.Blocked("asset_integrity_invalid"), result)
    }

    @Test fun sendsOnlyTheVerifiedPackHashThroughTheAuthenticatedSameOriginReceipt() = runBlocking {
        val hash = "a".repeat(64)
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val result = StoryPackDeliveryClient(OkHttpClient()).confirmPrepared(
            credential(), PreparedPackReceipt("assignment_bola_001", hash)
        )

        assertEquals(StoryPreparationReceiptResult.Confirmed, result)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals(
            "/api/v2/devices/device_demo_001/assignments/assignment_bola_001/prepared",
            request.path
        )
        assertEquals("Bearer ${credential().deviceToken}", request.getHeader("Authorization"))
        assertEquals(hash, org.json.JSONObject(request.body.readUtf8()).getString("packSha256"))
    }

    @Test fun retryableReceiptDoesNotPretendThatTheTabletReportedPreparation() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))

        val result = StoryPackDeliveryClient(OkHttpClient()).confirmPrepared(
            credential(), PreparedPackReceipt("assignment_bola_001", "a".repeat(64))
        )

        assertEquals(StoryPreparationReceiptResult.RetryableFailure, result)
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
           "bytes":${rawPack.toByteArray(Charsets.UTF_8).size},
           "priority":80,"expiresAt":"2026-09-17T00:00:00Z"}
        ]}
    """.trimIndent()

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
