package br.gov.interpretaai

import android.content.ContentValues
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.gov.interpretaai.data.LocalMetricsRepository
import br.gov.interpretaai.domain.EventType
import br.gov.interpretaai.domain.LearningEvent
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalMetricsMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val databaseName = "interpreta_ai_metrics.db"

    @Before
    @After
    fun clearDatabase() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun versionOneEventsArePreservedWithoutSuccessColumn() {
        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { database ->
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
            database.execSQL("CREATE INDEX events_pending_sync ON learning_events(synced_at, occurred_at)")
            database.insertOrThrow("learning_events", null, ContentValues().apply {
                put("event_type", "SESSION_STARTED")
                put("child_alias", "aluno-demo")
                put("classroom", "Turma 1A")
                put("activity", "missao-letra-m")
                put("success", 1)
                put("modality", "NONE")
                put("occurred_at", 1L)
            })
            database.version = 1
        }

        assertEquals(1, LocalMetricsRepository(context).snapshot().sessions)

        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { database ->
            val columns = database.rawQuery("PRAGMA table_info(learning_events)", null).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
                }
            }
            assertFalse(columns.contains("success"))
            assertEquals(true, columns.contains("event_id"))
            assertEquals(3, database.version)
        }
    }

    @Test
    fun versionTwoEventsReceiveStableIdsAndRemainPending() {
        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { database ->
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
                    modality TEXT NOT NULL,
                    occurred_at INTEGER NOT NULL,
                    synced_at INTEGER
                )
                """.trimIndent()
            )
            database.execSQL("CREATE INDEX events_pending_sync ON learning_events(synced_at, occurred_at)")
            database.insertOrThrow("learning_events", null, ContentValues().apply {
                put("event_type", "HELP_REQUESTED")
                put("child_alias", "pipa-07")
                put("classroom", "Turma 1A")
                put("activity", "comic-ball")
                put("modality", "TOUCH")
                put("occurred_at", 2L)
            })
            database.version = 2
        }

        val pending = LocalMetricsRepository(context).pending()

        assertEquals(1, pending.size)
        assertEquals("legacy-000000000001", pending.single().eventId)
    }

    @Test
    fun newEventsAreRemovedFromOutboxOnlyAfterConfirmation() {
        val repository = LocalMetricsRepository(context)
        repository.record(LearningEvent(
            type = EventType.SESSION_STARTED,
            eventId = "event-local-0001"
        ))

        assertEquals(listOf("event-local-0001"), repository.pending().map { it.eventId })
        repository.markSynced(listOf("event-local-0001"))
        assertEquals(0, repository.pending().size)
        assertEquals(1, repository.snapshot().sessions)
    }
}
