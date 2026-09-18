package br.gov.interpretaai.platform.storycache

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONObject
import java.security.KeyStore
import java.net.URI
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keeps the revocable v2 device credential encrypted with a key that cannot leave Android Keystore.
 * Enrollment UI is intentionally separate from the child journey.
 */
data class PairedDeviceCredential(
    val baseUrl: String,
    val deviceId: String,
    val deviceToken: String
) {
    fun valid(): Boolean = allowedBaseUrl() && DEVICE_ID.matches(deviceId)
        && deviceToken.length in 64..256 && deviceToken.startsWith("dvc.$deviceId.")

    private fun allowedBaseUrl(): Boolean = runCatching {
        if (baseUrl != baseUrl.trim()) return false
        val endpoint = URI(baseUrl)
        val secure = endpoint.scheme.equals("https", ignoreCase = true) && endpoint.host != null
        val localTest = endpoint.scheme.equals("http", ignoreCase = true)
            && endpoint.host in setOf("localhost", "127.0.0.1")
        (secure || localTest) && endpoint.userInfo == null
            && endpoint.query == null && endpoint.fragment == null
    }.getOrDefault(false)

    private companion object {
        val DEVICE_ID = Regex("[a-z0-9][a-z0-9_-]{2,63}")
    }
}

class DeviceCredentialStore(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences("interpretaai_v2_device", Context.MODE_PRIVATE)

    fun load(): PairedDeviceCredential? = runCatching {
        val encoded = preferences.getString(ENCRYPTED_CREDENTIAL, null) ?: return null
        val pieces = encoded.split('.', limit = 2)
        if (pieces.size != 2 || pieces[0] != FORMAT_VERSION) return null
        val bytes = Base64.decode(pieces[1], Base64.NO_WRAP)
        if (bytes.size <= IV_BYTES) return null
        val iv = bytes.copyOfRange(0, IV_BYTES)
        val cipherText = bytes.copyOfRange(IV_BYTES, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        }
        val json = JSONObject(String(cipher.doFinal(cipherText), Charsets.UTF_8))
        PairedDeviceCredential(
            json.getString("baseUrl"), json.getString("deviceId"), json.getString("deviceToken")
        ).takeIf(PairedDeviceCredential::valid)
    }.getOrNull()

    fun save(credential: PairedDeviceCredential) {
        require(credential.valid()) { "invalid_device_credential" }
        val json = JSONObject()
            .put("baseUrl", credential.baseUrl.trimEnd('/'))
            .put("deviceId", credential.deviceId)
            .put("deviceToken", credential.deviceToken)
            .toString().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.iv + cipher.doFinal(json)
        check(preferences.edit().putString(
            ENCRYPTED_CREDENTIAL,
            "$FORMAT_VERSION.${Base64.encodeToString(encrypted, Base64.NO_WRAP)}"
        ).commit()) { "device_credential_write_failed" }
    }

    fun clear() {
        check(preferences.edit().remove(ENCRYPTED_CREDENTIAL).commit()) {
            "device_credential_clear_failed"
        }
    }

    fun installationId(): String {
        preferences.getString(INSTALLATION_ID, null)?.takeIf(INSTALLATION::matches)?.let {
            return it
        }
        val generated = "install-${UUID.randomUUID()}"
        check(preferences.edit().putString(INSTALLATION_ID, generated).commit()) {
            "installation_id_write_failed"
        }
        return generated
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build())
        return generator.generateKey()
    }

    private companion object {
        const val ENCRYPTED_CREDENTIAL = "credential_v1"
        const val INSTALLATION_ID = "installation_id_v1"
        const val FORMAT_VERSION = "v1"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "interpretaai.v2.device.credential"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
        const val IV_BYTES = 12
        val INSTALLATION = Regex("[A-Za-z0-9._:-]{16,128}")
    }
}
