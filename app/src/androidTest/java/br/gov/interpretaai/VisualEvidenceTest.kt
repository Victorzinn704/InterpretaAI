package br.gov.interpretaai

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import br.gov.interpretaai.domain.BallAnswer
import br.gov.interpretaai.domain.BallClueAnswer
import br.gov.interpretaai.domain.ReadingMissionPack
import br.gov.interpretaai.domain.AssignedActivity
import br.gov.interpretaai.domain.AssignedLearner
import br.gov.interpretaai.domain.LearnerAvatars
import br.gov.interpretaai.ui.screens.ComicsScreen
import br.gov.interpretaai.ui.screens.CompleteScreen
import br.gov.interpretaai.ui.screens.HomeScreen
import br.gov.interpretaai.ui.screens.PuzzleScreen
import br.gov.interpretaai.ui.theme.InterpretaTheme
import br.gov.interpretaai.platform.VoiceTurnResult
import java.io.File
import org.junit.Rule
import org.junit.Test

/** Gera evidência real da interface; os arquivos ficam no diretório interno do APK de teste. */
class VisualEvidenceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun capturesStoryConnection() {
        var answer by mutableStateOf<BallAnswer?>(null)
        var clue by mutableStateOf<BallClueAnswer?>(null)
        compose.setContent {
            InterpretaTheme {
                ComicsScreen(
                    speak = {},
                    onBack = {},
                    ballAnswer = answer,
                    ballClueAnswer = clue,
                    leiaReply = when {
                        clue == BallClueAnswer.TREE -> VoiceTurnResult("Boa investigação! Vamos procurar atrás da árvore.")
                        clue == BallClueAnswer.OTHER -> VoiceTurnResult("Essa é uma possibilidade. Compare a mochila com as marcas da imagem e investigue outra vez.")
                        answer != null -> VoiceTurnResult("Isso! Você percebeu que falta a bola.")
                        else -> null
                    },
                    onBallAnswer = { answer = BallAnswer.BALL },
                    onBallClueAnswer = { clue = BallClueAnswer.TREE },
                    onBallClueOther = { clue = BallClueAnswer.OTHER }
                )
            }
        }
        capture("percurso-bola-1-ouvir")
        tap("EU OBSERVEI")
        capture("percurso-bola-2-responder")
        tap("BOLA")
        capture("percurso-bola-3-conectar")
        tap("SEGUIR AS PISTAS")
        capture("percurso-bola-4-investigar")
        tap("RESPONDER COM FIGURAS")
        capture("percurso-bola-4b-alternativas")
        tap("MOCHILA")
        capture("percurso-bola-4c-tentar-novamente")
        tap("ÁRVORE")
        capture("percurso-bola-5-explicar")
    }

    @Test fun capturesPuzzleAndGroupClosure() {
        compose.setContent {
            InterpretaTheme {
                PuzzleScreen(
                    speak = {},
                    onBack = {},
                    onHelp = {},
                    guided = true,
                    onCompleted = { _, _, _, _ -> }
                )
            }
        }
        capture("percurso-bola-6-manipular")
        swap(1, 4)
        swap(2, 4)
        swap(3, 4)
        capture("percurso-bola-7-palavra-som")
        tap("USAR NA HISTÓRIA")
        capture("percurso-bola-8-aplicar")
        tap("PRECISO DE UMA PISTA")
        capture("percurso-bola-8b-ajuda-progressiva")
        tap("USAR A PISTA COM A LEIA")
        tap("CONTAR AO GRUPO")
        capture("percurso-bola-9-colaborar")
    }

    @Test fun capturesFifthYearReadingClosure() {
        compose.setContent {
            InterpretaTheme {
                CompleteScreen(
                    completion = ReadingMissionPack.COMPARE_SOURCES.completion,
                    onSpeak = {},
                    onHome = {}
                )
            }
        }
        capture("reading-pack-5-closure")
    }

    @Test fun capturesSharedTabletHomeWithoutAliases() {
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
        capture("shared-tablet-home")
    }

    @Test fun capturesSharedTabletCollaborativeTurn() {
        compose.setContent {
            InterpretaTheme {
                ComicsScreen(
                    speak = {},
                    onBack = {},
                    learners = listOf(
                        AssignedLearner("pipa-07", LearnerAvatars.find("pipa")),
                        AssignedLearner("sol-08", LearnerAvatars.find("sol"))
                    )
                )
            }
        }
        capture("shared-tablet-collaborative-turn")
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text, substring = true).performClick()
    }

    private fun swap(first: Int, second: Int) {
        compose.onNodeWithContentDescription("Peça na posição $first de 4").performClick()
        compose.onNodeWithContentDescription("Peça na posição $second de 4").performClick()
    }

    private fun capture(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "$name-${bitmap.width}x${bitmap.height}.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
