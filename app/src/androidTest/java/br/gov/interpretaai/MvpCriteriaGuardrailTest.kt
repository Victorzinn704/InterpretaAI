package br.gov.interpretaai

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.gov.interpretaai.ui.screens.ApplyScreen
import br.gov.interpretaai.ui.screens.InterpretScreen
import br.gov.interpretaai.ui.screens.MissionScreen
import br.gov.interpretaai.ui.screens.TalkScreen
import br.gov.interpretaai.ui.theme.InterpretaTheme
import org.junit.Rule
import org.junit.Test

class MvpCriteriaGuardrailTest {
    @get:Rule val compose = createComposeRule()

    @Test fun phonemicMissionKeepsItsMainDecisionWithoutScrolling() {
        compose.setContent {
            InterpretaTheme {
                MissionScreen(AppUiState(), {}, {}, {}, {}, {})
            }
        }

        compose.onNodeWithText("RESPONDER COM A VOZ", substring = true).assertIsDisplayed()
        compose.onNodeWithText("PEDIR AJUDA", substring = true).assertIsDisplayed()
        compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
    }

    @Test fun interpretationUsesCuriosityAndDoesNotRequireScrolling() {
        compose.setContent {
            InterpretaTheme {
                InterpretScreen(AppUiState(selectedPlace = "Oficina"), {}, {}, {}, {})
            }
        }

        compose.onNodeWithText("Você observou outro lugar.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("OFICINA", substring = true).assertIsDisplayed()
        compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
    }

    @Test fun applicationOffersOnlyImplementedParticipationModes() {
        compose.setContent {
            InterpretaTheme {
                ApplyScreen(AppUiState(), {}, {}, {}, {}, {})
            }
        }

        compose.onNodeWithText("USAR CÂMERA", substring = true).assertIsDisplayed()
        compose.onNodeWithText("CONTAR À DUPLA", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Desenhar na tela", substring = true).assertDoesNotExist()
        compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
    }

    @Test fun guidedDisconnectionMakesConsciousUseVisible() {
        compose.mainClock.autoAdvance = false
        compose.setContent { InterpretaTheme { TalkScreen({}, {}, {}) } }

        compose.onNodeWithText("O celular já ajudou.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("TEMPO SEM DISTRAÇÕES", substring = true).assertIsDisplayed()
    }
}
