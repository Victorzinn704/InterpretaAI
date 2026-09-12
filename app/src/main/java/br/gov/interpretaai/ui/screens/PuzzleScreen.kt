package br.gov.interpretaai.ui.screens

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.interpretaai.R
import br.gov.interpretaai.domain.PuzzleGame
import br.gov.interpretaai.domain.PuzzleSize
import br.gov.interpretaai.domain.PuzzleSubject
import br.gov.interpretaai.ui.AttentionCue
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.ComicPanel
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.GuidedScrollScreen
import br.gov.interpretaai.ui.StageHeader
import br.gov.interpretaai.ui.theme.ComicBlue
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicInk
import br.gov.interpretaai.ui.theme.ComicYellow
import br.gov.interpretaai.ui.theme.SoftGreen
import kotlin.math.roundToInt

@Composable
fun PuzzleScreen(
    speak: (String) -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onCompleted: (subject: String, level: String, moves: Int, durationMs: Long) -> Unit
) {
    var mode by rememberSaveable { mutableStateOf("menu") }
    var subjectName by rememberSaveable { mutableStateOf(PuzzleSubject.BALL.name) }
    var columns by rememberSaveable { mutableIntStateOf(2) }
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    var tiles by rememberSaveable { mutableStateOf(PuzzleGame.initialTiles(PuzzleSize.EASY)) }
    var moves by rememberSaveable { mutableIntStateOf(0) }
    var round by rememberSaveable { mutableIntStateOf(0) }
    var showHint by rememberSaveable { mutableStateOf(false) }
    var startedAt by rememberSaveable { mutableLongStateOf(0L) }
    val subject = PuzzleSubject.valueOf(subjectName)
    val puzzleSize = if (columns == 2) PuzzleSize.EASY else PuzzleSize.CHALLENGE
    val currentSpeak by rememberUpdatedState(speak)

    val narration = if (mode == "menu") {
        "Escolha uma figura e o tamanho do quebra-cabeça. Para mover, toque em uma peça e depois toque em outra."
    } else {
        "Monte a figura de ${subject.spokenWord}. Toque em uma peça e depois em outra para trocar as duas."
    }
    LaunchedEffect(mode) { currentSpeak(narration) }
    DisposableEffect(Unit) { onDispose { currentSpeak("") } }

    val leave = {
        speak("")
        if (mode == "play") mode = "menu" else onBack()
    }
    BackHandler(onBack = leave)

    GuidedScrollScreen {
        StageHeader(
            title = if (mode == "menu") "Quebra-cabeças" else "Monte a ${subject.spokenWord}",
            stage = "LEIA • BRINCAR E FALAR",
            onBack = leave,
            onSpeak = { speak(narration) }
        )

        if (mode == "menu") {
            ComicPanel {
                Text("Escolha a figura", fontSize = 22.sp, fontWeight = FontWeight.Black)
                PuzzleSubject.entries.forEach { option ->
                    ComicButton(
                        text = option.spokenWord.uppercase(),
                        onClick = {
                            subjectName = option.name
                            speak(option.spokenWord)
                        },
                        modifier = Modifier.padding(top = 10.dp),
                        color = if (subject == option) ComicYellow else Color.White,
                        leading = option.icon
                    )
                }
            }
            ComicPanel {
                Text("Escolha o tamanho", fontSize = 22.sp, fontWeight = FontWeight.Black)
                ComicButton("2 × 2 • COMEÇAR", {
                    columns = 2
                    speak("Duas por duas. Quatro peças.")
                }, Modifier.padding(top = 10.dp), color = if (columns == 2) ComicYellow else Color.White)
                ComicButton("3 × 2 • DESAFIO", {
                    columns = 3
                    speak("Três por duas. Seis peças.")
                }, color = if (columns == 3) ComicYellow else Color.White)
            }
            GuidedComicButton("MONTAR ${subject.spokenWord.uppercase()}", {
                val size = if (columns == 2) PuzzleSize.EASY else PuzzleSize.CHALLENGE
                round++
                tiles = PuzzleGame.initialTiles(size, round)
                selected = -1
                moves = 0
                showHint = false
                startedAt = SystemClock.elapsedRealtime()
                mode = "play"
            }, color = ComicBlue, trailing = "→", cue = "COMECE AQUI")
        } else {
            if (showHint) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(3.dp, ComicInk)
                ) {
                    Image(
                        painter = painterResource(subject.drawable),
                        contentDescription = "Figura completa de ${subject.spokenWord}",
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                    )
                }
            }

            if (!PuzzleGame.isComplete(tiles)) {
                AttentionCue(if (selected < 0) "TOQUE EM UMA PEÇA" else "AGORA TOQUE EM OUTRA")
            }
            PuzzleBoard(
                subject = subject,
                puzzleSize = puzzleSize,
                tiles = tiles,
                selected = selected,
                onTileClick = { position ->
                    if (PuzzleGame.isComplete(tiles)) return@PuzzleBoard
                    if (selected < 0) {
                        selected = position
                        speak("Peça escolhida. Agora toque em outra.")
                    } else if (selected == position) {
                        selected = -1
                        speak("Peça desmarcada.")
                    } else {
                        val updated = PuzzleGame.swap(tiles, selected, position)
                        tiles = updated
                        selected = -1
                        moves++
                        if (PuzzleGame.isComplete(updated)) {
                            val duration = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0)
                            speak("Parabéns! Você montou a ${subject.spokenWord}! Agora fale: ${subject.spokenWord}.")
                            onCompleted(subject.spokenWord, puzzleSize.label, moves, duration)
                        } else {
                            speak("As peças trocaram de lugar.")
                        }
                    }
                }
            )

            Text(
                if (PuzzleGame.isComplete(tiles)) "Muito bem! Você montou a ${subject.spokenWord}."
                else "Movimentos: $moves",
                fontSize = 21.sp,
                fontWeight = FontWeight.Black
            )
            ComicButton(
                if (showHint) "ESCONDER A FIGURA" else "VER A FIGURA",
                {
                    showHint = !showHint
                    if (showHint) {
                        onHelp()
                        speak("Esta é a figura de ${subject.spokenWord}. Observe e continue montando.")
                    }
                },
                color = Color.White,
                leading = "👀"
            )
            ComicButton("OUVIR A PALAVRA", {
                speak(subject.spokenWord)
            }, color = ComicYellow, leading = "🔊")
            ComicButton("MONTAR DE NOVO", {
                round++
                tiles = PuzzleGame.initialTiles(puzzleSize, round)
                selected = -1
                moves = 0
                showHint = false
                startedAt = SystemClock.elapsedRealtime()
                speak("Vamos montar ${subject.spokenWord} de novo.")
            }, color = Color.White)
            if (PuzzleGame.isComplete(tiles)) {
                ComicPanel(color = SoftGreen) {
                    Text("Fale em voz alta: ${subject.spokenWord.uppercase()}", fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text("O professor pode ouvir como a criança reconhece e pronuncia a palavra.", Modifier.padding(top = 8.dp))
                }
                GuidedComicButton(
                    "ESCOLHER OUTRA FIGURA",
                    { mode = "menu" },
                    color = ComicGreen,
                    cue = "CONTINUE BRINCANDO"
                )
            }
        }
    }
}

@Composable
private fun PuzzleBoard(
    subject: PuzzleSubject,
    puzzleSize: PuzzleSize,
    tiles: List<Int>,
    selected: Int,
    onTileClick: (Int) -> Unit
) {
    val image = ImageBitmap.imageResource(subject.drawable)
    ComicPanel(contentPadding = PaddingValues(6.dp)) {
        Column(Modifier.fillMaxWidth().aspectRatio(1f)) {
            repeat(puzzleSize.rows) { row ->
                Row(Modifier.fillMaxWidth().weight(1f)) {
                    repeat(puzzleSize.columns) { column ->
                        val position = row * puzzleSize.columns + column
                        PuzzleTile(
                            image = image,
                            sourceIndex = tiles[position],
                            columns = puzzleSize.columns,
                            rows = puzzleSize.rows,
                            position = position,
                            total = tiles.size,
                            selected = selected == position,
                            onClick = { onTileClick(position) },
                            modifier = Modifier.weight(1f).fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PuzzleTile(
    image: ImageBitmap,
    sourceIndex: Int,
    columns: Int,
    rows: Int,
    position: Int,
    total: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sourceWidth = image.width / columns
    val sourceHeight = image.height / rows
    val sourceColumn = sourceIndex % columns
    val sourceRow = sourceIndex / columns
    Canvas(
        modifier
            .padding(2.dp)
            .border(if (selected) 5.dp else 2.dp, if (selected) ComicYellow else ComicInk)
            .semantics { contentDescription = "Peça na posição ${position + 1} de $total" }
            .clickable(onClick = onClick)
    ) {
        drawImage(
            image = image,
            srcOffset = IntOffset(sourceColumn * sourceWidth, sourceRow * sourceHeight),
            srcSize = IntSize(sourceWidth, sourceHeight),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
        )
        if (selected) {
            drawRect(ComicYellow, style = Stroke(4.dp.toPx()))
        }
    }
}

private val PuzzleSubject.drawable: Int
    @DrawableRes get() = when (this) {
        PuzzleSubject.BALL -> R.drawable.puzzle_ball
        PuzzleSubject.BANANA -> R.drawable.puzzle_banana
        PuzzleSubject.APPLE -> R.drawable.puzzle_apple
    }
