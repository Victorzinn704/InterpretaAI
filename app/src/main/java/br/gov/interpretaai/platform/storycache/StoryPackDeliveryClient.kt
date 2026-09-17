package br.gov.interpretaai.platform.storycache

import br.gov.interpretaai.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

data class DownloadedStoryPack(
    val assignmentId: String,
    val storyId: String,
    val version: Int,
    val rawJson: String,
    val sha256: String
)

sealed interface StoryPackDeliveryResult {
    data class Page(val packs: List<DownloadedStoryPack>, val nextCursor: String) : StoryPackDeliveryResult
    data object NoCredential : StoryPackDeliveryResult
    data object Unauthorized : StoryPackDeliveryResult
    data object RetryableFailure : StoryPackDeliveryResult
    data class Blocked(val code: String) : StoryPackDeliveryResult
}

/** Network-only v2 client. It never follows a manifest URL to a different origin. */
class StoryPackDeliveryClient(
    private val http: OkHttpClient = sharedHttp,
    private val appVersion: Int = BuildConfig.VERSION_CODE
) {
    suspend fun fetchPage(
        credential: PairedDeviceCredential?,
        cursor: String?
    ): StoryPackDeliveryResult {
        if (credential == null) return StoryPackDeliveryResult.NoCredential
        if (!credential.valid()) return StoryPackDeliveryResult.Blocked("device_credential_invalid")
        if (cursor != null && !CURSOR.matches(cursor)) {
            return StoryPackDeliveryResult.Blocked("manifest_cursor_invalid")
        }
        val root = runCatching { credential.baseUrl.trimEnd('/').toHttpUrl() }
            .getOrElse { return StoryPackDeliveryResult.Blocked("server_url_invalid") }
        val manifestUrl = root.newBuilder()
            .addPathSegments("api/v2/devices/${credential.deviceId}/manifest")
            .apply { cursor?.let { addQueryParameter("after", it) } }
            .build()
        return when (val manifest = execute(Request.Builder().url(manifestUrl)
            .header("Authorization", "Bearer ${credential.deviceToken}").get().build())) {
            is HttpResult.Failure -> StoryPackDeliveryResult.RetryableFailure
            is HttpResult.Response -> when (manifest.code) {
                200 -> downloadPage(credential, root, manifest.body)
                401, 403 -> StoryPackDeliveryResult.Unauthorized
                429, 503, 504 -> StoryPackDeliveryResult.RetryableFailure
                else -> StoryPackDeliveryResult.Blocked("manifest_http_${manifest.code}")
            }
        }
    }

    private suspend fun downloadPage(
        credential: PairedDeviceCredential,
        root: okhttp3.HttpUrl,
        body: String
    ): StoryPackDeliveryResult {
        val manifest = parseManifest(body) ?: return StoryPackDeliveryResult.Blocked("manifest_invalid")
        val downloaded = ArrayList<DownloadedStoryPack>(manifest.items.size)
        for (item in manifest.items) {
            if (item.minAppVersion > appVersion) continue
            val url = root.newBuilder()
                .addPathSegments("api/v2/devices/${credential.deviceId}/assignments/${item.assignmentId}/pack")
                .build()
            when (val pack = execute(Request.Builder().url(url)
                .header("Authorization", "Bearer ${credential.deviceToken}").get().build())) {
                is HttpResult.Failure -> return StoryPackDeliveryResult.RetryableFailure
                is HttpResult.Response -> when (pack.code) {
                    200 -> {
                        if (pack.body.toByteArray(Charsets.UTF_8).size != item.bytes
                            || pack.etag != "\"${item.sha256}\""
                            || sha256(pack.body) != item.sha256) {
                            return StoryPackDeliveryResult.Blocked("pack_integrity_invalid")
                        }
                        downloaded += DownloadedStoryPack(
                            item.assignmentId, item.storyId, item.version, pack.body, item.sha256
                        )
                    }
                    401, 403 -> return StoryPackDeliveryResult.Unauthorized
                    429, 503, 504 -> return StoryPackDeliveryResult.RetryableFailure
                    else -> return StoryPackDeliveryResult.Blocked("pack_http_${pack.code}")
                }
            }
        }
        return StoryPackDeliveryResult.Page(downloaded, manifest.nextCursor)
    }

    private fun parseManifest(body: String): Manifest? = runCatching {
        val json = JSONObject(body)
        val nextCursor = json.getString("nextCursor").takeIf(CURSOR::matches)
            ?: return null
        val items = json.getJSONArray("items")
        if (items.length() !in 0..MAX_ITEMS) return null
        List(items.length()) { index -> parseItem(items.getJSONObject(index)) ?: return null }
            .let { Manifest(it, nextCursor) }
    }.getOrNull()

    private fun parseItem(json: JSONObject): ManifestItem? {
        val assignmentId = json.optString("assignmentId")
        val storyId = json.optString("storyId")
        val version = json.optInt("storyVersion", 0)
        val minAppVersion = json.optInt("minAppVersion", 0)
        val hash = json.optString("packSha256")
        val bytes = json.optLong("bytes", -1)
        return if (ID.matches(assignmentId) && ID.matches(storyId) && version >= 1
            && minAppVersion >= 1 && SHA256.matches(hash) && bytes in 1..MAX_PACK_BYTES) {
            ManifestItem(assignmentId, storyId, version, minAppVersion, hash, bytes.toInt())
        } else null
    }

    private suspend fun execute(request: Request): HttpResult = suspendCancellableCoroutine { continuation ->
        val call = http.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                if (continuation.isActive) continuation.resume(HttpResult.Failure)
            }

            override fun onResponse(call: Call, response: Response) {
                val result = response.use {
                    HttpResult.Response(it.code, it.header("ETag"), it.body?.string().orEmpty())
                }
                if (continuation.isActive) continuation.resume(result)
            }
        })
    }

    private data class Manifest(val items: List<ManifestItem>, val nextCursor: String)
    private data class ManifestItem(
        val assignmentId: String,
        val storyId: String,
        val version: Int,
        val minAppVersion: Int,
        val sha256: String,
        val bytes: Int
    )

    private sealed interface HttpResult {
        data object Failure : HttpResult
        data class Response(val code: Int, val etag: String?, val body: String) : HttpResult
    }

    private companion object {
        const val MAX_ITEMS = 50
        const val MAX_PACK_BYTES = 262_144L
        val ID = Regex("[a-z0-9][a-z0-9_-]{2,63}")
        val SHA256 = Regex("[a-f0-9]{64}")
        val CURSOR = Regex("d1\\.[0-9]{1,18}")
        val sharedHttp = OkHttpClient.Builder()
            .retryOnConnectionFailure(false)
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .build()

        fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}
