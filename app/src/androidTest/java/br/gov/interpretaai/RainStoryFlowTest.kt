package br.gov.interpretaai

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import br.gov.interpretaai.ui.screens.ComicsScreen
import br.gov.interpretaai.ui.theme.InterpretaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RainStoryFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun rainIsAnIndependentStoryWithoutBallPuzzle() {
        val spoken = mutableListOf<String>()
        compose.setContent {
            InterpretaTheme { ComicsScreen(speak = spoken::add, onBack = {}) }
        }

        compose.onNodeWithText("←").performClick()
        compose.onNodeWithText("ÁGUA DA", substring = true).performClick()
        compose.onNodeWithText("A chuva começou").assertIsDisplayed()
        compose.onNodeWithText("LER • ACHE AS FOLHAS").assertIsDisplayed()
        compose.onNodeWithTag("rain-focus-0").performClick()
        compose.onNodeWithText("MONTAR A BOLA", substring = true).assertDoesNotExist()

        compose.onNodeWithText("SEGUIR AS FOLHAS", substring = true).performClick()
        compose.onNodeWithText("ENTENDER • SIGA A ÁGUA").assertIsDisplayed()
        compose.onNodeWithTag("rain-focus-1").performClick()
        compose.onNodeWithText("CONTINUAR", substring = true).performClick()
        compose.onNodeWithText("INTERPRETAR • COMPARE").assertIsDisplayed()
        compose.onNodeWithTag("rain-focus-2").performClick()
        compose.onNodeWithTag("rain-dry-route").performClick()
        compose.onNodeWithText("caminho está seco", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("rain-wet-route").performClick()
        compose.onNodeWithText("A água chegou às plantas").assertIsDisplayed()
        compose.onNodeWithText("APRENDER • EXPLIQUE A PISTA").assertIsDisplayed()
        compose.onNodeWithTag("rain-focus-3").performClick()
        compose.onNodeWithText("MONTAR A BOLA", substring = true).assertDoesNotExist()
        compose.runOnIdle {
            assertTrue(spoken.any { it.contains("folhas amarelas") })
            assertTrue(spoken.any { it.contains("raízes") })
        }
    }
}
