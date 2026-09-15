package br.gov.interpretaai.platform

import br.gov.interpretaai.BuildConfig
import br.gov.interpretaai.domain.AssignedActivity
import br.gov.interpretaai.domain.DrawingPrompt
import br.gov.interpretaai.domain.PilotRoomParticipant
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
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

sealed interface PilotClassroomResult {
    data class Published(val targetCount: Int) : PilotClassroomResult
    data class Failed(val message: String) : PilotClassroomResult
}

class PilotClassroomClient(
    private val baseUrl: String = BuildConfig.VOICE_API_URL,
    private val http: OkHttpClient = sharedHttp
) {
    suspend fun saveAndPublish(
        classroomId: String,
        classroomLabel: String,
        teacherToken: String,
        participants: List<PilotRoomParticipant>,
        activity: AssignedActivity,
        drawingPrompt: DrawingPrompt
    ): PilotClassroomResult {
        configurationIssue(classroomId, classroomLabel, teacherToken, participants)?.let {
            return PilotClassroomResult.Failed(it)
        }
        val root = "${baseUrl.trimEnd('/')}/api/v1/pilot/classrooms/$classroomId"
        val roster = JSONObject()
            .put("classroomLabel", classroomLabel)
            .put("participants", JSONArray(participants.map { participant ->
                JSONObject()
                    .put("learnerAlias", participant.learnerAlias)
                    .put("avatarId", participant.avatar.id)
                    .put("deviceId", participant.deviceId)
            }))
        val saved = request(Request.Builder().url(root).header("X-Teacher-Token", teacherToken)
            .put(roster.toString().toRequestBody(JSON)).build())
        if (saved.code != 200) return failure(saved.code)

        val assignment = JSONObject()
            .put("learnerAliases", JSONArray())
            .put("activity", activity.name)
            .put("drawingPrompt", drawingPrompt.name)
        val published = request(Request.Builder().url("$root/assignments")
            .header("X-Teacher-Token", teacherToken)
            .post(assignment.toString().toRequestBody(JSON)).build())
        if (published.code != 200) return failure(published.code)
        return runCatching {
            PilotClassroomResult.Published(JSONObject(published.body).getInt("targetCount"))
        }.getOrElse { PilotClassroomResult.Failed("Resposta inválida do servidor.") }
    }

    private fun configurationIssue(
        classroomId: String,
        classroomLabel: String,
        token: String,
        participants: List<PilotRoomParticipant>
    ): String? = when {
        baseUrl.isBlank() -> "Este APK foi gerado sem servidor online."
        !baseUrl.startsWith("https://") && !baseUrl.startsWith("http://127.0.0.1") &&
            !baseUrl.startsWith("http://localhost") -> "O servidor precisa usar HTTPS."
        !classroomId.matches(Regex("[a-zA-Z0-9_-]{3,64}")) -> "Código da sala inválido."
        classroomLabel.isBlank() || classroomLabel.length > 30 -> "Nome da turma inválido."
        token.length < 16 -> "Token muito curto."
        participants.isEmpty() || participants.size > 40 -> "Inclua de 1 a 40 participantes."
        participants.distinctBy { it.learnerAlias }.size != participants.size -> "Alias repetido na sala."
        participants.distinctBy { it.deviceId }.size != participants.size -> "Tablet repetido na sala."
        else -> null
    }

    private fun failure(code: Int) = PilotClassroomResult.Failed(when (code) {
        400 -> "Confira os participantes e a missão."
        401 -> "Token do professor não autorizado."
        409 -> "Um tablet já pertence a outra sala."
        503 -> "Sincronização ainda não ativada no servidor."
        -1 -> "Servidor indisponível."
        else -> "Não foi possível enviar para a sala."
    })

    private suspend fun request(request: Request): HttpResult =
        suspendCancellableCoroutine { continuation ->
            val call = http.newCall(request).apply { timeout().timeout(3_000, TimeUnit.MILLISECONDS) }
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, error: IOException) {
                    if (continuation.isActive) continuation.resume(HttpResult(-1, ""))
                }

                override fun onResponse(call: Call, response: Response) {
                    val result = response.use { HttpResult(it.code, it.body?.string().orEmpty()) }
                    if (continuation.isActive) continuation.resume(result)
                }
            })
        }

    private data class HttpResult(val code: Int, val body: String)

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
