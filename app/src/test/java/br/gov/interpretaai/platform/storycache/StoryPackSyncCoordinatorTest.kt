package br.gov.interpretaai.platform.storycache

import br.gov.interpretaai.domain.StoryAssetRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStream

class StoryPackSyncCoordinatorTest {
    @Test fun advancesCursorOnlyAfterPackAndEveryVariantAreAtomicallyAccepted() = runBlocking {
        val cursor = FakeCursor("d1.4")
        val cache = FakeCache()
        val delivery = FakeDelivery(StoryAssetDeliveryResult.Downloaded(byteArrayOf(1, 2, 3)))

        val result = StoryPackSyncCoordinator(delivery, cache, cursor).sync(credential(), StoryViewportClass.PHONE)

        assertEquals(StoryPackSyncResult.Updated(listOf("pack_bola_001"), listOf(
            StoryPackCacheState.FULLY_CACHED
        )), result)
        assertEquals(listOf("assignment_bola_001:maca_objeto:PHONE:3"), cache.installedAssets)
        assertEquals(listOf("device_demo_001" to "d1.5"), cursor.saved)
        assertEquals(listOf("device_demo_001:assignment_bola_001:pack_bola_001"), cache.bindings)
    }

    @Test fun doesNotAdvanceCursorWhenAVariantFailsIntegrityValidation() = runBlocking {
        val cursor = FakeCursor("d1.4")
        val cache = FakeCache(assetResult = StoryPackCacheRepository.AssetInstallResult.Blocked(
            "asset_hash_mismatch"
        ))
        val delivery = FakeDelivery(StoryAssetDeliveryResult.Downloaded(byteArrayOf(1, 2, 3)))

        val result = StoryPackSyncCoordinator(delivery, cache, cursor).sync(credential(), StoryViewportClass.PHONE)

        assertEquals(StoryPackSyncResult.Blocked("asset_hash_mismatch"), result)
        assertTrue(cursor.saved.isEmpty())
        assertTrue(cache.bindings.isEmpty())
    }

    @Test fun preservesCursorAndLetsWorkManagerRetryForATransientAssetFailure() = runBlocking {
        val cursor = FakeCursor("d1.4")
        val cache = FakeCache()
        val delivery = FakeDelivery(StoryAssetDeliveryResult.RetryableFailure)

        val result = StoryPackSyncCoordinator(delivery, cache, cursor).sync(credential(), StoryViewportClass.PHONE)

        assertEquals(StoryPackSyncResult.RetryableFailure, result)
        assertTrue(cursor.saved.isEmpty())
        assertTrue(cache.bindings.isEmpty())

    }

    @Test fun doesNotAdvanceCursorWhenAssignmentBindingConflicts() = runBlocking {
        val cursor = FakeCursor("d1.4")
        val cache = FakeCache(bindResult = AssignmentBindResult.Blocked("assignment_pack_conflict"))
        val delivery = FakeDelivery(StoryAssetDeliveryResult.Downloaded(byteArrayOf(1, 2, 3)))

        val result = StoryPackSyncCoordinator(delivery, cache, cursor).sync(credential(), StoryViewportClass.PHONE)

        assertEquals(StoryPackSyncResult.Blocked("assignment_pack_conflict"), result)
        assertTrue(cursor.saved.isEmpty())
    }

    @Test fun retriesWhenLocalAssignmentStorageIsTemporarilyUnavailable() = runBlocking {
        val cursor = FakeCursor("d1.4")
        val cache = FakeCache(bindResult = AssignmentBindResult.RetryableFailure)

        val result = StoryPackSyncCoordinator(FakeDelivery(
            StoryAssetDeliveryResult.Downloaded(byteArrayOf(1, 2, 3))
        ), cache, cursor).sync(credential(), StoryViewportClass.PHONE)

        assertEquals(StoryPackSyncResult.RetryableFailure, result)
        assertTrue(cursor.saved.isEmpty())
    }

    @Test fun neverPublishesAssignmentWhenSelectedAssetsAreStillMissing() = runBlocking {
        val cursor = FakeCursor("d1.4")
        val cache = FakeCache(assetResult = StoryPackCacheRepository.AssetInstallResult.Stored(
            StoryPackCacheState.PREPARING
        ))

        val result = StoryPackSyncCoordinator(FakeDelivery(
            StoryAssetDeliveryResult.Downloaded(byteArrayOf(1, 2, 3))
        ), cache, cursor).sync(credential(), StoryViewportClass.PHONE)

        assertEquals(StoryPackSyncResult.Blocked("pack_assets_incomplete"), result)
        assertTrue(cache.bindings.isEmpty())
        assertTrue(cursor.saved.isEmpty())
    }

    private fun credential() = PairedDeviceCredential(
        "https://api.example.test", "device_demo_001", "dvc.device_demo_001.${"a".repeat(64)}"
    )

    private class FakeDelivery(
        private val assetResult: StoryAssetDeliveryResult
    ) : StoryPackDeliveryGateway {
        override suspend fun fetchPage(
            credential: PairedDeviceCredential?,
            cursor: String?
        ) = StoryPackDeliveryResult.Page(listOf(
            DownloadedStoryPack(
                "assignment_bola_001", "story_bola_001", 1, "{}", "a".repeat(64), 80, null
            )
        ), "d1.5")

        override suspend fun downloadAsset(
            credential: PairedDeviceCredential?,
            assignmentId: String,
            asset: StoryPackAssetDownload
        ): StoryAssetDeliveryResult = assetResult
    }

    private class FakeCache(
        private val assetResult: StoryPackCacheRepository.AssetInstallResult =
            StoryPackCacheRepository.AssetInstallResult.Stored(StoryPackCacheState.FULLY_CACHED),
        private val bindResult: AssignmentBindResult = AssignmentBindResult.Bound
    ) : StoryPackCache {
        val installedAssets = mutableListOf<String>()
        val bindings = mutableListOf<String>()

        override suspend fun installManifest(
            rawJson: String,
            viewport: StoryViewportClass
        ) = StoryPackCacheRepository.InstallResult.Installed(
            "pack_bola_001", StoryPackCacheState.PREPARING
        )

        override suspend fun installAsset(
            packId: String,
            assetId: String,
            role: StoryAssetRole,
            viewport: StoryViewportClass,
            input: InputStream
        ): StoryPackCacheRepository.AssetInstallResult {
            installedAssets += "assignment_bola_001:$assetId:${role.name}:${input.readBytes().size}"
            return assetResult
        }

        override suspend fun pendingAssets(
            packId: String,
            viewport: StoryViewportClass
        ) = listOf(StoryPackAssetDownload(
            "maca_objeto", StoryAssetRole.PHONE, "image/png", 3, "a".repeat(64)
        ))

        override suspend fun bindAssignment(
            deviceId: String,
            assignmentId: String,
            packId: String,
            priority: Int,
            expiresAtMs: Long?
        ): AssignmentBindResult {
            if (bindResult == AssignmentBindResult.Bound) bindings += "$deviceId:$assignmentId:$packId"
            return bindResult
        }
    }

    private class FakeCursor(private val initial: String?) : StoryPackCursor {
        val saved = mutableListOf<Pair<String, String>>()
        override fun load(deviceId: String): String? = initial
        override fun save(deviceId: String, cursor: String) {
            saved += deviceId to cursor
        }
    }
}
