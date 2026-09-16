package br.gov.interpretaai.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TabletCapabilityReportTest {
    @Test fun classifiesCompletePilotWithoutRequiringStylusOrDeviceOwner() {
        assertEquals(
            TabletReadiness.READY,
            TabletCapabilityReport.evaluateReadiness(
                androidSdk = 35,
                shortestSideDp = 360,
                touchscreen = true,
                microphone = true,
                camera = true,
                speechRecognitionAvailable = true,
                ttsEngine = "com.example.tts"
            )
        )
    }

    @Test fun distinguishesLimitedHardwareFromAnIncompatibleScreen() {
        assertEquals(
            TabletReadiness.LIMITED,
            TabletCapabilityReport.evaluateReadiness(35, 360, true, true, false, true, "tts")
        )
        assertEquals(
            TabletReadiness.INCOMPATIBLE,
            TabletCapabilityReport.evaluateReadiness(35, 300, true, true, true, true, "tts")
        )
    }

    @Test fun exportContainsCapabilitiesButNoIdentifierFields() {
        val report = sampleReport()

        val exported = report.exportText()

        assertTrue(exported.contains("model=GET test tablet"))
        assertTrue(exported.contains("lock_task_permitted=true"))
        assertTrue(exported.contains("privacy=no_serial_no_imei_no_account_no_child_data"))
        assertFalse(exported.contains("serial="))
        assertFalse(exported.contains("imei="))
        assertFalse(exported.contains("token="))
        assertFalse(exported.contains("child="))
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
