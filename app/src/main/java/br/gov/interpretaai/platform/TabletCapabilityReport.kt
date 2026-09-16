package br.gov.interpretaai.platform

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.speech.SpeechRecognizer
import android.view.InputDevice
import kotlin.math.min
import kotlin.math.roundToInt

enum class TabletReadiness(val label: String) {
    READY("APTO PARA PILOTO"),
    LIMITED("APTO COM RESSALVAS"),
    INCOMPATIBLE("INCOMPATÍVEL COM O APK ATUAL")
}

data class TabletCapabilityReport(
    val manufacturer: String,
    val model: String,
    val device: String,
    val androidRelease: String,
    val androidSdk: Int,
    val securityPatch: String,
    val supportedAbis: String,
    val screenPixels: String,
    val shortestSideDp: Int,
    val densityDpi: Int,
    val touchscreen: Boolean,
    val microphone: Boolean,
    val camera: Boolean,
    val stylusActive: Boolean,
    val speechRecognitionAvailable: Boolean,
    val recognitionService: String,
    val ttsEngine: String,
    val deviceOwner: Boolean,
    val lockTaskPermitted: Boolean,
    val lockTaskActive: Boolean,
    val doNotDisturbAccess: Boolean
) {
    val readiness: TabletReadiness = evaluateReadiness(
        androidSdk, shortestSideDp, touchscreen, microphone,
        camera, speechRecognitionAvailable, ttsEngine
    )

    val recommendations: List<String> = buildList {
        if (!lockTaskPermitted) add("Provisionar Device Owner para bloqueio total; fixação comum exige confirmação adulta.")
        if (!doNotDisturbAccess) add("Conceder acesso a Não Perturbe antes da aula.")
        if (!camera) add("Missões com fotografia ficam indisponíveis; gibi, voz, desenho e quebra-cabeça continuam.")
        if (!microphone || !speechRecognitionAvailable) add("Validar microfone e serviço de reconhecimento para participação por voz.")
        if (ttsEngine == "não identificado") add("Instalar e testar uma voz pt-BR antes do piloto.")
        if (!stylusActive) add("Caneta não detectada agora; desenho por dedo continua disponível.")
        if (securityPatch.isBlank()) add("Confirmar política de atualizações com o MDM da rede.")
    }

    fun exportText(): String = buildString {
        appendLine("INTERPRETAAI_TABLET_AUDIT_V3")
        appendLine("manufacturer=$manufacturer")
        appendLine("model=$model")
        appendLine("device=$device")
        appendLine("android=$androidRelease sdk=$androidSdk security_patch=${securityPatch.ifBlank { "unknown" }}")
        appendLine("abis=$supportedAbis")
        appendLine("screen=$screenPixels shortest_dp=$shortestSideDp density_dpi=$densityDpi")
        appendLine("touchscreen=$touchscreen microphone=$microphone camera=$camera stylus_active=$stylusActive")
        appendLine("speech_recognition=$speechRecognitionAvailable service=$recognitionService")
        appendLine("tts_engine=$ttsEngine")
        appendLine("device_owner=$deviceOwner lock_task_permitted=$lockTaskPermitted lock_task_active=$lockTaskActive")
        appendLine("dnd_access=$doNotDisturbAccess readiness=${readiness.name}")
        append("privacy=no_serial_no_imei_no_account_no_child_data")
    }

    companion object {
        fun evaluateReadiness(
            androidSdk: Int,
            shortestSideDp: Int,
            touchscreen: Boolean,
            microphone: Boolean,
            camera: Boolean,
            speechRecognitionAvailable: Boolean,
            ttsEngine: String
        ): TabletReadiness = when {
            androidSdk < 26 || shortestSideDp < 320 || !touchscreen -> TabletReadiness.INCOMPATIBLE
            !microphone || !camera || !speechRecognitionAvailable || ttsEngine == "não identificado" ->
                TabletReadiness.LIMITED
            else -> TabletReadiness.READY
        }
    }
}

object TabletCapabilityCollector {
    fun collect(
        context: Context,
        deviceOwner: Boolean,
        lockTaskPermitted: Boolean,
        doNotDisturbAccess: Boolean
    ): TabletCapabilityReport {
        val packageManager = context.packageManager
        val metrics = context.resources.displayMetrics
        val widthDp = (metrics.widthPixels / metrics.density).roundToInt()
        val heightDp = (metrics.heightPixels / metrics.density).roundToInt()
        val recognitionService = Settings.Secure.getString(
            context.contentResolver, "voice_recognition_service"
        ).orEmpty().ifBlank { "não identificado" }
        val ttsEngine = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.TTS_DEFAULT_SYNTH
        ).orEmpty().ifBlank { "não identificado" }
        val stylusActive = InputDevice.getDeviceIds().any { deviceId ->
            InputDevice.getDevice(deviceId)?.supportsSource(InputDevice.SOURCE_STYLUS) == true
        }
        val activityManager = context.getSystemService(ActivityManager::class.java)

        return TabletCapabilityReport(
            manufacturer = Build.MANUFACTURER.ifBlank { "não identificado" },
            model = Build.MODEL.ifBlank { "não identificado" },
            device = Build.DEVICE.ifBlank { "não identificado" },
            androidRelease = Build.VERSION.RELEASE.ifBlank { "não identificado" },
            androidSdk = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH.orEmpty(),
            supportedAbis = Build.SUPPORTED_ABIS.joinToString(",").ifBlank { "não identificado" },
            screenPixels = "${metrics.widthPixels}x${metrics.heightPixels}",
            shortestSideDp = min(widthDp, heightDp),
            densityDpi = metrics.densityDpi,
            touchscreen = packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN),
            microphone = packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE),
            camera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY),
            stylusActive = stylusActive,
            speechRecognitionAvailable = SpeechRecognizer.isRecognitionAvailable(context),
            recognitionService = recognitionService,
            ttsEngine = ttsEngine,
            deviceOwner = deviceOwner,
            lockTaskPermitted = lockTaskPermitted,
            lockTaskActive = activityManager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE,
            doNotDisturbAccess = doNotDisturbAccess
        )
    }
}

