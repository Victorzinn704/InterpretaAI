package br.gov.interpretaai.platform.storycache

import android.content.Context
import android.os.Build
import br.gov.interpretaai.BuildConfig

data class DeviceEnrollmentOutcome(
    val deviceId: String,
    val message: String,
    val availableStory: AssignedStorySummary? = null
)

/** Adult setup flow: one-time pairing, Keystore persistence and immediate offline preparation. */
class DeviceEnrollmentCoordinator(
    context: Context,
    private val cache: StoryPackCacheRepository,
    private val pairing: DevicePairingClient = DevicePairingClient()
) {
    private val application = context.applicationContext
    private val store = DeviceCredentialStore(application)

    suspend fun current(): DeviceEnrollmentOutcome {
        val credential = store.load() ?: return DeviceEnrollmentOutcome(
            "", "Tablet 2.0 ainda não pareado."
        )
        return DeviceEnrollmentOutcome(
            credential.deviceId,
            "Tablet pareado. As histórias ficam disponíveis offline após a sincronização.",
            readyStory(credential)
        )
    }

    suspend fun pair(baseUrl: String, code: String): DeviceEnrollmentOutcome {
        val profile = runCatching { deviceProfile() }.getOrElse {
            return DeviceEnrollmentOutcome("", "Não foi possível identificar este aparelho.")
        }
        return when (val result = pairing.redeem(baseUrl, code, profile)) {
            is DevicePairingResult.Paired -> {
                if (runCatching { store.save(result.credential) }.isFailure) {
                    DeviceEnrollmentOutcome(
                        "", "Não foi possível proteger a credencial neste aparelho."
                    )
                } else {
                    val synced = synchronize(result.credential)
                    synced.copy(message = "Pareado com a turma ${result.classroomId}. ${synced.message}")
                }
            }
            DevicePairingResult.InvalidCode -> DeviceEnrollmentOutcome(
                currentDeviceId(), "Código inválido, expirado ou já utilizado. Gere outro no Estúdio."
            )
            DevicePairingResult.InstallationAlreadyPaired -> DeviceEnrollmentOutcome(
                currentDeviceId(),
                "Este tablet já foi registrado. Revogue o vínculo anterior antes de parear novamente."
            )
            DevicePairingResult.RetryableFailure -> DeviceEnrollmentOutcome(
                currentDeviceId(),
                "A conexão falhou agora. O código não foi salvo; confira a rede e tente novamente."
            )
            is DevicePairingResult.Blocked -> DeviceEnrollmentOutcome(
                currentDeviceId(), "Pareamento recusado com segurança (${result.code})."
            )
        }
    }

    suspend fun syncCurrent(): DeviceEnrollmentOutcome {
        val credential = store.load() ?: return DeviceEnrollmentOutcome(
            "", "Pareie este tablet antes de buscar histórias."
        )
        return synchronize(credential)
    }

    private suspend fun synchronize(credential: PairedDeviceCredential): DeviceEnrollmentOutcome {
        val viewport = viewport()
        val result = runCatching {
            StoryPackSyncCoordinator(
                StoryPackDeliveryClient(), cache, StoryPackCursorStore(application)
            ).sync(credential, viewport)
        }.getOrElse { StoryPackSyncResult.RetryableFailure }
        val available = readyStory(credential)
        return when (result) {
            is StoryPackSyncResult.Updated -> DeviceEnrollmentOutcome(
                credential.deviceId,
                "${result.installedPackIds.size} história(s) pronta(s) para uso offline.",
                available
            )
            StoryPackSyncResult.NoChange -> DeviceEnrollmentOutcome(
                credential.deviceId, "Sincronização concluída; nenhuma atualização nova.", available
            )
            StoryPackSyncResult.NoCredential -> DeviceEnrollmentOutcome(
                credential.deviceId, "Pareamento necessário para sincronizar.", available
            )
            StoryPackSyncResult.Unauthorized -> {
                runCatching { store.clear() }
                DeviceEnrollmentOutcome(
                    "", "O pareamento foi revogado. Gere um novo código no Estúdio."
                )
            }
            StoryPackSyncResult.RetryableFailure -> {
                StoryPackSyncScheduler.scheduleNow(application)
                DeviceEnrollmentOutcome(
                    credential.deviceId,
                    "Rede instável. O conteúdo já baixado continua offline e haverá nova tentativa.",
                    available
                )
            }
            is StoryPackSyncResult.Blocked -> DeviceEnrollmentOutcome(
                credential.deviceId,
                "Atualização recusada com segurança (${result.code}); o cache anterior foi preservado.",
                available
            )
        }
    }

    private suspend fun readyStory(credential: PairedDeviceCredential): AssignedStorySummary? =
        runCatching { cache.readyAssignments(credential.deviceId, viewport()).firstOrNull() }.getOrNull()

    private fun currentDeviceId(): String = store.load()?.deviceId.orEmpty()

    private fun deviceProfile(): PairingDeviceProfile {
        val configuration = application.resources.configuration
        return PairingDeviceProfile(
            installationId = store.installationId(),
            appVersion = BuildConfig.VERSION_CODE,
            architecture = when {
                Build.SUPPORTED_ABIS.any { it == "arm64-v8a" } -> "ARM64"
                Build.SUPPORTED_ABIS.isNotEmpty() -> "UNIVERSAL"
                else -> "UNKNOWN"
            },
            viewportWidthDp = configuration.screenWidthDp.coerceIn(240, 2000),
            viewportHeightDp = configuration.screenHeightDp.coerceIn(320, 3000)
        )
    }

    private fun viewport() = if (application.resources.configuration.smallestScreenWidthDp >= 600) {
        StoryViewportClass.TABLET
    } else {
        StoryViewportClass.PHONE
    }
}
