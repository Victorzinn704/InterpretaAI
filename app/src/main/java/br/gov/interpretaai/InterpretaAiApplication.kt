package br.gov.interpretaai

import android.app.Application
import br.gov.interpretaai.data.LocalMetricsRepository

class InterpretaAiApplication : Application() {
    val metricsRepository by lazy { LocalMetricsRepository(this) }
}
