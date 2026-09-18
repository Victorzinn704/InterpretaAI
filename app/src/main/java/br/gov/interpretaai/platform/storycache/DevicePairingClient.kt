package br.gov.interpretaai.platform.storycache

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

data class PairingDeviceProfile(
    val installationId: String,
    val appVersion: Int,
    val architecture: String,
    val viewportWidthDp: Int,
    val viewportHeightDp: Int
) {
    fun valid(): Boolean = INSTALLATION.matches(installationId) && appVersion >= 1
        && architecture in setOf("ARM64", "UNIVERSAL", "UNKNOWN")
        && viewportWidthDp in 240..2000 && viewportHeightDp in 320..3000

    private companion object {
        val INSTALLATION = Regex("[A-Za-z0-9._:-]{16,128}")
    }
}

sealed interface DevicePairingResult {
    data class Paired(
        val credential: PairedDeviceCredential,
        val schoolId: String,
        val classroomId: String
    ) : DevicePairingResult
    data object InvalidCode : DevicePairingResult
    data object InstallationAlreadyPaired : DevicePairingResult
    data object RetryableFailure : DevicePairingResult
    data class Blocked(val code: String) : DevicePairingResult
}

/** Adult-only bootstrap. The one-time secret is never logged or retained after redemption. */
class DevicePairingClient(
    private val http: OkHttpClient = defaultHttp
) {
    suspend fun redeem(
        baseUrl: String,
        rawCode: String,
        profile: PairingDeviceProfile
    ): DevicePairingResult = withContext(Dispatchers.IO) {
        val code = normalizeCode(rawCode)
            ?: return@withContext DevicePairingResult.Blocked("pairing_code_format_invalid")
        if (!profile.valid()) {
            return@withContext DevicePairingResult.Blocked("device_profile_invalid")
        }
        val root = allowedRoot(baseUrl)
            ?: return@withContext DevicePairingResult.Blocked("server_url_invalid")
        val url = root.newBuilder().addPathSegments("api/v2/device-pairings/redeem").build()
        val payload = JSONObject()
            .put("code", code)
            .put("installationId", profile.installationId)
            .put("appVersion", profile.appVersion)
            .put("architecture", profile.architecture)
            .put("viewportWidthDp", profile.viewportWidthDp)
            .put("viewportHeightDp", profile.viewportHeightDp)
            .toString().toRequestBody(JSON)
        val request = Request.Builder().url(url).post(payload).build()
        val response = try {
            http.newCall(request).execute()
        } catch (_: IOException) {
            return@withContext DevicePairingResult.RetryableFailure
        }
        response.use {
            when (it.code) {
                201 -> {
                    val bytes = it.body?.byteStream()?.use(::boundedBytes)
                        ?: return@withContext DevicePairingResult.Blocked("pairing_response_invalid")
                    if (bytes.size > MAX_RESPONSE_BYTES) {
                        return@withContext DevicePairingResult.Blocked("pairing_response_too_large")
                    }
                    parseCredential(root, bytes)
                }
                400 -> DevicePairingResult.InvalidCode
                409 -> DevicePairingResult.InstallationAlreadyPaired
                429, 503, 504 -> DevicePairingResult.RetryableFailure
                else -> DevicePairingResult.Blocked("pairing_http_${it.code}")
            }
        }
    }

    private fun parseCredential(root: HttpUrl, bytes: ByteArray): DevicePairingResult = runCatching {
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        val credential = PairedDeviceCredential(
            root.toString().trimEnd('/'),
            json.getString("deviceId"),
            json.getString("deviceToken")
        )
        val schoolId = json.getString("schoolId")
        val classroomId = json.getString("classroomId")
        if (!credential.valid() || !ID.matches(schoolId) || !ID.matches(classroomId)) {
            return@runCatching DevicePairingResult.Blocked("pairing_credential_invalid")
        }
        DevicePairingResult.Paired(credential, schoolId, classroomId)
    }.getOrElse { DevicePairingResult.Blocked("pairing_response_invalid") }

    private fun boundedBytes(input: java.io.InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > MAX_RESPONSE_BYTES) return ByteArray(MAX_RESPONSE_BYTES + 1)
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun allowedRoot(value: String): HttpUrl? = runCatching {
        if (value != value.trim()) return null
        value.trimEnd('/').toHttpUrl().takeIf { url ->
            url.username.isEmpty() && url.password.isEmpty()
                && url.encodedPath == "/" && url.query == null && url.fragment == null
                && (url.isHttps || (!url.isHttps && url.host in LOOPBACK_HOSTS))
        }
    }.getOrNull()

    private fun normalizeCode(raw: String): String? {
        val compact = raw.uppercase().filterNot(Char::isWhitespace).replace("-", "")
        if (!COMPACT_CODE.matches(compact)) return null
        return compact.take(4) + "-" + compact.drop(4)
    }

    private companion object {
        const val MAX_RESPONSE_BYTES = 4_096
        val JSON = "application/json; charset=utf-8".toMediaType()
        val ID = Regex("[a-z0-9][a-z0-9_-]{2,63}")
        val COMPACT_CODE = Regex("[23456789A-HJ-NP-Z]{8}")
        val LOOPBACK_HOSTS = setOf("localhost", "127.0.0.1")
        val defaultHttp = OkHttpClient.Builder()
            .followRedirects(false)
            .retryOnConnectionFailure(false)
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(8, TimeUnit.SECONDS)
            .build()
    }
}
