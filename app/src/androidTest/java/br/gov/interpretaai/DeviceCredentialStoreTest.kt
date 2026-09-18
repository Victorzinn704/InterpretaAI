package br.gov.interpretaai

import androidx.test.platform.app.InstrumentationRegistry
import br.gov.interpretaai.platform.storycache.DeviceCredentialStore
import br.gov.interpretaai.platform.storycache.PairedDeviceCredential
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceCredentialStoreTest {
    @Test fun encryptsCredentialAndKeepsOnlyAStableRandomInstallationIdAfterClear() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = DeviceCredentialStore(context)
        store.clear()
        val installationId = store.installationId()
        val credential = PairedDeviceCredential(
            "https://interpretaai.example",
            "device_test_001",
            "dvc.device_test_001.${"a".repeat(64)}"
        )

        store.save(credential)

        assertEquals(credential, store.load())
        val raw = context.getSharedPreferences("interpretaai_v2_device", 0)
            .getString("credential_v1", "").orEmpty()
        assertFalse(raw.contains(credential.deviceToken))
        store.clear()
        assertNull(store.load())
        assertEquals(installationId, store.installationId())
    }
}
