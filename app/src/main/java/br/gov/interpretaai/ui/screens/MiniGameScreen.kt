package br.gov.interpretaai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import br.gov.interpretaai.R
import br.gov.interpretaai.domain.AssignedActivity
import br.gov.interpretaai.domain.AssistedAdvanceReason
import br.gov.interpretaai.domain.MiniGameRules
import br.gov.interpretaai.ui.AssistedAdvanceStage
import br.gov.interpretaai.ui.ChildStageScaffold
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.ComicPanel
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.rememberReengagementVisual
import br.gov.interpretaai.ui.rememberAssistedAdvanceReason
import br.gov.interpretaai.ui.theme.ComicBlue
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicInk
import br.gov.interpretaai.ui.theme.ComicYellow

@Composable
fun MiniGameScreen(
    activity: AssignedActivity,
    speak: (String) -> Unit,
    voiceBusy: Boolean = false,
    reducedStimuli: Boolean = false,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onComplete: () -> Unit,
    onAssistedAdvance: (AssistedAdvanceReason) -> Unit = {}
) {
    val tablet = LocalConfiguration.current.screenWidthDp >= 600
    var progress by rememberSaveable(activity) { mutableIntStateOf(0) }
    var hint by rememberSaveable(activity) { mutableStateOf(false) }
    var interactionNonce by rememberSaveable(activity) { mutableIntStateOf(0) }
    var unsuccessfulAttempts by rememberSaveable(activity) { mutableIntStateOf(0) }
    val letters = activity == AssignedActivity.IMAGE_LETTERS
    val dots = activity == AssignedActivity.CONNECT_DOTS
    val total = if (letters) 4 else 5
    val done = progress == total
    val advanceReason = rememberAssistedAdvanceReason(
        stageKey = activity.name,
        unsuccessfulAttempts = unsuccessfulAttempts,
        hasCheckableAnswer = true,
        busy = voiceBusy || done
    )
    val intro = when (activity) {
        AssignedActivity.NUMBER_PATH -> "Tem paredes no caminho. Vamos do um ao cinco? Toque no número que vem depois."
        AssignedActivity.CONNECT_DOTS -> "Vamos ligar os pontos do um ao cinco. Que figura será que aparece?"
        else -> "Olhe bem a figura. Agora monte o nome dela, letra por letra."
    }
    LaunchedEffect(activity) { speak("Oi! Eu sou a LÉIA, e este é o Alfa. $intro") }
    val reconnecting = rememberReengagementVisual(
        stageKey = "${activity.name}:$progress",
        interactionNonce = interactionNonce,
        busy = voiceBusy || done,
        reducedStimuli = reducedStimuli,
        speak = speak,
        spokenPrompt = "Ei, eu e o Alfa estamos aqui. Quer uma pista?"
    )

    fun chooseNumber(value: Int) {
        interactionNonce++
        if (MiniGameRules.acceptsNumber(progress, value, dots)) {
            progress++
            hint = false
            speak(if (progress == total) {
                if (dots) "Você desenhou uma casa! Casa começa com o som da letra C. Conte ao colega o que apareceu."
                else "Você chegou ao cinco! Conte ao colega qual número veio antes."
            } else value.toString())
        } else {
            unsuccessfulAttempts++
            if (!hint) {
                hint = true
                speak("Quase! Qual número vem logo depois? Se quiser, eu dou uma pista.")
            }
        }
    }

    fun chooseLetter(value: Char) {
        interactionNonce++
        if (MiniGameRules.acceptsLetter(progress, value)) {
            progress++
            hint = false
            speak(if (progress == total) "Você formou bola! B de bola tem o som /b/. Mostre a figura ao colega."
                else if (value == 'B') "Bê, som /b/" else value.toString())
        } else {
            unsuccessfulAttempts++
            if (!hint) {
                hint = true
                speak("Quase! Diga bola devagar e escute qual letra vem agora.")
            }
        }
    }

    if (advanceReason != null) {
        AssistedAdvanceStage(advanceReason, speak) { onAssistedAdvance(advanceReason) }
        return
    }

    ChildStageScaffold {
        ComicPanel(color = ComicBlue) {
            Text(activity.label.uppercase(), color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Text("LÉIA E ALFA • UMA MISSÃO POR VEZ", color = Color.White, fontSize = 16.sp)
        }
        Text(
            when {
                done -> if (dots) "UMA CASA!" else if (letters) "BOLA!" else "CHEGOU AO 5!"
                hint -> "Qual vem depois? LÉIA e Alfa podem dar uma pista."
                letters -> "QUAL LETRA VEM AGORA?"
                else -> "QUAL NÚMERO VEM AGORA?"
            },
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Black,
            fontSize = if (tablet) 25.sp else 19.sp,
            color = if (reconnecting) ComicBlue else ComicInk
        )
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (activity) {
                AssignedActivity.NUMBER_PATH -> NumberMaze(progress, tablet, ::chooseNumber)
                AssignedActivity.CONNECT_DOTS -> ConnectDots(progress, tablet, ::chooseNumber)
                else -> PictureLetters(progress, tablet, ::chooseLetter)
            }
        }
        if (done) {
            GuidedComicButton("CONTAR PARA A TURMA", onComplete, color = ComicGreen, cue = "AGORA O TABLET DESCANSA")
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ComicButton("OUVIR", { interactionNonce++; speak(intro) }, Modifier.weight(1f), color = ComicYellow, leading = "🔊")
                ComicButton("AJUDA", {
                    interactionNonce++
                    hint = true
                    onHelp()
                    speak(if (letters) "Procure a letra ${MiniGameRules.ballLetters[progress]}."
                        else "Procure o número ${progress + 1}.")
                }, Modifier.weight(1f), color = Color.White, leading = "💡")
            }
        }
        ComicButton("VOLTAR", onBack, color = Color.White)
    }
}

@Composable
private fun NumberMaze(progress: Int, tablet: Boolean, choose: (Int) -> Unit) {
    val cells = listOf(1, 0, 0, 2, 3, 0, 0, 4, 5)
    ComicPanel(modifier = Modifier.widthIn(max = if (tablet) 660.dp else 520.dp), color = ComicYellow) {
        Text("SAÍDA 1 → CHEGADA 5", fontWeight = FontWeight.Black, fontSize = 16.sp)
        cells.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { number ->
                    val passed = number in 1..progress
                    Surface(
                        modifier = Modifier.weight(1f).height(if (tablet) 100.dp else 72.dp)
                            .semantics {
                                if (number == 0) contentDescription = "Parede; caminho fechado"
                                else stateDescription = if (passed) "Já percorrido" else "Número disponível"
                            }
                            .clickable(enabled = number > progress && progress < 5) { choose(number) },
                        color = when { number == 0 -> ComicInk.copy(alpha = .12f); passed -> ComicGreen; else -> Color.White },
                        shape = RoundedCornerShape(15.dp),
                        border = BorderStroke(2.dp, ComicInk)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(if (number == 0) "🧱" else if (passed) "✓ $number" else "$number",
                                fontSize = 26.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectDots(progress: Int, tablet: Boolean, choose: (Int) -> Unit) {
    val positions = listOf(.17f to .78f, .17f to .48f, .5f to .16f, .83f to .48f, .83f to .78f)
    val boardHeight = if (tablet) 360.dp else 255.dp
    val dotSize = if (tablet) 76.dp else 58.dp
    ComicPanel(modifier = Modifier.widthIn(max = if (tablet) 660.dp else 520.dp), color = ComicYellow) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(boardHeight)) {
            Canvas(Modifier.matchParentSize()) {
                for (index in 0 until (progress - 1).coerceAtLeast(0)) {
                    val a = positions[index]
                    val b = positions[index + 1]
                    drawLine(ComicBlue, Offset(size.width * a.first, size.height * a.second),
                        Offset(size.width * b.first, size.height * b.second), 8.dp.toPx(), cap = StrokeCap.Round)
                }
                if (progress == 5) {
                    drawLine(ComicBlue, Offset(size.width * .83f, size.height * .78f),
                        Offset(size.width * .17f, size.height * .78f), 8.dp.toPx(), cap = StrokeCap.Round)
                }
            }
            positions.forEachIndexed { index, pair ->
                Surface(
                    modifier = Modifier.offset(maxWidth * pair.first - dotSize / 2,
                        boardHeight * pair.second - dotSize / 2)
                        .size(dotSize)
                        .semantics { stateDescription = if (index < progress) "Já ligado" else "Ponto disponível" }
                        .clickable(enabled = index >= progress && progress < 5) { choose(index + 1) },
                    color = if (index < progress) ComicGreen else Color.White,
                    shape = RoundedCornerShape(50), border = BorderStroke(3.dp, ComicInk)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${index + 1}", fontSize = 23.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun PictureLetters(progress: Int, tablet: Boolean, choose: (Char) -> Unit) {
    ComicPanel(modifier = Modifier.widthIn(max = if (tablet) 660.dp else 520.dp), color = ComicYellow) {
        Image(
            painter = painterResource(R.drawable.ball_photo_v1),
            contentDescription = "Bola de futebol",
            modifier = Modifier.fillMaxWidth().height(if (tablet) 240.dp else 155.dp),
            contentScale = ContentScale.Fit
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            MiniGameRules.ballLetters.forEachIndexed { index, letter ->
                Text(if (index < progress) "$letter " else "_ ", fontSize = 30.sp, fontWeight = FontWeight.Black)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf('L', 'A', 'B', 'O').forEach { letter ->
                Surface(
                    modifier = Modifier.weight(1f).height(if (tablet) 86.dp else 64.dp)
                        .semantics {
                            stateDescription = if (letter in MiniGameRules.ballLetters.take(progress))
                                "Letra já usada" else "Letra disponível"
                        }
                        .clickable(enabled = progress < 4 &&
                        letter !in MiniGameRules.ballLetters.take(progress)) { choose(letter) },
                    color = if (letter in MiniGameRules.ballLetters.take(progress)) ComicGreen else Color.White,
                    shape = RoundedCornerShape(14.dp), border = BorderStroke(2.dp, ComicInk)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("$letter", fontSize = 25.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
