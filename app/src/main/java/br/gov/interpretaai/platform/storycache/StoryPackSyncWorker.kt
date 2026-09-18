package br.gov.interpretaai.platform.storycache

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.gov.interpretaai.InterpretaAiApplication
import java.util.concurrent.TimeUnit

/** Background-only synchronizer; it never navigates, speaks or changes an active child session. */
class StoryPackSyncWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? InterpretaAiApplication ?: return Result.failure()
        val credential = DeviceCredentialStore(applicationContext).load()
        val viewport = if (applicationContext.resources.configuration.smallestScreenWidthDp >= 600) {
            StoryViewportClass.TABLET
        } else {
            StoryViewportClass.PHONE
        }
        val coordinator = StoryPackSyncCoordinator(
            StoryPackDeliveryClient(), app.storyPackCache, StoryPackCursorStore(applicationContext)
        )
        return when (coordinator.sync(credential, viewport)) {
            StoryPackSyncResult.RetryableFailure -> Result.retry()
            StoryPackSyncResult.Unauthorized,
            StoryPackSyncResult.NoCredential,
            StoryPackSyncResult.NoChange,
            is StoryPackSyncResult.Updated,
            is StoryPackSyncResult.Blocked -> Result.success()
        }
    }
}

object StoryPackSyncScheduler {
    private const val UNIQUE_NOW = "interpretaai-story-pack-sync-now"
    private const val UNIQUE_PERIODIC = "interpretaai-story-pack-sync-periodic"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodic = PeriodicWorkRequestBuilder<StoryPackSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext).apply {
            enqueueUniquePeriodicWork(UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, periodic)
        }
        scheduleNow(context)
    }

    fun scheduleNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val immediate = OneTimeWorkRequestBuilder<StoryPackSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(UNIQUE_NOW, ExistingWorkPolicy.REPLACE, immediate)
    }
}
