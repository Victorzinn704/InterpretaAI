package br.gov.interpretaai.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import br.gov.interpretaai.domain.EventType
import br.gov.interpretaai.domain.LearningEvent
import br.gov.interpretaai.domain.MetricsRepository
import br.gov.interpretaai.domain.MetricsSnapshot

class LocalMetricsRepository(context: Context) : MetricsRepository {
    private val db = MetricsDb(context.applicationContext)

    override fun record(event: LearningEvent) {
        val values = ContentValues().apply {
            put("event_type", event.type.name)
            put("child_alias", event.childAlias)
            put("classroom", event.classroom)
            put("activity", event.activity)
            put("value", event.value)
            event.durationMs?.let { put("duration_ms", it) }
            event.success?.let { put("success", if (it) 1 else 0) }
            put("modality", event.modality.name)
            put("occurred_at", event.occurredAt)
        }
        db.writableDatabase.insertOrThrow("learning_events", null, values)
    }

    override fun snapshot(): MetricsSnapshot {
        val sql = """
            SELECT
              SUM(CASE WHEN event_type = ? THEN 1 ELSE 0 END) sessions,
              SUM(CASE WHEN event_type = ? THEN 1 ELSE 0 END) attempts,
              SUM(CASE WHEN event_type = ? AND success = 1 THEN 1 ELSE 0 END) correct_attempts,
              SUM(CASE WHEN event_type = ? THEN 1 ELSE 0 END) completed_stages,
              SUM(CASE WHEN event_type = ? THEN 1 ELSE 0 END) help_requests,
              SUM(CASE WHEN event_type = ? AND modality = 'VOICE' THEN 1 ELSE 0 END) voice_responses,
              SUM(CASE WHEN event_type = ? AND activity = 'gibi-bola-amigos' THEN 1 ELSE 0 END) comic_observations,
              SUM(CASE WHEN event_type = ? AND activity = 'gibi-bola-amigos' THEN 1 ELSE 0 END) comic_cycles_completed,
              SUM(CASE WHEN event_type = ? AND activity = 'quebra-cabeca-palavras' THEN 1 ELSE 0 END) puzzles_completed,
              COALESCE(AVG(CASE WHEN event_type = ? AND activity = 'quebra-cabeca-palavras' THEN duration_ms END), 0) average_puzzle_ms,
              COALESCE(AVG(CASE WHEN event_type = ? THEN duration_ms END), 0) average_response_ms
            FROM learning_events
        """.trimIndent()
        val args = arrayOf(
            EventType.SESSION_STARTED.name,
            EventType.RESPONSE_SUBMITTED.name,
            EventType.RESPONSE_SUBMITTED.name,
            EventType.STAGE_COMPLETED.name,
            EventType.HELP_REQUESTED.name,
            EventType.RESPONSE_SUBMITTED.name,
            EventType.OBSERVATION_RECORDED.name,
            EventType.SESSION_COMPLETED.name,
            EventType.SESSION_COMPLETED.name,
            EventType.SESSION_COMPLETED.name,
            EventType.RESPONSE_SUBMITTED.name
        )
        return db.readableDatabase.rawQuery(sql, args).use { cursor ->
            cursor.moveToFirst()
            MetricsSnapshot(
                sessions = cursor.getInt(0),
                attempts = cursor.getInt(1),
                correctAttempts = cursor.getInt(2),
                completedStages = cursor.getInt(3),
                helpRequests = cursor.getInt(4),
                voiceResponses = cursor.getInt(5),
                comicObservations = cursor.getInt(6),
                comicCyclesCompleted = cursor.getInt(7),
                puzzlesCompleted = cursor.getInt(8),
                averagePuzzleMs = cursor.getLong(9),
                averageResponseMs = cursor.getLong(10)
            )
        }
    }

    override fun clear() {
        db.writableDatabase.delete("learning_events", null, null)
    }

    private class MetricsDb(context: Context) : SQLiteOpenHelper(
        context,
        "interpreta_ai_metrics.db",
        null,
        1
    ) {
        override fun onCreate(database: SQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE learning_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    event_type TEXT NOT NULL,
                    child_alias TEXT NOT NULL,
                    classroom TEXT NOT NULL,
                    activity TEXT NOT NULL,
                    value TEXT,
                    duration_ms INTEGER,
                    success INTEGER,
                    modality TEXT NOT NULL,
                    occurred_at INTEGER NOT NULL,
                    synced_at INTEGER
                )
                """.trimIndent()
            )
            database.execSQL(
                "CREATE INDEX events_pending_sync ON learning_events(synced_at, occurred_at)"
            )
        }

        override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }
}
