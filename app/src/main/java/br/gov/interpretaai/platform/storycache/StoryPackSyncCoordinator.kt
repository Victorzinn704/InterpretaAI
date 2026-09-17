package br.gov.interpretaai.platform.storycache

import android.content.Context
import java.io.ByteArrayInputStream

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
 * Writes the cursor only after every pack and its declared variants pass transport, parser and file
 * checks. A cache miss therefore retries the same server page instead of skipping a child's activity.
 */
class StoryPackSyncCoordinator(
    private val delivery: StoryPackDeliveryGateway,
    private val cache: StoryPackCache,
    private val cursors: StoryPackCursor
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
                            val prepared = prepareAssets(
                                active, downloaded.assignmentId, installed.packId, installed.state, viewport
                            )
                            when (prepared) {
                                is AssetPreparation.Prepared -> {
                                    if (prepared.state != StoryPackCacheState.FULLY_CACHED) {
                                        return StoryPackSyncResult.Blocked("pack_assets_incomplete")
                                    }
                                    when (val binding = cache.bindAssignment(
                                        active.deviceId, downloaded.assignmentId, installed.packId,
                                        downloaded.priority, downloaded.expiresAtMs
                                    )) {
                                        AssignmentBindResult.Bound -> Unit
                                        AssignmentBindResult.RetryableFailure ->
                                            return StoryPackSyncResult.RetryableFailure
                                        is AssignmentBindResult.Blocked ->
                                            return StoryPackSyncResult.Blocked(binding.code)
                                    }
                                    ids += installed.packId
                                    states += prepared.state
                                }
                                AssetPreparation.Unauthorized -> return StoryPackSyncResult.Unauthorized
                                AssetPreparation.RetryableFailure -> return StoryPackSyncResult.RetryableFailure
                                is AssetPreparation.Blocked -> return StoryPackSyncResult.Blocked(prepared.code)
                            }
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

    private suspend fun prepareAssets(
        credential: PairedDeviceCredential,
        assignmentId: String,
        packId: String,
        initialState: StoryPackCacheState,
        viewport: StoryViewportClass
    ): AssetPreparation {
        var state = initialState
        for (asset in cache.pendingAssets(packId, viewport)) {
            when (val fetched = delivery.downloadAsset(credential, assignmentId, asset)) {
                is StoryAssetDeliveryResult.Downloaded -> when (val stored = cache.installAsset(
                    packId, asset.assetId, asset.role, viewport, ByteArrayInputStream(fetched.bytes)
                )) {
                    is StoryPackCacheRepository.AssetInstallResult.Stored -> state = stored.state
                    is StoryPackCacheRepository.AssetInstallResult.Blocked -> {
                        return AssetPreparation.Blocked(stored.code)
                    }
                }
                StoryAssetDeliveryResult.Unauthorized -> return AssetPreparation.Unauthorized
                StoryAssetDeliveryResult.RetryableFailure -> return AssetPreparation.RetryableFailure
                is StoryAssetDeliveryResult.Blocked -> return AssetPreparation.Blocked(fetched.code)
            }
        }
        return AssetPreparation.Prepared(state)
    }

    private sealed interface AssetPreparation {
        data class Prepared(val state: StoryPackCacheState) : AssetPreparation
        data object Unauthorized : AssetPreparation
        data object RetryableFailure : AssetPreparation
        data class Blocked(val code: String) : AssetPreparation
    }
}

interface StoryPackCursor {
    fun load(deviceId: String): String?
    fun save(deviceId: String, cursor: String)
}

class StoryPackCursorStore(context: Context) : StoryPackCursor {
    private val preferences = context.applicationContext
        .getSharedPreferences("interpretaai_v2_story_cursor", Context.MODE_PRIVATE)

    override fun load(deviceId: String): String? = preferences.getString(key(deviceId), null)

    override fun save(deviceId: String, cursor: String) {
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
