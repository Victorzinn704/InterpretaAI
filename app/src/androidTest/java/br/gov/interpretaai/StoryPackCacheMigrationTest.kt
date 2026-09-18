package br.gov.interpretaai

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.gov.interpretaai.platform.storycache.StoryPackCacheDatabase
import br.gov.interpretaai.platform.storycache.StoryPackCacheRepository
import br.gov.interpretaai.platform.storycache.StoryPackFileStore
import br.gov.interpretaai.platform.storycache.StoryViewportClass
import br.gov.interpretaai.platform.storycache.AssignmentBindResult
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoryPackCacheMigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "story-cache-migration-test.db"

    @Before @After fun clear() { context.deleteDatabase(name) }

    @Test fun v1PackSurvivesAndAssignmentAndSessionTablesAreCreated() = runBlocking {
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL("""CREATE TABLE IF NOT EXISTS `story_pack_cache` (`packId` TEXT NOT NULL, `storyId` TEXT NOT NULL, `version` INTEGER NOT NULL, `minAppVersion` INTEGER NOT NULL, `rawJson` TEXT NOT NULL, `state` TEXT NOT NULL, `isPinned` INTEGER NOT NULL, `createdAtMs` INTEGER NOT NULL, `updatedAtMs` INTEGER NOT NULL, `lastOpenedAtMs` INTEGER, PRIMARY KEY(`packId`))""")
            db.execSQL("""CREATE UNIQUE INDEX IF NOT EXISTS `index_story_pack_cache_storyId_version` ON `story_pack_cache` (`storyId`, `version`)""")
            db.execSQL("""CREATE TABLE IF NOT EXISTS `story_pack_asset_cache` (`packId` TEXT NOT NULL, `assetId` TEXT NOT NULL, `role` TEXT NOT NULL, `required` INTEGER NOT NULL, `expectedPath` TEXT NOT NULL, `mediaType` TEXT NOT NULL, `expectedBytes` INTEGER NOT NULL, `sha256` TEXT NOT NULL, `available` INTEGER NOT NULL, `updatedAtMs` INTEGER NOT NULL, PRIMARY KEY(`packId`, `assetId`, `role`))""")
            db.execSQL("""CREATE INDEX IF NOT EXISTS `index_story_pack_asset_cache_sha256` ON `story_pack_asset_cache` (`sha256`)""")
            db.execSQL("""CREATE INDEX IF NOT EXISTS `index_story_pack_asset_cache_packId_available` ON `story_pack_asset_cache` (`packId`, `available`)""")
            db.execSQL("""INSERT INTO story_pack_cache (packId,storyId,version,minAppVersion,rawJson,state,isPinned,createdAtMs,updatedAtMs) VALUES ('pack_old_001','story_old_001',1,1,'{}','DISCOVERED',0,1,1)""")
            db.version = 1
        }

        val migrated = Room.databaseBuilder(context, StoryPackCacheDatabase::class.java, name)
            .addMigrations(StoryPackCacheDatabase.MIGRATION_1_2,
                StoryPackCacheDatabase.MIGRATION_2_3).build()
        try {
            assertEquals("pack_old_001", migrated.cacheDao().findPack("pack_old_001")?.packId)
            assertTrue(migrated.cacheDao().activeAssignments("device_one_001", 0).isEmpty())
            assertTrue(migrated.cacheDao().activeSessions("device_one_001").isEmpty())
        } finally {
            migrated.close()
        }
    }

    @Test fun aPreparedStoryBelongsOnlyToItsPairedDeviceUntilExpiry() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, StoryPackCacheDatabase::class.java).build()
        val files = File(context.cacheDir, "story-cache-assignment-test")
        var clock = 1_000L
        val cache = StoryPackCacheRepository(database.cacheDao(), StoryPackFileStore(files),
            now = { clock }, appVersion = 21)
        try {
            assertTrue(cache.installManifest(assetlessStory(), StoryViewportClass.PHONE)
                is StoryPackCacheRepository.InstallResult.Installed)
            assertEquals(AssignmentBindResult.Bound, cache.bindAssignment(
                "device_school_a", "assignment_story_a", "pack_story_a", 80, 2_000L
            ))
            assertEquals(1, cache.readyAssignments("device_school_a", StoryViewportClass.PHONE).size)
            assertEquals(1, cache.preparedReceipts("device_school_a", StoryViewportClass.PHONE).size)
            assertEquals(sha256(assetlessStory()), cache.preparedReceipts(
                "device_school_a", StoryViewportClass.PHONE
            ).single().packSha256)
            assertTrue(cache.readyAssignments("device_school_b", StoryViewportClass.PHONE).isEmpty())
            assertEquals(null, cache.loadAssignedStory(
                "device_school_b", "assignment_story_a", StoryViewportClass.PHONE))
            val started = cache.loadAssignedStory(
                "device_school_a", "assignment_story_a", StoryViewportClass.PHONE)!!
            assertEquals("grupo", started.resumeNodeId)
            assertTrue(cache.saveSessionNode("device_school_a", started, "fim"))
            assertEquals("fim", cache.loadAssignedStory(
                "device_school_a", "assignment_story_a", StoryViewportClass.PHONE)?.resumeNodeId)
            assertEquals(true, database.cacheDao().findPack("pack_story_a")?.isPinned)
            clock = 2_001L
            assertEquals(1, cache.readyAssignments("device_school_a", StoryViewportClass.PHONE).size)
            assertTrue(cache.preparedReceipts("device_school_a", StoryViewportClass.PHONE).isEmpty())
            assertEquals("fim", cache.loadAssignedStory(
                "device_school_a", "assignment_story_a", StoryViewportClass.PHONE)?.resumeNodeId)
            assertTrue(cache.completeSession("device_school_a", started))
            assertEquals(false, database.cacheDao().findPack("pack_story_a")?.isPinned)
            assertTrue(cache.readyAssignments("device_school_a", StoryViewportClass.PHONE).isEmpty())
            assertEquals(null, cache.loadAssignedStory(
                "device_school_a", "assignment_story_a", StoryViewportClass.PHONE))
        } finally {
            database.close()
        }
    }

    @Test fun withdrawalHidesTheStoryAndEndsItsLocalSessionWithoutDeletingThePack() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, StoryPackCacheDatabase::class.java).build()
        val files = File(context.cacheDir, "story-cache-withdrawal-test")
        val cache = StoryPackCacheRepository(database.cacheDao(), StoryPackFileStore(files),
            now = { 1_000L }, appVersion = 21)
        try {
            cache.installManifest(assetlessStory(), StoryViewportClass.PHONE)
            assertEquals(AssignmentBindResult.Bound, cache.bindAssignment(
                "device_school_a", "assignment_story_a", "pack_story_a", 80, null
            ))
            val story = cache.loadAssignedStory(
                "device_school_a", "assignment_story_a", StoryViewportClass.PHONE)!!
            cache.withdrawAssignment("device_school_a", "assignment_story_a")
            assertTrue(cache.readyAssignments("device_school_a", StoryViewportClass.PHONE).isEmpty())
            assertTrue(cache.preparedReceipts("device_school_a", StoryViewportClass.PHONE).isEmpty())
            assertEquals(null, cache.loadAssignedStory(
                "device_school_a", "assignment_story_a", StoryViewportClass.PHONE))
            assertEquals(false, cache.saveSessionNode("device_school_a", story, "fim"))
            assertEquals(false, database.cacheDao().findPack("pack_story_a")?.isPinned)
            assertEquals("REVOKED", database.cacheDao().findSession(
                "device_school_a", "assignment_story_a")?.state)
            assertTrue(database.cacheDao().findPack("pack_story_a") != null)
        } finally {
            database.close()
        }
    }

    private fun assetlessStory() = """
        {
          "schemaVersion":"1.0","packId":"pack_story_a","storyId":"story_a",
          "version":1,"minAppVersion":21,"title":"Conversa com a turma",
          "methodology":"LEIA","objectiveIds":["conversar"],"startNodeId":"grupo",
          "nodes":[
            {"id":"grupo","type":"GROUP_HANDOFF","objectiveIds":["conversar"],
             "instruction":"Conte sua ideia à dupla.","nextNodeId":"fim"},
            {"id":"fim","type":"END","objectiveIds":["conversar"],
             "closingSpeech":"Obrigado por conversar!"}
          ],"assets":[],
          "accessibility":{"minTouchTargetDp":48,"reducedStimuliSupported":true,
            "spokenInstructions":true,"noRequiredScroll":true},
          "provenance":{"createdBy":"TEACHER","approvedBy":"professora_teste",
            "approvedAt":"2026-09-17T12:00:00Z","sourceRefs":[],"assetOrigins":[]}
        }
    """.trimIndent()

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
