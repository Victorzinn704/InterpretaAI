package br.gov.interpretaai.ui.screens

import androidx.annotation.DrawableRes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.interpretaai.R
import br.gov.interpretaai.ui.ChildStageScaffold
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.ComicPanel
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.InteractiveComicPortrait
import br.gov.interpretaai.ui.Pill
import br.gov.interpretaai.ui.StageHeader
import br.gov.interpretaai.ui.theme.ComicBlue
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicYellow
import br.gov.interpretaai.ui.theme.SoftBlue

private data class RainPage(
    val title: String,
    val methodCue: String,
    val narration: String,
    val description: String,
    @DrawableRes val image: Int,
    val focusLabel: String,
    val focusX: Float,
    val focusY: Float,
    val focusReaction: String,
    val action: String
)

private val rainPages = listOf(
    RainPage(
        "A chuva começou",
        "LER • ACHE AS FOLHAS",
        "A chuva caiu no pátio. Algumas folhas amarelas entraram na água. Vamos descobrir para onde essa água vai?",
        "Lia, Davi e Alfa observam a chuva levar folhas pelo pátio",
        R.drawable.comic_rain_path_01_shelter_v1,
        "Folhas amarelas na água",
        .24f,
        .70f,
        "Você achou as folhas amarelas! Veja para onde a água leva as folhas.",
        "SEGUIR AS FOLHAS"
    ),
    RainPage(
        "A água faz uma curva",
        "ENTENDER • SIGA A ÁGUA",
        "A água desceu pelo chão, deu a volta na pedra e carregou as folhas junto. Para onde ela continua?",
        "A água da chuva contorna uma pedra e carrega três folhas amarelas",
        R.drawable.comic_rain_path_02_stream_v1,
        "Água contornando a pedra",
        .61f,
        .72f,
        "Olha só! A água passa ao redor da pedra e continua seu caminho.",
        "CONTINUAR SEGUINDO"
    ),
    RainPage(
        "Onde está a pista?",
        "INTERPRETAR • COMPARE",
        "Compare os dois caminhos. Em qual deles aparecem água e as mesmas folhas amarelas?",
        "Lia, Davi e Alfa comparam o caminho seco com o caminho molhado até a ponte",
        R.drawable.comic_rain_path_03_routes_v1,
        "Folhas no caminho molhado",
        .75f,
        .68f,
        "Você achou água e folhas no mesmo caminho. Qual opção combina com essa pista?",
        ""
    ),
    RainPage(
        "A água chegou às plantas",
        "APRENDER • EXPLIQUE A PISTA",
        "A água passou por baixo da ponte e molhou a terra perto das raízes. Conte para alguém: como você sabe que ela chegou até ali?",
        "Lia, Davi e Alfa descobrem a água da chuva chegando às plantas do jardim",
        R.drawable.comic_rain_path_04_garden_v1,
        "Terra molhada junto às raízes",
        .29f,
        .64f,
        "Você encontrou a terra molhada! A água chegou até as raízes.",
        "CONTEI O QUE DESCOBRI"
    )
)

@Composable
fun RainStoryScreen(
    speak: (String) -> Unit,
    onBack: () -> Unit,
    onCompleted: () -> Unit,
    reducedStimuli: Boolean = false
) {
    var pageIndex by rememberSaveable { mutableIntStateOf(0) }
    var tryAgain by rememberSaveable { mutableStateOf(false) }
    val page = rainPages[pageIndex]

    BackHandler(onBack = onBack)
    LaunchedEffect(pageIndex) {
        tryAgain = false
        speak(page.narration)
    }

    ChildStageScaffold { compact ->
        StageHeader("A água da chuva", "LEIA • OBSERVAR E DESCOBRIR", onBack) {
            speak(page.narration)
        }
        Pill(page.methodCue, ComicYellow)
        Text(page.title, fontSize = 21.sp, fontWeight = FontWeight.Black)
        InteractiveComicPortrait(
            description = page.description,
            focusLabel = page.focusLabel,
            focusX = page.focusX,
            focusY = page.focusY,
            onFocusFound = { speak(page.focusReaction) },
            imageAspectRatio = if (compact) 16f / 9f else 4f / 3f,
            drawableRes = page.image,
            reducedStimuli = reducedStimuli,
            tag = "rain-focus-$pageIndex"
        )
        ComicPanel(color = if (tryAgain) ComicYellow else SoftBlue) {
            Text(
                if (tryAgain) "Este caminho está seco. Procure onde aparecem água e folhas amarelas."
                else page.narration,
                fontSize = if (compact) 16.sp else 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (pageIndex == 2) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ComicButton(
                    "MOLHADO",
                    { speak("Isso! A água e as folhas estão no caminho molhado."); pageIndex++ },
                    Modifier.weight(1f).testTag("rain-wet-route"),
                    color = ComicGreen,
                    leading = "💧"
                )
                ComicButton(
                    "SECO",
                    { tryAgain = true; speak("Este caminho está seco. Procure onde aparecem água e folhas amarelas.") },
                    Modifier.weight(1f).testTag("rain-dry-route"),
                    color = Color.White,
                    leading = "☀️"
                )
            }
        } else {
            GuidedComicButton(
                page.action,
                {
                    if (pageIndex == rainPages.lastIndex) onCompleted() else pageIndex++
                },
                color = if (pageIndex == rainPages.lastIndex) ComicGreen else ComicBlue,
                trailing = if (pageIndex == rainPages.lastIndex) "✓" else "→",
                cue = if (pageIndex == rainPages.lastIndex) "DEPOIS DE CONTAR" else "CONTINUE A INVESTIGAÇÃO"
            )
        }
    }
}
