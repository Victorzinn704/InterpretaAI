package br.gov.interpretaai.platform

import br.gov.interpretaai.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import java.util.UUID
import kotlin.random.Random

data class VoiceTurnResult(
    val replyText: String,
    val audio: ByteArray? = null,
    val audioMimeType: String = "",
    val visualReaction: String = "ENCOURAGE",
    val nextAction: String = "SPEAK_AGAIN",
    val degraded: Boolean = false
)

class VoiceTurnClient(private val baseUrl: String = BuildConfig.VOICE_API_URL) {
    fun warmup() {
        if (baseUrl.isBlank()) return
        val connection = (URL("${baseUrl.trimEnd('/')}/api/v1/gateway/warmup").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 1_000
            readTimeout = 1_000
        }
        try {
            connection.responseCode
        } catch (_: Exception) {
            // Aquecimento é oportunista e nunca bloqueia a jornada local.
        } finally {
            connection.disconnect()
        }
    }

    fun send(sessionId: String, sceneId: String, turn: Int, transcript: String, reducedStimuli: Boolean): VoiceTurnResult {
        if (baseUrl.isBlank()) return offline()
        val idempotencyKey = UUID.randomUUID().toString()
        repeat(2) { attempt ->
            when (val result = sendOnce(
                sessionId, sceneId, turn, transcript, reducedStimuli, idempotencyKey
            )) {
                is AttemptResult.Success -> return result.value
                AttemptResult.Fatal -> return offline()
                AttemptResult.Retryable -> if (attempt == 0) {
                    Thread.sleep(Random.nextLong(80, 181))
                }
            }
        }
        return offline()
    }

    private fun sendOnce(
        sessionId: String,
        sceneId: String,
        turn: Int,
        transcript: String,
        reducedStimuli: Boolean,
        idempotencyKey: String
    ): AttemptResult {
        val connection = (URL("${baseUrl.trimEnd('/')}/api/v1/voice-turn").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 1_500
            readTimeout = 6_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Idempotency-Key", idempotencyKey)
        }
        return try {
            val body = JSONObject()
                .put("sessionId", sessionId)
                .put("sceneId", sceneId)
                .put("turn", turn.coerceIn(1, 3))
                .put("transcript", transcript.take(280))
                .put("speaker", "LEIA_FEMALE")
                .put("reducedStimuli", reducedStimuli)
                .toString()
            connection.outputStream.use { it.write(body.toByteArray()) }
            val status = connection.responseCode
            if (status !in 200..299) {
                return if (status in setOf(425, 502, 503, 504)) {
                    AttemptResult.Retryable
                } else AttemptResult.Fatal
            }
            val response = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val encoded = response.optString("audioBase64")
            AttemptResult.Success(VoiceTurnResult(
                replyText = response.getString("replyText"),
                audio = encoded.takeIf { it.isNotBlank() }?.let(Base64.getDecoder()::decode),
                audioMimeType = response.optString("audioMimeType"),
                visualReaction = response.optString("visualReaction", "ENCOURAGE"),
                nextAction = response.optString("nextAction", "SPEAK_AGAIN"),
                degraded = response.optBoolean("degraded", false)
            ))
        } catch (_: Exception) {
            AttemptResult.Retryable
        } finally {
            connection.disconnect()
        }
    }

    private fun offline() = VoiceTurnResult(
        replyText = "A LEIA está sem internet, mas continua com você.",
        degraded = true
    )

    private sealed interface AttemptResult {
        data class Success(val value: VoiceTurnResult) : AttemptResult
        data object Retryable : AttemptResult
        data object Fatal : AttemptResult
    }
}
