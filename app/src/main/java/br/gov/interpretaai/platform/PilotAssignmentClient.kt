package br.gov.interpretaai.platform

import br.gov.interpretaai.BuildConfig
import br.gov.interpretaai.domain.AssignedActivity
import br.gov.interpretaai.domain.ClassroomAssignment
import br.gov.interpretaai.domain.DrawingPrompt
import br.gov.interpretaai.domain.LearnerAvatars
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
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

sealed interface PilotSyncResult {
    data class Updated(val assignment: ClassroomAssignment, val version: Long) : PilotSyncResult
    data object NoChange : PilotSyncResult
    data class Failed(val message: String) : PilotSyncResult
}

class PilotAssignmentClient(
    private val baseUrl: String = BuildConfig.VOICE_API_URL,
    private val http: OkHttpClient = sharedHttp
) {
    suspend fun fetch(deviceId: String, deviceToken: String, afterVersion: Long): PilotSyncResult {
        configurationIssue(deviceId, deviceToken)?.let { return PilotSyncResult.Failed(it) }
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/v1/pilot/assignments/$deviceId?afterVersion=${afterVersion.coerceAtLeast(0)}")
            .header("X-Device-Token", deviceToken)
            .get()
            .build()
        return await(request)
    }

    suspend fun publish(
        deviceId: String,
        teacherToken: String,
        assignment: ClassroomAssignment
    ): PilotSyncResult {
        configurationIssue(deviceId, teacherToken)?.let { return PilotSyncResult.Failed(it) }
        val body = JSONObject()
            .put("classroomLabel", assignment.classroomLabel)
            .put("avatarId", assignment.avatar.id)
            .put("activity", assignment.activity.name)
            .put("drawingPrompt", assignment.drawingPrompt.name)
            .toString()
            .toRequestBody(JSON)
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/api/v1/pilot/assignments/$deviceId")
            .header("X-Teacher-Token", teacherToken)
            .put(body)
            .build()
        return await(request)
    }

    private fun configurationIssue(deviceId: String, token: String): String? = when {
        baseUrl.isBlank() -> "Este APK foi gerado sem servidor online."
        !baseUrl.startsWith("https://") && !baseUrl.startsWith("http://127.0.0.1") &&
            !baseUrl.startsWith("http://localhost") -> "O servidor precisa usar HTTPS."
        !deviceId.matches(Regex("[a-zA-Z0-9_-]{6,64}")) -> "ID do tablet inválido."
        token.length < 16 -> "Token muito curto."
        else -> null
    }

    private suspend fun await(request: Request): PilotSyncResult =
        suspendCancellableCoroutine { continuation ->
            val call = http.newCall(request).apply { timeout().timeout(3_000, TimeUnit.MILLISECONDS) }
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, error: IOException) {
                    if (continuation.isActive) continuation.resume(PilotSyncResult.Failed("Servidor indisponível."))
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = response.use {
                        when (it.code) {
                            200 -> it.body?.string()?.let(::parse)
                                ?: PilotSyncResult.Failed("Resposta vazia do servidor.")
                            204 -> PilotSyncResult.NoChange
                            401 -> PilotSyncResult.Failed("Token não autorizado.")
                            503 -> PilotSyncResult.Failed("Sincronização ainda não ativada no servidor.")
                            else -> PilotSyncResult.Failed("Não foi possível sincronizar agora.")
                        }
                    }
                    if (continuation.isActive) continuation.resume(result)
                }
            })
        }

    private fun parse(body: String): PilotSyncResult = runCatching {
        val json = JSONObject(body)
        val assignment = ClassroomAssignment(
            classroomLabel = json.getString("classroomLabel"),
            avatar = LearnerAvatars.find(json.getString("avatarId")),
            activity = AssignedActivity.valueOf(json.getString("activity")),
            drawingPrompt = DrawingPrompt.valueOf(json.getString("drawingPrompt"))
        )
        PilotSyncResult.Updated(assignment, json.getLong("version"))
    }.getOrElse { PilotSyncResult.Failed("Resposta inválida do servidor.") }

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
