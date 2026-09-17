package br.gov.interpretaai.platform.storycache

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(
    tableName = "story_pack_cache",
    indices = [Index(value = ["storyId", "version"], unique = true)]
)
data class StoryPackCacheEntity(
    @PrimaryKey val packId: String,
    val storyId: String,
    val version: Int,
    val minAppVersion: Int,
    val rawJson: String,
    val state: String,
    val isPinned: Boolean,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val lastOpenedAtMs: Long?
)

@Entity(
    tableName = "story_pack_asset_cache",
    primaryKeys = ["packId", "assetId", "role"],
    indices = [Index(value = ["sha256"]), Index(value = ["packId", "available"])]
)
data class StoryPackAssetCacheEntity(
    val packId: String,
    val assetId: String,
    val role: String,
    val required: Boolean,
    val expectedPath: String,
    val mediaType: String,
    val expectedBytes: Long,
    val sha256: String,
    val available: Boolean,
    val updatedAtMs: Long
)

@Entity(
    tableName = "story_pack_assignment_cache",
    primaryKeys = ["deviceId", "assignmentId"],
    indices = [Index(value = ["packId"])]
)
data class StoryPackAssignmentCacheEntity(
    val deviceId: String,
    val assignmentId: String,
    val packId: String,
    val priority: Int,
    val expiresAtMs: Long?,
    val updatedAtMs: Long
)

@Entity(
    tableName = "story_pack_session_cache",
    primaryKeys = ["deviceId", "assignmentId"],
    indices = [Index(value = ["packId"]), Index(value = ["deviceId", "state"])]
)
data class StoryPackSessionCacheEntity(
    val deviceId: String,
    val assignmentId: String,
    val packId: String,
    val currentNodeId: String,
    val state: String,
    val startedAtMs: Long,
    val updatedAtMs: Long,
    val completedAtMs: Long?
)

@Dao
interface StoryPackCacheDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPack(pack: StoryPackCacheEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAssets(assets: List<StoryPackAssetCacheEntity>)

    @Query("select * from story_pack_cache where packId = :packId")
    suspend fun findPack(packId: String): StoryPackCacheEntity?

    @Query("select * from story_pack_cache where storyId = :storyId and version = :version")
    suspend fun findStoryVersion(storyId: String, version: Int): StoryPackCacheEntity?

    @Query("select * from story_pack_asset_cache where packId = :packId")
    suspend fun assetsForPack(packId: String): List<StoryPackAssetCacheEntity>

    @Query("select * from story_pack_assignment_cache where deviceId = :deviceId and (expiresAtMs is null or expiresAtMs > :nowMs) order by priority desc, updatedAtMs desc")
    suspend fun activeAssignments(deviceId: String, nowMs: Long): List<StoryPackAssignmentCacheEntity>

    @Query("select * from story_pack_assignment_cache where deviceId = :deviceId and assignmentId = :assignmentId")
    suspend fun findAssignment(deviceId: String, assignmentId: String): StoryPackAssignmentCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssignment(assignment: StoryPackAssignmentCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: StoryPackSessionCacheEntity)

    @Query("select * from story_pack_session_cache where deviceId = :deviceId and assignmentId = :assignmentId")
    suspend fun findSession(deviceId: String, assignmentId: String): StoryPackSessionCacheEntity?

    @Query("select * from story_pack_session_cache where deviceId = :deviceId and state = 'ACTIVE' order by updatedAtMs desc")
    suspend fun activeSessions(deviceId: String): List<StoryPackSessionCacheEntity>

    @Query("select count(*) from story_pack_session_cache where packId = :packId and state = 'ACTIVE'")
    suspend fun activeSessionCount(packId: String): Int

    @Query("select * from story_pack_asset_cache where packId = :packId and assetId = :assetId and role = :role")
    suspend fun findAsset(packId: String, assetId: String, role: String): StoryPackAssetCacheEntity?

    @Query("update story_pack_asset_cache set available = :available, updatedAtMs = :nowMs where packId = :packId and assetId = :assetId and role = :role")
    suspend fun markAssetAvailable(packId: String, assetId: String, role: String, available: Boolean, nowMs: Long)

    @Query("update story_pack_cache set state = :state, updatedAtMs = :nowMs where packId = :packId")
    suspend fun updateState(packId: String, state: String, nowMs: Long)

    @Query("update story_pack_cache set isPinned = :pinned, lastOpenedAtMs = :nowMs, updatedAtMs = :nowMs where packId = :packId")
    suspend fun pin(packId: String, pinned: Boolean, nowMs: Long)

    @Transaction
    suspend fun insertImmutablePack(
        pack: StoryPackCacheEntity,
        assets: List<StoryPackAssetCacheEntity>
    ): StoryPackCacheEntity {
        val existing = findPack(pack.packId)
        if (existing != null) {
            require(existing.rawJson == pack.rawJson) { "pack_id_content_conflict" }
            return existing
        }
        val existingVersion = findStoryVersion(pack.storyId, pack.version)
        if (existingVersion != null) {
            require(existingVersion.packId == pack.packId && existingVersion.rawJson == pack.rawJson) {
                "story_version_content_conflict"
            }
            return existingVersion
        }
        insertPack(pack)
        insertAssets(assets)
        return pack
    }

    @Transaction
    suspend fun bindAssignment(assignment: StoryPackAssignmentCacheEntity) {
        require(findPack(assignment.packId) != null) { "assignment_pack_not_found" }
        val existing = findAssignment(assignment.deviceId, assignment.assignmentId)
        require(existing == null || existing.packId == assignment.packId) {
            "assignment_pack_conflict"
        }
        upsertAssignment(assignment)
    }

    @Transaction
    suspend fun startOrResumeSession(
        deviceId: String,
        assignmentId: String,
        packId: String,
        startNodeId: String,
        nowMs: Long
    ): StoryPackSessionCacheEntity {
        require(findAssignment(deviceId, assignmentId)?.packId == packId) {
            "session_assignment_not_found"
        }
        val existing = findSession(deviceId, assignmentId)
        if (existing?.state == "ACTIVE") {
            require(existing.packId == packId) { "session_pack_conflict" }
            pin(packId, true, nowMs)
            return existing
        }
        val created = StoryPackSessionCacheEntity(
            deviceId, assignmentId, packId, startNodeId, "ACTIVE", nowMs, nowMs, null
        )
        upsertSession(created)
        pin(packId, true, nowMs)
        return created
    }

    @Transaction
    suspend fun moveActiveSession(
        deviceId: String,
        assignmentId: String,
        packId: String,
        nodeId: String,
        nowMs: Long
    ): Boolean {
        val existing = findSession(deviceId, assignmentId) ?: return false
        if (existing.state != "ACTIVE" || existing.packId != packId) return false
        upsertSession(existing.copy(currentNodeId = nodeId, updatedAtMs = nowMs))
        return true
    }

    @Transaction
    suspend fun completeActiveSession(
        deviceId: String,
        assignmentId: String,
        packId: String,
        nowMs: Long
    ): Boolean {
        val existing = findSession(deviceId, assignmentId) ?: return false
        if (existing.state != "ACTIVE" || existing.packId != packId) return false
        upsertSession(existing.copy(state = "COMPLETED", updatedAtMs = nowMs, completedAtMs = nowMs))
        if (activeSessionCount(packId) == 0) pin(packId, false, nowMs)
        return true
    }
}

@Database(
    entities = [StoryPackCacheEntity::class, StoryPackAssetCacheEntity::class,
        StoryPackAssignmentCacheEntity::class, StoryPackSessionCacheEntity::class],
    version = 3,
    exportSchema = true
)
abstract class StoryPackCacheDatabase : RoomDatabase() {
    abstract fun cacheDao(): StoryPackCacheDao

    companion object {
        @Volatile private var instance: StoryPackCacheDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS `story_pack_assignment_cache` (`deviceId` TEXT NOT NULL, `assignmentId` TEXT NOT NULL, `packId` TEXT NOT NULL, `priority` INTEGER NOT NULL, `expiresAtMs` INTEGER, `updatedAtMs` INTEGER NOT NULL, PRIMARY KEY(`deviceId`, `assignmentId`))""")
                db.execSQL("""CREATE INDEX IF NOT EXISTS `index_story_pack_assignment_cache_packId` ON `story_pack_assignment_cache` (`packId`)""")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE IF NOT EXISTS `story_pack_session_cache` (`deviceId` TEXT NOT NULL, `assignmentId` TEXT NOT NULL, `packId` TEXT NOT NULL, `currentNodeId` TEXT NOT NULL, `state` TEXT NOT NULL, `startedAtMs` INTEGER NOT NULL, `updatedAtMs` INTEGER NOT NULL, `completedAtMs` INTEGER, PRIMARY KEY(`deviceId`, `assignmentId`))""")
                db.execSQL("""CREATE INDEX IF NOT EXISTS `index_story_pack_session_cache_packId` ON `story_pack_session_cache` (`packId`)""")
                db.execSQL("""CREATE INDEX IF NOT EXISTS `index_story_pack_session_cache_deviceId_state` ON `story_pack_session_cache` (`deviceId`, `state`)""")
            }
        }

        fun get(context: Context): StoryPackCacheDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                StoryPackCacheDatabase::class.java,
                "interpretaai-story-cache.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
        }
    }
}
