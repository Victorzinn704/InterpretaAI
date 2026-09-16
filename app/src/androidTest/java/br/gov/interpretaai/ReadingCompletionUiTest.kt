package br.gov.interpretaai

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.gov.interpretaai.domain.ReadingMissionPack
import br.gov.interpretaai.ui.screens.CompleteScreen
import br.gov.interpretaai.ui.theme.InterpretaTheme
import org.junit.Rule
import org.junit.Test

class ReadingCompletionUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fifthYearClosureCelebratesEvidenceAndReturnsToTheBeginning() {
        compose.setContent {
            InterpretaTheme {
                CompleteScreen(
                    completion = ReadingMissionPack.COMPARE_SOURCES.completion,
                    onSpeak = {},
                    onHome = {}
                )
            }
        }

        compose.onNodeWithText("VOCÊ COMPAROU AS FONTES!").assertIsDisplayed()
        compose.onNodeWithText("data, quantidade", substring = true).assertIsDisplayed()
        compose.onNodeWithText("tablet descansa", substring = true).assertIsDisplayed()
        compose.onNodeWithText("VOLTAR AO INÍCIO", substring = true).assertIsDisplayed()
        compose.onNodeWithText("letrinha M", substring = true).assertDoesNotExist()
    }
}
