package br.gov.interpretaai.platform.storycache

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DevicePairingClientTest {
    private lateinit var server: MockWebServer

    @Before fun start() {
        server = MockWebServer()
        server.start()
    }

    @After fun stop() = server.shutdown()

    @Test fun redeemsOneTimeCodeWithoutSendingAnyAdultToken() = runBlocking {
        val token = "dvc.device_demo_001.${"a".repeat(64)}"
        server.enqueue(MockResponse().setResponseCode(201).setBody("""
            {"deviceId":"device_demo_001","deviceToken":"$token",
             "schoolId":"school_centro","classroomId":"class_1a",
             "issuedAt":"2026-09-17T20:00:00Z"}
        """.trimIndent()))

        val result = client().redeem(server.url("/").toString(), "23456789", profile())

        assertTrue(result is DevicePairingResult.Paired)
        result as DevicePairingResult.Paired
        assertEquals("device_demo_001", result.credential.deviceId)
        assertEquals("school_centro", result.schoolId)
        assertEquals("class_1a", result.classroomId)
        val request = server.takeRequest()
        assertEquals("/api/v2/device-pairings/redeem", request.path)
        assertEquals(null, request.getHeader("Authorization"))
        val body = JSONObject(request.body.readUtf8())
        assertEquals("2345-6789", body.getString("code"))
        assertEquals("install-00000000-0000-0000-0000-000000000001",
            body.getString("installationId"))
        assertEquals(22, body.getInt("appVersion"))
        assertEquals("ARM64", body.getString("architecture"))
        assertEquals(800, body.getInt("viewportWidthDp"))
        assertEquals(1280, body.getInt("viewportHeightDp"))
    }

    @Test fun rejectsMalformedCodeBeforeNetwork() = runBlocking {
        val result = client().redeem(server.url("/").toString(), "1111-1111", profile())

        assertEquals(DevicePairingResult.Blocked("pairing_code_format_invalid"), result)
        assertEquals(0, server.requestCount)
    }

    @Test fun acceptsOnlyHttpsRootOrExactLoopbackRoot() = runBlocking {
        assertEquals(DevicePairingResult.Blocked("server_url_invalid"),
            client().redeem("http://example.test", "2345-6789", profile()))
        assertEquals(DevicePairingResult.Blocked("server_url_invalid"),
            client().redeem("https://example.test/prefix", "2345-6789", profile()))
        assertEquals(DevicePairingResult.Blocked("server_url_invalid"),
            client().redeem("http://localhost.evil.example", "2345-6789", profile()))
        assertEquals(0, server.requestCount)
    }

    @Test fun keepsInvalidExpiredAndUsedCodesInTheSameSafeOutcome() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(400))

        val result = client().redeem(server.url("/").toString(), "2345-6789", profile())

        assertEquals(DevicePairingResult.InvalidCode, result)
    }

    @Test fun doesNotFollowPairingRedirectsToAnotherOrigin() = runBlocking {
        val other = MockWebServer().also { it.start() }
        try {
            server.enqueue(MockResponse().setResponseCode(302)
                .setHeader("Location", other.url("/collect")))

            val result = client().redeem(server.url("/").toString(), "2345-6789", profile())

            assertEquals(DevicePairingResult.Blocked("pairing_http_302"), result)
            assertEquals(0, other.requestCount)
        } finally {
            other.shutdown()
        }
    }

    @Test fun mapsTemporaryCapacityFailureToRetryWithoutPersistingAnything() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))

        val result = client().redeem(server.url("/").toString(), "2345-6789", profile())

        assertEquals(DevicePairingResult.RetryableFailure, result)
    }

    private fun client() = DevicePairingClient(OkHttpClient.Builder()
        .followRedirects(false).build())

    private fun profile() = PairingDeviceProfile(
        installationId = "install-00000000-0000-0000-0000-000000000001",
        appVersion = 22,
        architecture = "ARM64",
        viewportWidthDp = 800,
        viewportHeightDp = 1280
    )
}
