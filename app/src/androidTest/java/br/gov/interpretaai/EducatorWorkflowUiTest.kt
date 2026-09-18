package br.gov.interpretaai

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import br.gov.interpretaai.domain.AssignedActivity
import br.gov.interpretaai.domain.DrawingPrompt
import br.gov.interpretaai.domain.LearnerAvatars
import br.gov.interpretaai.domain.MetricsSnapshot
import br.gov.interpretaai.platform.TabletCapabilityReport
import br.gov.interpretaai.ui.screens.EducatorScreen
import br.gov.interpretaai.ui.theme.InterpretaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import java.io.File

class EducatorWorkflowUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun mainWorkflowSeparatesMissionClassroomAndTechnicalSetup() {
        val published = mutableListOf<AssignedActivity>()
        compose.setContent {
            InterpretaTheme {
                EducatorScreen(
                    metrics = MetricsSnapshot(),
                    isDeviceOwner = true,
                    hasDndAccess = true,
                    tabletReport = report(),
                    onCopyTabletReport = {},
                    onBack = {},
                    onRequestDnd = {},
                    onStartFocus = {},
                    onStopFocus = {},
                    onClearMetrics = {},
                    reducedStimuli = false,
                    onReducedStimuliChange = {},
                    challengeMode = false,
                    onChallengeModeChange = {},
                    drawingPrompt = DrawingPrompt.BALL,
                    classroomLabel = "Turma 2B",
                    learnerAlias = "pipa-07",
                    activeAvatar = LearnerAvatars.find("pipa"),
                    assignedActivity = AssignedActivity.STORY_SEQUENCE_2,
                    syncDeviceId = "tablet-room-01",
                    syncStatus = "Tablet conectado.",
                    roomSyncStatus = "Sala pronta.",
                    isSyncing = false,
                    pairingServerUrl = "https://interpretaai.example",
                    pairedV2DeviceId = "",
                    devicePairingStatus = "Tablet 2.0 ainda não pareado.",
                    isPairingDevice = false,
                    classroomSeats = emptyList(),
                    classroomSessionStatus = "Digite o código da aula para escolher esta carteira.",
                    isJoiningClassroom = false,
                    onPublishAssignment = { published += it.activity },
                    onConfigurePilotReceiver = { _, _ -> },
                    onRefreshPilotAssignment = {},
                    onPairV2Device = { _, _ -> },
                    onSyncPreparedStories = {},
                    onResolveClassroomSession = {},
                    onJoinClassroomSession = {},
                    onPublishRemoteAssignment = { _, _, _ -> },
                    onPublishRoomAssignment = { _, _, _, _, _ -> }
                )
            }
        }

        compose.onNodeWithText("PIN").performTextInput("2468")
        compose.onNodeWithText("ENTRAR").performClick()

        compose.onNodeWithText("HISTÓRIA EM FOCO").assertIsDisplayed()
        compose.onNodeWithText("EDITAR HISTÓRIA E TURMA").assertDoesNotExist()
        compose.onNodeWithText("USAR NESTE TABLET", substring = true).performClick()
        compose.runOnIdle {
            assertEquals(listOf(AssignedActivity.STORY_SEQUENCE_2), published)
        }

        compose.onNodeWithText("ALTERAR HISTÓRIA E TURMA", substring = true)
            .performScrollTo().performClick()
        compose.onNodeWithText("EDITAR HISTÓRIA E TURMA").assertIsDisplayed()
        listOf(
            AssignedActivity.NUMBER_PATH,
            AssignedActivity.CONNECT_DOTS,
            AssignedActivity.IMAGE_LETTERS
        ).forEach { activity ->
            compose.onNodeWithText(activity.label, substring = true).performScrollTo().performClick()
            compose.onNodeWithText("USAR NESTE TABLET", substring = true)
                .performScrollTo().performClick()
        }
        compose.runOnIdle {
            assertEquals(
                listOf(
                    AssignedActivity.STORY_SEQUENCE_2,
                    AssignedActivity.NUMBER_PATH,
                    AssignedActivity.CONNECT_DOTS,
                    AssignedActivity.IMAGE_LETTERS
                ),
                published
            )
        }
        compose.onNodeWithTag("mission-year-5").performScrollTo().performClick()
        compose.onNodeWithText("2 missões disponíveis neste recorte").assertIsDisplayed()
        compose.onNodeWithText("Duas fontes", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Missão do som M", substring = true).assertDoesNotExist()
        capture("educator-workflow-mission")

        compose.onNodeWithTag("educator-tab-classroom").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("MISSÃO PRONTA PARA ENVIO").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Token do tablet").assertDoesNotExist()
        capture("educator-workflow-classroom")
        compose.onNodeWithText("CONFIGURAR CONEXÃO", substring = true)
            .performScrollTo().performClick()
        compose.onNodeWithText("Token do tablet").performScrollTo().assertIsDisplayed()

        compose.onNodeWithTag("educator-tab-tablet").performScrollTo().performClick()
        compose.onNodeWithText("MODO TOTEM").assertIsDisplayed()
        compose.onNodeWithText("CONEXÃO 2.0 E MODO OFFLINE").assertIsDisplayed()
        compose.onNodeWithTag("v2-pairing-open").performScrollTo().performClick()
        compose.onNodeWithText("Código temporário", substring = true)
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("DIAGNÓSTICO DESTE TABLET")
            .performScrollTo().assertIsDisplayed()
    }

    private fun capture(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "$name-${bitmap.width}x${bitmap.height}.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun report() = TabletCapabilityReport(
        manufacturer = "Fabricante",
        model = "Tablet piloto",
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
        stylusActive = true,
        speechRecognitionAvailable = true,
        recognitionService = "recognizer",
        ttsEngine = "tts",
        deviceOwner = true,
        lockTaskPermitted = true,
        lockTaskActive = true,
        doNotDisturbAccess = true
    )
}
