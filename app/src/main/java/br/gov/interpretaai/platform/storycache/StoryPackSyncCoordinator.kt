package br.gov.interpretaai.platform.storycache

import android.content.Context

sealed interface StoryPackSyncResult {
    data class Updated(
        val installedPackIds: List<String>,
        val states: List<StoryPackCacheState>
    ) : StoryPackSyncResult
    data object NoChange : StoryPackSyncResult
    data object NoCredential : StoryPackSyncResult
    data object Unauthorized : StoryPackSyncResult
    data object RetryableFailure : StoryPackSyncResult
    data class Blocked(val code: String) : StoryPackSyncResult
}

/**
 * Writes the cursor only after every downloaded pack in the page passes byte/hash/parser checks.
 * A cache miss therefore retries the same server page instead of skipping a child's activity.
 */
class StoryPackSyncCoordinator(
    private val delivery: StoryPackDeliveryClient,
    private val cache: StoryPackCacheRepository,
    private val cursors: StoryPackCursorStore
) {
    suspend fun sync(
        credential: PairedDeviceCredential?,
        viewport: StoryViewportClass
    ): StoryPackSyncResult {
        val active = credential ?: return StoryPackSyncResult.NoCredential
        val result = delivery.fetchPage(active, cursors.load(active.deviceId))
        return when (result) {
            StoryPackDeliveryResult.NoCredential -> StoryPackSyncResult.NoCredential
            StoryPackDeliveryResult.Unauthorized -> StoryPackSyncResult.Unauthorized
            StoryPackDeliveryResult.RetryableFailure -> StoryPackSyncResult.RetryableFailure
            is StoryPackDeliveryResult.Blocked -> StoryPackSyncResult.Blocked(result.code)
            is StoryPackDeliveryResult.Page -> {
                if (result.packs.isEmpty()) {
                    cursors.save(active.deviceId, result.nextCursor)
                    return StoryPackSyncResult.NoChange
                }
                val ids = mutableListOf<String>()
                val states = mutableListOf<StoryPackCacheState>()
                for (downloaded in result.packs) {
                    when (val installed = cache.installManifest(downloaded.rawJson, viewport)) {
                        is StoryPackCacheRepository.InstallResult.Installed -> {
                            ids += installed.packId
                            states += installed.state
                        }
                        is StoryPackCacheRepository.InstallResult.Blocked -> {
                            return StoryPackSyncResult.Blocked(
                                installed.issues.firstOrNull()?.code ?: "pack_contract_invalid"
                            )
                        }
                        is StoryPackCacheRepository.InstallResult.Conflict -> {
                            return StoryPackSyncResult.Blocked("pack_version_conflict")
                        }
                    }
                }
                cursors.save(active.deviceId, result.nextCursor)
                StoryPackSyncResult.Updated(ids, states)
            }
        }
    }
}

class StoryPackCursorStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("interpretaai_v2_story_cursor", Context.MODE_PRIVATE)

    fun load(deviceId: String): String? = preferences.getString(key(deviceId), null)

    fun save(deviceId: String, cursor: String) {
        require(CURSOR.matches(cursor)) { "manifest_cursor_invalid" }
        check(preferences.edit().putString(key(deviceId), cursor).commit()) {
            "manifest_cursor_write_failed"
        }
    }

    fun clear(deviceId: String) {
        preferences.edit().remove(key(deviceId)).apply()
    }

    private fun key(deviceId: String) = "cursor_$deviceId"

    private companion object {
        val CURSOR = Regex("d1\\.[0-9]{1,18}")
    }
}
