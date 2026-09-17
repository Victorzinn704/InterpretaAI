package br.gov.interpretaai

import android.app.Application
import br.gov.interpretaai.data.LocalMetricsRepository
import br.gov.interpretaai.platform.storycache.StoryPackCacheRepository
import br.gov.interpretaai.platform.storycache.StoryPackSyncScheduler

class InterpretaAiApplication : Application() {
    val metricsRepository by lazy { LocalMetricsRepository(this) }
    val storyPackCache by lazy { StoryPackCacheRepository.from(this) }

    override fun onCreate() {
        super.onCreate()
        StoryPackSyncScheduler.schedule(this)
    }
}
