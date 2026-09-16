package br.gov.interpretaai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import br.gov.interpretaai.platform.TabletCapabilityReport
import br.gov.interpretaai.ui.screens.TabletDiagnosticCard
import br.gov.interpretaai.ui.theme.InterpretaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TabletDiagnosticUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun educatorCanReviewAndCopyPrivacySafeTabletDiagnostic() {
        val report = sampleReport()
        var copied: TabletCapabilityReport? = null

        compose.setContent {
            InterpretaTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    TabletDiagnosticCard(report) { copied = it }
                }
            }
        }

        compose.onNodeWithText("DIAGNÓSTICO DESTE TABLET").assertIsDisplayed()
        compose.onNodeWithText("GET test tablet", substring = true).assertIsDisplayed()
        compose.onNodeWithText("O diagnóstico não coleta serial", substring = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("COPIAR DIAGNÓSTICO", substring = true)
            .performScrollTo().performClick()

        assertEquals(report, copied)
    }

    private fun sampleReport() = TabletCapabilityReport(
        manufacturer = "Fabricante",
        model = "GET test tablet",
        device = "tablet",
        androidRelease = "15",
        androidSdk = 35,
        securityPatch = "2026-08-01",
        supportedAbis = "arm64-v8a",
        screenPixels = "1600x2560",
        shortestSideDp = 600,
        densityDpi = 320,
        touchscreen = true,
        microphone = true,
        camera = true,
        stylusActive = false,
        speechRecognitionAvailable = true,
        recognitionService = "recognizer",
        ttsEngine = "tts",
        deviceOwner = true,
        lockTaskPermitted = true,
        lockTaskActive = true,
        doNotDisturbAccess = true
    )
}
