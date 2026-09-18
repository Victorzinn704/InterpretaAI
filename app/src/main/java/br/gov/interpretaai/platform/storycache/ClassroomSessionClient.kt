package br.gov.interpretaai.platform.storycache

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ClassroomSeatChoice(
    val learnerId: String,
    val displayName: String,
    val seatNumber: Int,
    val available: Boolean
)

data class ClassroomLobby(
    val sessionId: String,
    val classroomId: String,
    val code: String,
    val learners: List<ClassroomSeatChoice>
)

data class ActiveClassroomSeat(
    val sessionId: String,
    val classroomId: String,
    val learnerAlias: String,
    val seatNumber: Int
)

sealed interface ClassroomSessionResult {
    data class Lobby(val value: ClassroomLobby) : ClassroomSessionResult
    data class Joined(val value: ActiveClassroomSeat) : ClassroomSessionResult
    data object NoCredential : ClassroomSessionResult
    data object InvalidCode : ClassroomSessionResult
    data object SeatUnavailable : ClassroomSessionResult
    data object RetryableFailure : ClassroomSessionResult
    data class Blocked(val code: String) : ClassroomSessionResult
}

/** Adult setup client. Real names are held in memory only and never enter the child journey. */
class ClassroomSessionClient(
    context: Context,
    private val http: OkHttpClient = DEFAULT_HTTP
) {
    private val credentials = DeviceCredentialStore(context.applicationContext)

    suspend fun resolve(rawCode: String): ClassroomSessionResult = request(rawCode, null)

    suspend fun join(rawCode: String, learnerId: String): ClassroomSessionResult =
        request(rawCode, learnerId)

    private suspend fun request(rawCode: String, learnerId: String?): ClassroomSessionResult =
        withContext(Dispatchers.IO) {
            val credential = credentials.load() ?: return@withContext ClassroomSessionResult.NoCredential
            val code = normalizeCode(rawCode)
                ?: return@withContext ClassroomSessionResult.Blocked("classroom_session_code_format_invalid")
            if (learnerId != null && !ID.matches(learnerId)) {
                return@withContext ClassroomSessionResult.Blocked("learner_id_invalid")
            }
            val endpoint = credential.baseUrl.trimEnd('/') + "/api/v2/devices/" +
                credential.deviceId + "/classroom-sessions/" +
                if (learnerId == null) "resolve" else "join"
            val payload = JSONObject().put("code", code).apply {
                if (learnerId != null) put("learnerId", learnerId)
            }.toString().toRequestBody(JSON)
            val request = Request.Builder().url(endpoint).post(payload)
                .header("Authorization", "Bearer ${credential.deviceToken}").build()
            val response = try { http.newCall(request).execute() }
            catch (_: IOException) { return@withContext ClassroomSessionResult.RetryableFailure }
            response.use {
                when (it.code) {
                    200 -> runCatching {
                        val json = JSONObject(it.body?.string().orEmpty())
                        if (learnerId == null) parseLobby(code, json) else parseSeat(json)
                    }.getOrElse { ClassroomSessionResult.Blocked("classroom_session_response_invalid") }
                    400, 404 -> ClassroomSessionResult.InvalidCode
                    409 -> ClassroomSessionResult.SeatUnavailable
                    429, 503, 504 -> ClassroomSessionResult.RetryableFailure
                    else -> ClassroomSessionResult.Blocked("classroom_session_http_${it.code}")
                }
            }
        }

    private fun parseLobby(code: String, json: JSONObject): ClassroomSessionResult {
        val learners = json.getJSONArray("learners")
        val choices = (0 until learners.length()).map { index ->
            val item = learners.getJSONObject(index)
            ClassroomSeatChoice(
                item.getString("learnerId"), item.getString("displayName"),
                item.getInt("seatNumber"), item.getBoolean("available")
            )
        }
        val lobby = ClassroomLobby(
            json.getString("sessionId"), json.getString("classroomId"), code, choices
        )
        check(ID.matches(lobby.sessionId) && ID.matches(lobby.classroomId))
        return ClassroomSessionResult.Lobby(lobby)
    }

    private fun parseSeat(json: JSONObject): ClassroomSessionResult {
        val seat = ActiveClassroomSeat(
            json.getString("sessionId"), json.getString("classroomId"),
            json.getString("learnerAlias"), json.getInt("seatNumber")
        )
        check(ID.matches(seat.sessionId) && ID.matches(seat.classroomId) && ALIAS.matches(seat.learnerAlias))
        return ClassroomSessionResult.Joined(seat)
    }

    private fun normalizeCode(raw: String): String? {
        val compact = raw.uppercase().filterNot(Char::isWhitespace).replace("-", "")
        if (!CODE.matches(compact)) return null
        return compact.take(4) + "-" + compact.drop(4)
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
        val ID = Regex("[a-z0-9][a-z0-9_-]{2,63}")
        val ALIAS = Regex("(sol|pipa|estrela|foguete)-[0-9]{2,3}")
        val CODE = Regex("[23456789A-HJ-NP-Z]{8}")
        val DEFAULT_HTTP = OkHttpClient.Builder()
            .followRedirects(false).retryOnConnectionFailure(false)
            .connectTimeout(3, TimeUnit.SECONDS).readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(8, TimeUnit.SECONDS).build()
    }
}
