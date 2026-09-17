package br.gov.interpretaai.platform.storycache

import android.content.Context
import br.gov.interpretaai.BuildConfig
import br.gov.interpretaai.domain.LearningStoryPack
import br.gov.interpretaai.domain.LearningStoryPackParser
import br.gov.interpretaai.domain.StoryAssetRole
import br.gov.interpretaai.domain.StoryPackIssue
import br.gov.interpretaai.domain.StoryPackLoadResult
import java.io.File
import java.io.InputStream

data class StoryPackAssetDownload(
    val assetId: String,
    val role: StoryAssetRole,
    val mediaType: String,
    val expectedBytes: Long,
    val sha256: String
)

interface StoryPackCache {
    suspend fun installManifest(rawJson: String, viewport: StoryViewportClass): StoryPackCacheRepository.InstallResult
    suspend fun installAsset(
        packId: String,
        assetId: String,
        role: StoryAssetRole,
        viewport: StoryViewportClass,
        input: InputStream
    ): StoryPackCacheRepository.AssetInstallResult
    suspend fun pendingAssets(packId: String, viewport: StoryViewportClass): List<StoryPackAssetDownload>
}

class StoryPackCacheRepository(
    private val dao: StoryPackCacheDao,
    private val files: StoryPackFileStore,
    private val now: () -> Long = System::currentTimeMillis,
    private val appVersion: Int = BuildConfig.VERSION_CODE
) : StoryPackCache {
    sealed interface InstallResult {
        data class Installed(val packId: String, val state: StoryPackCacheState) : InstallResult
        data class Blocked(val issues: List<StoryPackIssue>) : InstallResult
        data class Conflict(val packId: String) : InstallResult
    }

    sealed interface AssetInstallResult {
        data class Stored(val state: StoryPackCacheState) : AssetInstallResult
        data class Blocked(val code: String) : AssetInstallResult
    }

    override suspend fun installManifest(
        rawJson: String,
        viewport: StoryViewportClass
    ): InstallResult = when (val parsed = LearningStoryPackParser.parse(rawJson, appVersion)) {
        is StoryPackLoadResult.Blocked -> InstallResult.Blocked(parsed.issues)
        is StoryPackLoadResult.Accepted -> {
            val pack = parsed.pack
            val initialAssets = pack.assets.flatMap { asset -> asset.variants.map { variant ->
                StoryPackAssetCacheEntity(
                    packId = pack.packId,
                    assetId = asset.id,
                    role = variant.role.name,
                    required = asset.required,
                    expectedPath = variant.path,
                    mediaType = variant.mediaType,
                    expectedBytes = variant.bytes,
                    sha256 = variant.sha256,
                    available = files.isVerified(variant.sha256, variant.bytes),
                    updatedAtMs = now()
                )
            } }
            val available = initialAssets.filter { it.available }.map {
                CachedVariantKey(it.assetId, StoryAssetRole.valueOf(it.role))
            }.toSet()
            val state = StoryPackCachePolicy.nextState(pack, viewport, available)
            val entity = StoryPackCacheEntity(
                packId = pack.packId,
                storyId = pack.storyId,
                version = pack.version,
                minAppVersion = pack.minAppVersion,
                rawJson = rawJson,
                state = state.name,
                isPinned = false,
                createdAtMs = now(),
                updatedAtMs = now(),
                lastOpenedAtMs = null
            )
            try {
                val stored = dao.insertImmutablePack(entity, initialAssets)
                if (stored.packId == entity.packId && stored.state != entity.state) {
                    dao.updateState(entity.packId, state.name, now())
                }
                InstallResult.Installed(pack.packId, StoryPackCacheState.valueOf(
                    dao.findPack(pack.packId)?.state ?: state.name
                ))
            } catch (_: IllegalArgumentException) {
                InstallResult.Conflict(pack.packId)
            }
        }
    }

    override suspend fun installAsset(
        packId: String,
        assetId: String,
        role: StoryAssetRole,
        viewport: StoryViewportClass,
        input: InputStream
    ): AssetInstallResult {
        val pack = dao.findPack(packId) ?: return AssetInstallResult.Blocked("pack_not_found")
        val asset = dao.findAsset(packId, assetId, role.name)
            ?: return AssetInstallResult.Blocked("asset_not_declared")
        return when (val write = files.install(asset.sha256, asset.expectedBytes, input)) {
            is StoryPackFileStore.CacheWriteResult.Rejected -> AssetInstallResult.Blocked(write.code)
            is StoryPackFileStore.CacheWriteResult.Stored,
            is StoryPackFileStore.CacheWriteResult.AlreadyPresent -> {
                dao.markAssetAvailable(packId, assetId, role.name, true, now())
                val state = refreshState(pack, viewport)
                AssetInstallResult.Stored(state)
            }
        }
    }

    /**
     * Gives first-scene variants priority, but exposes one variant at a time to keep preparation
     * bounded on school tablets. File verification is repeated because files may be evicted while
     * Room still records them as available.
     */
    override suspend fun pendingAssets(
        packId: String,
        viewport: StoryViewportClass
    ): List<StoryPackAssetDownload> {
        val stored = dao.findPack(packId) ?: return emptyList()
        val parsed = LearningStoryPackParser.parse(stored.rawJson, appVersion)
        if (parsed !is StoryPackLoadResult.Accepted) return emptyList()
        val start = StoryPackCachePolicy.startAssetKeys(parsed.pack, viewport)
        val selected = StoryPackCachePolicy.selectedAssetKeys(parsed.pack, viewport)
        return dao.assetsForPack(packId).mapNotNull { asset ->
            val key = CachedVariantKey(asset.assetId, StoryAssetRole.valueOf(asset.role))
            if (key !in selected) return@mapNotNull null
            val available = files.isVerified(asset.sha256, asset.expectedBytes)
            if (available != asset.available) {
                dao.markAssetAvailable(packId, asset.assetId, asset.role, available, now())
            }
            if (available) null else StoryPackAssetDownload(
                asset.assetId,
                key.role,
                asset.mediaType,
                asset.expectedBytes,
                asset.sha256
            )
        }.sortedWith(compareBy<StoryPackAssetDownload> {
            CachedVariantKey(it.assetId, it.role) !in start
        }.thenBy { it.assetId }.thenBy { it.role.name })
    }

    suspend fun loadPreparedPack(
        packId: String,
        viewport: StoryViewportClass
    ): StoryPackLoadResult {
        val entity = dao.findPack(packId) ?: return StoryPackLoadResult.Blocked(listOf(
            StoryPackIssue("pack_not_found", "$", "A atividade não está disponível neste aparelho.")
        ))
        val parsed = LearningStoryPackParser.parse(entity.rawJson, appVersion)
        if (parsed is StoryPackLoadResult.Accepted) {
            val state = refreshState(entity, viewport)
            if (state !in setOf(StoryPackCacheState.READY_TO_START, StoryPackCacheState.FULLY_CACHED,
                    StoryPackCacheState.ACTIVE, StoryPackCacheState.COMPLETED)) {
                return StoryPackLoadResult.Blocked(listOf(StoryPackIssue(
                    "pack_not_prepared", "$", "A atividade ainda está sendo preparada."
                )))
            }
        }
        return parsed
    }

    suspend fun pinActive(packId: String, active: Boolean) {
        dao.pin(packId, active, now())
    }

    suspend fun reconcile(viewport: StoryViewportClass) {
        files.deletePartialFiles()
        // Future manifest sync supplies the pack IDs. Reconciliation is intentionally local-only.
    }

    private suspend fun refreshState(
        entity: StoryPackCacheEntity,
        viewport: StoryViewportClass
    ): StoryPackCacheState {
        val parsed = LearningStoryPackParser.parse(entity.rawJson, appVersion)
        if (parsed !is StoryPackLoadResult.Accepted) {
            dao.updateState(entity.packId, StoryPackCacheState.FAILED.name, now())
            return StoryPackCacheState.FAILED
        }
        val available = dao.assetsForPack(entity.packId).filter { asset ->
            val verified = files.isVerified(asset.sha256, asset.expectedBytes)
            if (verified != asset.available) {
                dao.markAssetAvailable(entity.packId, asset.assetId, asset.role, verified, now())
            }
            verified
        }.map { CachedVariantKey(it.assetId, StoryAssetRole.valueOf(it.role)) }.toSet()
        val state = StoryPackCachePolicy.nextState(parsed.pack, viewport, available)
        dao.updateState(entity.packId, state.name, now())
        return state
    }

    companion object {
        fun from(context: Context): StoryPackCacheRepository = StoryPackCacheRepository(
            StoryPackCacheDatabase.get(context).cacheDao(),
            StoryPackFileStore(File(context.filesDir, "story-pack-assets"))
        )
    }
}
