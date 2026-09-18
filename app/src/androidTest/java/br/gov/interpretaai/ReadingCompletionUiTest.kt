package br.gov.interpretaai

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
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

    @Test fun reducedStimuliKeepsLeiaAndMessageButRemovesDecorativeMotion() {
        compose.setContent {
            InterpretaTheme {
                CompleteScreen(
                    onSpeak = {},
                    onHome = {},
                    reducedStimuli = true
                )
            }
        }

        compose.onNodeWithTag("leia-reaction-scene").assertIsDisplayed()
        compose.onNodeWithTag("leia-reaction-motion").assertDoesNotExist()
        compose.onNodeWithText("VOCÊ AJUDOU A LÉIA!", substring = true).assertIsDisplayed()
    }
}
