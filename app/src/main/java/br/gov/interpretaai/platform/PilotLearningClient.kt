package br.gov.interpretaai.platform

import br.gov.interpretaai.BuildConfig
import br.gov.interpretaai.domain.EventType
import br.gov.interpretaai.domain.LearningEvent
import br.gov.interpretaai.domain.ResponseModality
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class PilotLearningClient(
    private val baseUrl: String = BuildConfig.VOICE_API_URL,
    private val http: OkHttpClient = sharedHttp
) {
    suspend fun send(deviceId: String, deviceToken: String, events: List<LearningEvent>): Boolean {
        if (!validConfiguration(deviceId, deviceToken) || events.isEmpty() || events.size > 50) return false
        val body = JSONObject()
            .put("deviceId", deviceId)
            .put("events", JSONArray(events.map(::eventJson)))
            .toString()
            .toRequestBody(JSON)
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/v1/pilot/learning-events:batch")
            .header("X-Device-Token", deviceToken)
            .post(body)
            .build()
        return suspendCancellableCoroutine { continuation ->
            val call = http.newCall(request).apply { timeout().timeout(3_000, TimeUnit.MILLISECONDS) }
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, error: IOException) {
                    if (continuation.isActive) continuation.resume(false)
                }

                override fun onResponse(call: Call, response: Response) {
                    val accepted = response.use { it.isSuccessful }
                    if (continuation.isActive) continuation.resume(accepted)
                }
            })
        }
    }

    private fun eventJson(event: LearningEvent) = JSONObject()
        .put("eventId", event.eventId)
        .put("activityId", event.activity)
        .put("type", event.type.name)
        .put("modality", event.modality.name)
        .apply { event.durationMs?.let { put("durationMs", it.coerceIn(0, 600_000)) } }
        .put("observationCategory", observationCategory(event))
        .put("occurredAt", Instant.ofEpochMilli(event.occurredAt).toString())

    private fun observationCategory(event: LearningEvent) = when {
        event.type == EventType.RESPONSE_SUBMITTED && event.modality == ResponseModality.VOICE ->
            "ORAL_EXPRESSION"
        event.type == EventType.OBSERVATION_RECORDED -> "CONTEXT_REASONING"
        event.type in setOf(
            EventType.STAGE_COMPLETED,
            EventType.HELP_REQUESTED,
            EventType.STAGE_ADVANCED_WITH_SUPPORT
        ) -> "PARTICIPATION"
        else -> "NONE"
    }

    private fun validConfiguration(deviceId: String, token: String): Boolean =
        baseUrl.isNotBlank() &&
            (baseUrl.startsWith("https://") || baseUrl.startsWith("http://127.0.0.1") ||
                baseUrl.startsWith("http://localhost")) &&
            deviceId.matches(Regex("[a-zA-Z0-9_-]{6,64}")) && token.length >= 16

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
        val sharedHttp = OkHttpClient.Builder()
            .retryOnConnectionFailure(false)
            .connectTimeout(1_000, TimeUnit.MILLISECONDS)
            .readTimeout(3_000, TimeUnit.MILLISECONDS)
            .callTimeout(3_000, TimeUnit.MILLISECONDS)
            .build()
    }
}
