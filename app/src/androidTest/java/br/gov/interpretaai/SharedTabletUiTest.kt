package br.gov.interpretaai

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import br.gov.interpretaai.domain.AssignedActivity
import br.gov.interpretaai.domain.AssignedLearner
import br.gov.interpretaai.domain.LearnerAvatars
import br.gov.interpretaai.ui.screens.HomeScreen
import br.gov.interpretaai.ui.theme.InterpretaTheme
import org.junit.Rule
import org.junit.Test

class SharedTabletUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sharedTabletShowsEveryAvatarButNoInternalAlias() {
        compose.setContent {
            InterpretaTheme {
                HomeScreen(
                    onSchool = {},
                    classroomLabel = "Turma 2B",
                    learners = listOf(
                        AssignedLearner("pipa-07", LearnerAvatars.find("pipa")),
                        AssignedLearner("sol-08", LearnerAvatars.find("sol"))
                    ),
                    assignedActivity = AssignedActivity.STORY_SEQUENCE_2,
                    onEducator = {},
                    onSpeak = {},
                    onFocus = {}
                )
            }
        }

        compose.onNodeWithText("GRUPO DE 2", substring = true).assertIsDisplayed()
        compose.onNodeWithText("🪁☀️", substring = true).assertIsDisplayed()
        compose.onNodeWithText("ANTES E DEPOIS", substring = true).assertIsDisplayed()
        compose.onNodeWithText("pipa-07", substring = true).assertDoesNotExist()
        compose.onNodeWithText("sol-08", substring = true).assertDoesNotExist()
        compose.onAllNodes(hasScrollAction()).assertCountEquals(0)
    }
}
