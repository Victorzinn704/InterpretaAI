package br.gov.interpretaai

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import br.gov.interpretaai.domain.AssignedActivity
import br.gov.interpretaai.domain.AssistedAdvanceReason
import br.gov.interpretaai.ui.screens.MiniGameScreen
import br.gov.interpretaai.ui.theme.InterpretaTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class AssistedAdvanceUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun threeUnsuccessfulCheckableAttemptsLeadToAKindHandoff() {
        var reason: AssistedAdvanceReason? = null
        compose.setContent {
            InterpretaTheme {
                MiniGameScreen(
                    activity = AssignedActivity.NUMBER_PATH,
                    speak = {},
                    onBack = {},
                    onHelp = {},
                    onComplete = {},
                    onAssistedAdvance = { reason = it }
                )
            }
        }

        repeat(3) { compose.onNodeWithText("2").performClick() }
        compose.onNodeWithText("Não foi dessa vez, e tudo bem.", substring = true).assertExists()
        capture("avanco-com-apoio-tentativas")
        compose.onNodeWithText("VAMOS CONTINUAR", substring = true).performClick()

        compose.runOnIdle { assertEquals(AssistedAdvanceReason.ATTEMPT_LIMIT, reason) }
    }

    private fun capture(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "$name-${bitmap.width}x${bitmap.height}.png")
            .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
