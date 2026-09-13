package br.gov.interpretaai.platform

import br.gov.interpretaai.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

data class VoiceTurnResult(
    val replyText: String,
    val audio: ByteArray? = null,
    val visualReaction: String = "ENCOURAGE",
    val nextAction: String = "SPEAK_AGAIN",
    val degraded: Boolean = false
)

class VoiceTurnClient(private val baseUrl: String = BuildConfig.VOICE_API_URL) {
    fun send(sessionId: String, sceneId: String, turn: Int, transcript: String, reducedStimuli: Boolean): VoiceTurnResult {
        if (baseUrl.isBlank()) return offline()
        val connection = (URL("${baseUrl.trimEnd('/')}/api/v1/voice-turn").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 6_000
            readTimeout = 6_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
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
            if (connection.responseCode !in 200..299) return offline()
            val response = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val encoded = response.optString("audioBase64")
            VoiceTurnResult(
                replyText = response.getString("replyText"),
                audio = encoded.takeIf { it.isNotBlank() }?.let(Base64.getDecoder()::decode),
                visualReaction = response.optString("visualReaction", "ENCOURAGE"),
                nextAction = response.optString("nextAction", "SPEAK_AGAIN"),
                degraded = response.optBoolean("degraded", false)
            )
        } catch (_: Exception) {
            offline()
        } finally {
            connection.disconnect()
        }
    }

    private fun offline() = VoiceTurnResult(
        replyText = "A LEIA está sem internet, mas continua com você.",
        degraded = true
    )
}
