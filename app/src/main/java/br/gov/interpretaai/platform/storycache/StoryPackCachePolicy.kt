package br.gov.interpretaai.platform.storycache

import br.gov.interpretaai.domain.ComicStoryNode
import br.gov.interpretaai.domain.EndStoryNode
import br.gov.interpretaai.domain.GroupHandoffStoryNode
import br.gov.interpretaai.domain.LearningStoryPack
import br.gov.interpretaai.domain.PuzzleStoryNode
import br.gov.interpretaai.domain.StoryAsset
import br.gov.interpretaai.domain.StoryAssetKind
import br.gov.interpretaai.domain.StoryAssetRole
import br.gov.interpretaai.domain.StoryNode
import br.gov.interpretaai.domain.WordBuilderStoryNode

enum class StoryPackCacheState {
    DISCOVERED,
    PREPARING,
    READY_TO_START,
    FULLY_CACHED,
    ACTIVE,
    COMPLETED,
    WAITING_FOR_NETWORK,
    FAILED
}

enum class StoryViewportClass { PHONE, TABLET }

data class CachedVariantKey(val assetId: String, val role: StoryAssetRole)

/** Pure policy: a remote manifest never decides if a child can start by itself. */
object StoryPackCachePolicy {
    fun nextState(
        pack: LearningStoryPack,
        viewport: StoryViewportClass,
        available: Set<CachedVariantKey>
    ): StoryPackCacheState {
        val start = pack.nodes.firstOrNull { it.id == pack.startNodeId }
            ?: return StoryPackCacheState.FAILED
        val startReady = references(start).all { reference ->
            selectVariant(pack.assets.firstOrNull { it.id == reference.assetId }, viewport, reference.audio)
                ?.let { CachedVariantKey(reference.assetId, it.role) in available } == true
        }
        if (!startReady) return StoryPackCacheState.PREPARING
        val fullReady = pack.assets.filter { it.required }.all { asset ->
            selectVariant(asset, viewport, asset.kind == StoryAssetKind.NARRATION_AUDIO)
                ?.let { CachedVariantKey(asset.id, it.role) in available } == true
        }
        return if (fullReady) StoryPackCacheState.FULLY_CACHED else StoryPackCacheState.READY_TO_START
    }

    fun startAssetKeys(
        pack: LearningStoryPack,
        viewport: StoryViewportClass
    ): Set<CachedVariantKey> {
        val start = pack.nodes.firstOrNull { it.id == pack.startNodeId } ?: return emptySet()
        return references(start).mapNotNull { reference ->
            selectVariant(pack.assets.firstOrNull { it.id == reference.assetId }, viewport, reference.audio)
                ?.let { CachedVariantKey(reference.assetId, it.role) }
        }.toSet()
    }

    private data class AssetReference(val assetId: String, val audio: Boolean = false)

    private fun references(node: StoryNode): List<AssetReference> = when (node) {
        is ComicStoryNode -> listOf(AssetReference(node.visualAssetId)) +
            node.dialogue.mapNotNull { line -> line.audioAssetId?.let { AssetReference(it, audio = true) } }
        is PuzzleStoryNode -> listOf(AssetReference(node.imageAssetId))
        is WordBuilderStoryNode -> listOf(AssetReference(node.imageAssetId))
        is GroupHandoffStoryNode, is EndStoryNode -> emptyList()
    }

    private fun selectVariant(
        asset: StoryAsset?,
        viewport: StoryViewportClass,
        audio: Boolean
    ) = asset?.variants?.let { variants ->
        when {
            audio -> variants.firstOrNull { it.role == StoryAssetRole.AUDIO }
            viewport == StoryViewportClass.TABLET -> variants.firstOrNull { it.role == StoryAssetRole.TABLET }
                ?: variants.firstOrNull { it.role == StoryAssetRole.PHONE }
                ?: variants.firstOrNull { it.role != StoryAssetRole.AUDIO }
            else -> variants.firstOrNull { it.role == StoryAssetRole.PHONE }
                ?: variants.firstOrNull { it.role == StoryAssetRole.TABLET }
                ?: variants.firstOrNull { it.role != StoryAssetRole.AUDIO }
        }
    }
}
