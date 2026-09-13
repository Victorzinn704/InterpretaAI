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
import br.gov.interpretaai.ui.screens.ComicsScreen
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
                        clue != null -> VoiceTurnResult("Boa investigação! Vamos procurar atrás da árvore.")
                        answer != null -> VoiceTurnResult("Isso! Você percebeu que falta a bola.")
                        else -> null
                    },
                    onBallAnswer = { answer = BallAnswer.BALL },
                    onBallClueAnswer = { clue = BallClueAnswer.TREE }
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
        tap("ATRÁS DA ÁRVORE")
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
        tap("USAR: ATRÁS DA ÁRVORE")
        tap("CONTAR AO GRUPO")
        capture("percurso-bola-9-colaborar")
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
