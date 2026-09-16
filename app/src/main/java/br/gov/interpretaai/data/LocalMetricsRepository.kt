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
            put("event_id", event.eventId)
            put("event_type", event.type.name)
            put("child_alias", event.childAlias)
            put("classroom", event.classroom)
            put("activity", event.activity)
            put("value", event.value)
            event.durationMs?.let { put("duration_ms", it) }
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
                completedStages = cursor.getInt(2),
                helpRequests = cursor.getInt(3),
                voiceResponses = cursor.getInt(4),
                comicObservations = cursor.getInt(5),
                comicCyclesCompleted = cursor.getInt(6),
                puzzlesCompleted = cursor.getInt(7),
                averagePuzzleMs = cursor.getLong(8),
                averageResponseMs = cursor.getLong(9)
            )
        }
    }

    override fun pending(limit: Int): List<LearningEvent> {
        val safeLimit = limit.coerceIn(1, 50)
        return db.readableDatabase.query(
            "learning_events",
            arrayOf(
                "event_id", "event_type", "child_alias", "classroom", "activity", "value",
                "duration_ms", "modality", "occurred_at"
            ),
            "synced_at IS NULL",
            null,
            null,
            null,
            "occurred_at, id",
            safeLimit.toString()
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(LearningEvent(
                        eventId = cursor.getString(0),
                        type = EventType.valueOf(cursor.getString(1)),
                        childAlias = cursor.getString(2),
                        classroom = cursor.getString(3),
                        activity = cursor.getString(4),
                        value = cursor.getString(5),
                        durationMs = cursor.takeUnless { it.isNull(6) }?.getLong(6),
                        modality = br.gov.interpretaai.domain.ResponseModality.valueOf(cursor.getString(7)),
                        occurredAt = cursor.getLong(8)
                    ))
                }
            }
        }
    }

    override fun markSynced(eventIds: List<String>) {
        if (eventIds.isEmpty()) return
        val placeholders = eventIds.joinToString(",") { "?" }
        db.writableDatabase.update(
            "learning_events",
            ContentValues().apply { put("synced_at", System.currentTimeMillis()) },
            "event_id IN ($placeholders)",
            eventIds.toTypedArray()
        )
    }

    override fun clear() {
        db.writableDatabase.delete("learning_events", null, null)
    }

    private class MetricsDb(context: Context) : SQLiteOpenHelper(
        context,
        "interpreta_ai_metrics.db",
        null,
        3
    ) {
        override fun onCreate(database: SQLiteDatabase) {
            createEventsTable(database)
        }

        override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion < 2) {
                database.execSQL("DROP INDEX IF EXISTS events_pending_sync")
                database.execSQL("ALTER TABLE learning_events RENAME TO learning_events_legacy")
                createEventsTable(database)
                database.execSQL(
                    """
                    INSERT INTO learning_events (
                        id, event_id, event_type, child_alias, classroom, activity, value,
                        duration_ms, modality, occurred_at, synced_at
                    )
                    SELECT
                        id, 'legacy-' || printf('%012d', id), event_type, child_alias, classroom, activity, value,
                        duration_ms, modality, occurred_at, synced_at
                    FROM learning_events_legacy
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE learning_events_legacy")
            } else if (oldVersion < 3) {
                database.execSQL("ALTER TABLE learning_events ADD COLUMN event_id TEXT")
                database.execSQL(
                    "UPDATE learning_events SET event_id = 'legacy-' || printf('%012d', id)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX events_event_id ON learning_events(event_id)"
                )
            }
        }

        private fun createEventsTable(database: SQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE learning_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    event_id TEXT NOT NULL UNIQUE,
                    event_type TEXT NOT NULL,
                    child_alias TEXT NOT NULL,
                    classroom TEXT NOT NULL,
                    activity TEXT NOT NULL,
                    value TEXT,
                    duration_ms INTEGER,
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
    }
}
