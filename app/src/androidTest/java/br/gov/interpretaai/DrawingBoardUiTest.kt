package br.gov.interpretaai

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import br.gov.interpretaai.domain.DrawingPrompt
import br.gov.interpretaai.ui.screens.DrawingBoardScreen
import br.gov.interpretaai.ui.theme.InterpretaTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DrawingBoardUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun tapAndLongStrokeCanBeUndoneAndRedoneWithoutLosingTheBoard() {
        var spoken = ""
        compose.setContent {
            InterpretaTheme {
                DrawingBoardScreen(
                    prompt = DrawingPrompt.TREE,
                    speak = { spoken = it },
                    onBack = {},
                    onComplete = {}
                )
            }
        }

        compose.onNodeWithText("↶").assertIsNotEnabled()
        compose.onNodeWithTag("drawing-canvas").performTouchInput { click(center) }
        compose.onNodeWithTag("drawing-canvas").performTouchInput {
            swipe(Offset(width * .2f, height * .7f), Offset(width * .8f, height * .3f), 500)
        }
        compose.onNodeWithText("↶").assertIsEnabled().performClick()
        compose.onNodeWithText("↷").assertIsEnabled().performClick()
        compose.onNodeWithTag("drawing-eraser").performClick()
        assertTrue(spoken.contains("Borracha ligada"))
        compose.onNodeWithTag("drawing-clear").performClick()
        assertTrue(spoken.contains("Quadro limpo"))
        compose.onNodeWithContentDescription("LÉIA e Alfa acompanham esta etapa").assertExists()
    }
}
