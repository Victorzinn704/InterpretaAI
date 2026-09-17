package br.gov.interpretaai.platform.storycache

import br.gov.interpretaai.domain.ComicStoryNode
import br.gov.interpretaai.domain.EndStoryNode
import br.gov.interpretaai.domain.LearningStoryPack
import br.gov.interpretaai.domain.PuzzleStoryNode
import br.gov.interpretaai.domain.StoryAsset
import br.gov.interpretaai.domain.StoryAssetKind
import br.gov.interpretaai.domain.StoryAssetOrigin
import br.gov.interpretaai.domain.StoryAssetRole
import br.gov.interpretaai.domain.StoryAssetVariant
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryPackCachePolicyTest {
    @Test
    fun onlyReleasesTheStartWhenItsSelectedAssetIsVerified() {
        val pack = pack()
        val comic = CachedVariantKey("comic", StoryAssetRole.PHONE)
        val puzzle = CachedVariantKey("apple", StoryAssetRole.PHONE)

        assertEquals(StoryPackCacheState.PREPARING, StoryPackCachePolicy.nextState(
            pack, StoryViewportClass.PHONE, emptySet()
        ))
        assertEquals(StoryPackCacheState.READY_TO_START, StoryPackCachePolicy.nextState(
            pack, StoryViewportClass.PHONE, setOf(comic)
        ))
        assertEquals(StoryPackCacheState.FULLY_CACHED, StoryPackCachePolicy.nextState(
            pack, StoryViewportClass.PHONE, setOf(comic, puzzle)
        ))
    }

    @Test
    fun tabletUsesTabletArtworkButCanFallBackToPhoneArtwork() {
        assertEquals(setOf(CachedVariantKey("comic", StoryAssetRole.TABLET)),
            StoryPackCachePolicy.startAssetKeys(pack(), StoryViewportClass.TABLET))

        val original = pack()
        val phoneOnly = original.copy(assets = listOf(original.assets.first().copy(
            variants = listOf(original.assets.first().variants.first())
        )) + original.assets.drop(1))
        assertEquals(setOf(CachedVariantKey("comic", StoryAssetRole.PHONE)),
            StoryPackCachePolicy.startAssetKeys(phoneOnly, StoryViewportClass.TABLET))
    }

    @Test
    fun privateFileStoreRejectsWrongContentAndDeduplicatesTheVerifiedAsset() {
        val root = createTempDirectory("interpretaai-story-cache-").toFile()
        try {
            val bytes = "maca".encodeToByteArray()
            val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            val store = StoryPackFileStore(root)

            assertTrue(store.install(hash, bytes.size.toLong(), ByteArrayInputStream(bytes))
                is StoryPackFileStore.CacheWriteResult.Stored)
            assertTrue(store.isVerified(hash, bytes.size.toLong()))
            assertTrue(store.install(hash, bytes.size.toLong(), ByteArrayInputStream(bytes))
                is StoryPackFileStore.CacheWriteResult.AlreadyPresent)
            val anotherHash = MessageDigest.getInstance("SHA-256").digest("pera".encodeToByteArray())
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            assertEquals("asset_hash_mismatch", (store.install(anotherHash, bytes.size.toLong(),
                ByteArrayInputStream("bola".encodeToByteArray()))
                as StoryPackFileStore.CacheWriteResult.Rejected).code)
        } finally {
            root.deleteRecursively()
        }
    }

    private fun pack() = LearningStoryPack(
        schemaVersion = "1.0", packId = "pack_001", storyId = "story_001", version = 1,
        minAppVersion = 1, title = "A maçã", methodology = "LEIA", objectiveIds = listOf("maca"),
        startNodeId = "comic", nodes = listOf(
            ComicStoryNode("comic", listOf("maca"), emptyList(), "comic", "Uma maçã", emptyList(), null, "puzzle"),
            PuzzleStoryNode("puzzle", listOf("maca"), emptyList(), "apple", "2x2", setOf("DRAG"), "Monte", "Pronto", "end"),
            EndStoryNode("end", listOf("maca"), emptyList(), "Fim")
        ), assets = listOf(
            asset("comic", StoryAssetKind.SCENE_IMAGE, required = true, phoneAndTablet = true),
            asset("apple", StoryAssetKind.OBJECT_IMAGE, required = true, phoneAndTablet = false)
        ), minTouchTargetDp = 48, reducedStimuliSupported = true, spokenInstructions = true,
        noRequiredScroll = true, assetOrigins = listOf(
            StoryAssetOrigin("comic", true), StoryAssetOrigin("apple", true)
        )
    )

    private fun asset(
        id: String,
        kind: StoryAssetKind,
        required: Boolean,
        phoneAndTablet: Boolean
    ): StoryAsset {
        val phone = StoryAssetVariant(StoryAssetRole.PHONE, "images/$id.webp", "image/webp", 8,
            "a".repeat(64))
        return StoryAsset(id, kind, required, if (phoneAndTablet) listOf(
            phone, StoryAssetVariant(StoryAssetRole.TABLET, "images/${id}_tablet.webp", "image/webp", 8,
                "b".repeat(64))
        ) else listOf(phone))
    }
}
