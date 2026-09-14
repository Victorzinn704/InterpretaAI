package br.gov.interpretaai.platform

import br.gov.interpretaai.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.Base64
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.random.Random

data class VoiceTurnResult(
    val replyText: String,
    val audio: ByteArray? = null,
    val audioMimeType: String = "",
    val visualReaction: String = "ENCOURAGE",
    val nextAction: String = "SPEAK_AGAIN",
    val degraded: Boolean = false,
    val audioPending: Boolean = false
)

sealed interface VoiceTurnProgress {
    data object Ack : VoiceTurnProgress
    data class FinalText(val value: VoiceTurnResult) : VoiceTurnProgress
}

class VoiceTurnClient(
    private val baseUrl: String = BuildConfig.VOICE_API_URL,
    private val http: OkHttpClient = sharedHttp
) {
    fun warmup() {
        if (baseUrl.isBlank()) return
        try {
            val request = Request.Builder()
                .url("${baseUrl.trimEnd('/')}/api/v1/gateway/warmup")
                .post(ByteArray(0).toRequestBody())
                .build()
            http.newCall(request).apply {
                timeout().timeout(1_000, TimeUnit.MILLISECONDS)
            }.execute().close()
        } catch (_: Exception) {
            // Aquecimento é oportunista e nunca bloqueia a jornada local.
        }
    }

    suspend fun send(
        sessionId: String,
        sceneId: String,
        turn: Int,
        transcript: String,
        reducedStimuli: Boolean,
        onProgress: (VoiceTurnProgress) -> Unit = {}
    ): VoiceTurnResult {
        if (baseUrl.isBlank()) return offline()
        val idempotencyKey = UUID.randomUUID().toString()
        var useLegacyEndpoint = false
        repeat(2) { attempt ->
            val result = if (useLegacyEndpoint) {
                sendLegacy(sessionId, sceneId, turn, transcript, reducedStimuli, idempotencyKey)
            } else {
                sendOnce(
                    sessionId, sceneId, turn, transcript, reducedStimuli, idempotencyKey, onProgress
                )
            }
            when (result) {
                is AttemptResult.Success -> return result.value
                AttemptResult.Fatal -> return offline()
                AttemptResult.LegacyRequired -> {
                    useLegacyEndpoint = true
                    when (val legacy = sendLegacy(
                        sessionId, sceneId, turn, transcript, reducedStimuli, idempotencyKey
                    )) {
                        is AttemptResult.Success -> return legacy.value
                        AttemptResult.Fatal, AttemptResult.LegacyRequired -> return offline()
                        AttemptResult.Retryable -> if (attempt == 0) {
                            delay(Random.nextLong(80, 181))
                        }
                    }
                }
                AttemptResult.Retryable -> if (attempt == 0) {
                    delay(Random.nextLong(80, 181))
                }
            }
        }
        return offline()
    }

    private suspend fun sendOnce(
        sessionId: String,
        sceneId: String,
        turn: Int,
        transcript: String,
        reducedStimuli: Boolean,
        idempotencyKey: String,
        onProgress: (VoiceTurnProgress) -> Unit
    ): AttemptResult {
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/v1/voice-turn/stream")
            .header("Accept", NDJSON)
            .header("Idempotency-Key", idempotencyKey)
            .post(requestBody(sessionId, sceneId, turn, transcript, reducedStimuli))
            .build()
        return await(request) { response -> parseStream(response, onProgress) }
    }

    private suspend fun sendLegacy(
        sessionId: String,
        sceneId: String,
        turn: Int,
        transcript: String,
        reducedStimuli: Boolean,
        idempotencyKey: String
    ): AttemptResult {
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/v1/voice-turn")
            .header("Accept", "application/json")
            .header("Idempotency-Key", idempotencyKey)
            .post(requestBody(sessionId, sceneId, turn, transcript, reducedStimuli))
            .build()
        return await(request) { response ->
            response.body?.string()?.let { body ->
                runCatching { AttemptResult.Success(parseResult(JSONObject(body))) }
                    .getOrDefault(AttemptResult.Retryable)
            } ?: AttemptResult.Retryable
        }
    }

    private fun requestBody(
        sessionId: String,
        sceneId: String,
        turn: Int,
        transcript: String,
        reducedStimuli: Boolean
    ) = JSONObject()
        .put("sessionId", sessionId)
        .put("sceneId", sceneId)
        .put("turn", turn.coerceIn(1, 3))
        .put("transcript", transcript.take(280))
        .put("speaker", "LEIA_FEMALE")
        .put("reducedStimuli", reducedStimuli)
        .toString()
        .toRequestBody(JSON)

    private suspend fun await(
        request: Request,
        consume: (Response) -> AttemptResult
    ): AttemptResult = suspendCancellableCoroutine { continuation ->
        val call = http.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                if (continuation.isActive) continuation.resume(AttemptResult.Retryable)
            }

            override fun onResponse(call: Call, response: Response) {
                val result = response.use {
                    if (!it.isSuccessful) {
                        if (it.code in setOf(404, 405) && request.url.encodedPath.endsWith("/stream")) {
                            AttemptResult.LegacyRequired
                        } else if (it.code in setOf(425, 502, 503, 504)) AttemptResult.Retryable
                        else AttemptResult.Fatal
                    } else runCatching { consume(it) }.getOrDefault(AttemptResult.Retryable)
                }
                if (continuation.isActive) continuation.resume(result)
            }
        })
    }

    private fun parseStream(
        response: Response,
        onProgress: (VoiceTurnProgress) -> Unit
    ): AttemptResult {
        val source = response.body?.source() ?: return AttemptResult.Retryable
        var complete: VoiceTurnResult? = null
        while (!source.exhausted()) {
            val line = source.readUtf8Line()?.takeIf { it.isNotBlank() } ?: continue
            val event = JSONObject(line)
            when (event.getString("type")) {
                "ACK" -> runCatching { onProgress(VoiceTurnProgress.Ack) }
                "FINAL_TEXT" -> event.optJSONObject("response")?.let {
                    runCatching {
                        onProgress(VoiceTurnProgress.FinalText(parseResult(it).copy(audioPending = true)))
                    }
                }
                "COMPLETE", "FALLBACK" -> event.optJSONObject("response")?.let {
                    complete = parseResult(it)
                }
            }
        }
        return complete?.let(AttemptResult::Success) ?: AttemptResult.Retryable
    }

    private fun parseResult(response: JSONObject): VoiceTurnResult {
        val encoded = response.optString("audioBase64")
        return VoiceTurnResult(
            replyText = response.getString("replyText"),
            audio = encoded.takeIf { it.isNotBlank() }?.let(Base64.getDecoder()::decode),
            audioMimeType = response.optString("audioMimeType"),
            visualReaction = response.optString("visualReaction", "ENCOURAGE"),
            nextAction = response.optString("nextAction", "SPEAK_AGAIN"),
            degraded = response.optBoolean("degraded", false)
        )
    }

    private fun offline() = VoiceTurnResult(
        replyText = "A LEIA está sem internet, mas continua com você.",
        degraded = true
    )

    private sealed interface AttemptResult {
        data class Success(val value: VoiceTurnResult) : AttemptResult
        data object Retryable : AttemptResult
        data object LegacyRequired : AttemptResult
        data object Fatal : AttemptResult
    }

    private companion object {
        const val NDJSON = "application/x-ndjson"
        val JSON = "application/json; charset=utf-8".toMediaType()
        val sharedHttp: OkHttpClient = OkHttpClient.Builder()
            .retryOnConnectionFailure(false)
            .connectTimeout(1_500, TimeUnit.MILLISECONDS)
            .readTimeout(6_000, TimeUnit.MILLISECONDS)
            .callTimeout(6_500, TimeUnit.MILLISECONDS)
            .build()
    }
}
