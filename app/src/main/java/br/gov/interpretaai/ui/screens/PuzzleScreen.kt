package br.gov.interpretaai.ui.screens

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
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
import br.gov.interpretaai.domain.BallAnswer
import br.gov.interpretaai.domain.BallAnswerResolver
import br.gov.interpretaai.domain.BallInstruction
import br.gov.interpretaai.domain.BallInstructionResolver
import br.gov.interpretaai.domain.ResponseModality
import br.gov.interpretaai.ui.AttentionCue
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.ComicPanel
import br.gov.interpretaai.ui.ChildStageScaffold
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.StageHeader
import br.gov.interpretaai.ui.Pill
import br.gov.interpretaai.ui.LocalSoundEffect
import br.gov.interpretaai.ui.rememberPuzzleGuidance
import br.gov.interpretaai.ui.rememberReengagementVisual
import br.gov.interpretaai.ui.ComicPortrait
import br.gov.interpretaai.platform.SoundCue
import br.gov.interpretaai.ui.theme.ComicBlue
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicInk
import br.gov.interpretaai.ui.theme.ComicYellow
import br.gov.interpretaai.ui.theme.SoftGreen
import kotlin.math.roundToInt
import kotlin.math.abs

@Composable
fun PuzzleScreen(
    speak: (String) -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onCompleted: (subject: String, level: String, moves: Int, durationMs: Long) -> Unit,
    guided: Boolean = false,
    challengeMode: Boolean = false,
    listen: (((String) -> Unit) -> Unit) = {},
    voiceBusy: Boolean = false,
    listening: Boolean = false,
    voiceMessage: String? = null,
    reducedStimuli: Boolean = false,
    onApplication: (ResponseModality) -> Unit = {},
    onGuidedFinished: () -> Unit = {}
) {
    val guidedColumns = if (challengeMode) 3 else 2
    val guidedSize = if (challengeMode) PuzzleSize.CHALLENGE else PuzzleSize.EASY
    var mode by rememberSaveable(guided, challengeMode) { mutableStateOf(if (guided) "play" else "menu") }
    var subjectName by rememberSaveable { mutableStateOf(PuzzleSubject.BALL.name) }
    var columns by rememberSaveable(guided, challengeMode) { mutableIntStateOf(if (guided) guidedColumns else 2) }
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    var tiles by rememberSaveable(guided, challengeMode) { mutableStateOf(PuzzleGame.initialTiles(if (guided) guidedSize else PuzzleSize.EASY)) }
    var moves by rememberSaveable { mutableIntStateOf(0) }
    var round by rememberSaveable { mutableIntStateOf(0) }
    var showHint by rememberSaveable { mutableStateOf(false) }
    var startedAt by rememberSaveable { mutableLongStateOf(0L) }
    var interactionNonce by rememberSaveable { mutableIntStateOf(0) }
    var repeatFeedback by rememberSaveable { mutableStateOf("") }
    var applicationDone by rememberSaveable { mutableStateOf(false) }
    var showApplicationHelp by rememberSaveable { mutableStateOf(false) }
    val subject = PuzzleSubject.valueOf(subjectName)
    val puzzleSize = if (columns == 2) PuzzleSize.EASY else PuzzleSize.CHALLENGE
    val currentSpeak by rememberUpdatedState(speak)
    val playSound = LocalSoundEffect.current

    val narration = if (mode == "menu") {
        "Escolha uma figura e o tamanho do quebra-cabeça. Para mover, toque em uma peça e depois toque em outra."
    } else if (mode == "apply") {
        "Agora use o que você entendeu. Diga a Davi onde ele deve procurar a bola."
    } else if (mode == "group") {
        "Agora o celular descansa. Conte ao colega qual pista resolveu o mistério e troquem de papel."
    } else {
        "Monte a figura de ${subject.spokenWord}. Você pode tocar em duas peças ou arrastar uma peça."
    }
    LaunchedEffect(mode) {
        if (mode == "play" && startedAt == 0L) startedAt = SystemClock.elapsedRealtime()
        currentSpeak(narration)
    }
    DisposableEffect(Unit) { onDispose { currentSpeak("") } }

    val leave = {
        speak("")
        if (mode == "play" && !guided) mode = "menu" else onBack()
    }
    BackHandler(onBack = leave)

    val applicationReconnecting = rememberReengagementVisual(
        stageKey = "application-$round",
        interactionNonce = interactionNonce,
        busy = mode != "apply" || listening || voiceBusy || applicationDone,
        reducedStimuli = reducedStimuli,
        speak = speak,
        spokenPrompt = "Davi ainda precisa da sua pista. Quer tentar comigo?"
    )

    ChildStageScaffold { compact ->
        StageHeader(
            title = when (mode) {
                "menu" -> "Quebra-cabeças"
                "group" -> "Aprender juntos"
                "apply" -> "Use sua descoberta"
                else -> "Monte a ${subject.spokenWord}"
            },
            stage = if (guided) "PERCURSO BOLA • LEIA" else "LEIA • BRINCAR E FALAR",
            onBack = leave,
            onSpeak = { speak(narration) }
        )
        voiceMessage?.let { Text(it, fontSize = 16.sp, fontWeight = FontWeight.Bold) }

        if (mode == "group") {
            ComicPanel(color = SoftGreen) {
                Text("📱  →  👫", fontSize = 50.sp, fontWeight = FontWeight.Black)
                Text("Agora o celular descansa.", fontSize = 25.sp, fontWeight = FontWeight.Black)
                Text("Conte ao colega: qual pista mostrou onde a bola estava?", fontSize = 20.sp)
                Text("Depois troquem de papel: uma criança dá a pista e a outra procura.", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(if (compact) 1f else .35f))
            GuidedComicButton(
                "TERMINAMOS JUNTOS",
                onGuidedFinished,
                color = ComicGreen,
                leading = "🤝",
                cue = "DEPOIS DA CONVERSA"
            )
        } else if (mode == "apply") {
            ComicPanel(color = SoftGreen) {
                Text("APRENDER • USE O QUE ENTENDEU", fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text("Davi está esperando sua orientação.", Modifier.padding(top = 6.dp), fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text("Diga onde ele deve procurar a bola e use a pista da história.", fontSize = 17.sp)
            }
            if (applicationReconnecting) AttentionCue("DÊ UMA ORIENTAÇÃO PARA DAVI")
            if (repeatFeedback.isBlank()) {
                ComicPortrait(
                    0,
                    "Davi espera uma orientação para procurar a bola",
                    imageAspectRatio = if (compact) 16f / 7f else 2f
                )
            }
            if (repeatFeedback.isNotBlank()) {
                ComicPanel(color = ComicYellow) {
                    Text(repeatFeedback, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.weight(if (compact) 1f else .35f))
            if (applicationDone) {
                GuidedComicButton(
                    "CONTAR AO GRUPO",
                    { interactionNonce++; mode = "group" },
                    color = ComicGreen,
                    leading = "🤝",
                    cue = "LEVE A DESCOBERTA ADIANTE"
                )
            } else {
                GuidedComicButton(
                    if (listening) "ESTOU OUVINDO..." else "DAR A PISTA COM A VOZ",
                    {
                        interactionNonce++
                        listen { heard ->
                            when (BallInstructionResolver.resolve(heard)) {
                                BallInstruction.COMPLETE -> {
                                    repeatFeedback = "Você ligou a bola à pista da árvore. Sua orientação ajuda Davi a agir!"
                                    applicationDone = true
                                    onApplication(ResponseModality.VOICE)
                                    playSound(SoundCue.DISCOVERY)
                                    speak("Sua orientação ajuda Davi: procure a bola atrás da árvore!")
                                }
                                BallInstruction.PARTIAL -> {
                                    repeatFeedback = "Você começou a orientação. Agora junte a bola e a árvore na mesma ideia."
                                    speak("Boa pista. Tente juntar bola e árvore na mesma orientação.")
                                }
                                BallInstruction.OTHER, BallInstruction.EMPTY -> {
                                    repeatFeedback = "Eu ouvi sua ideia. Observe a pista e diga onde Davi deve procurar a bola."
                                    speak("Vamos usar a pista: onde Davi deve procurar a bola?")
                                }
                            }
                        }
                    },
                    color = ComicBlue,
                    enabled = !listening,
                    leading = "🎤",
                    cue = "EXPLIQUE PARA AJUDAR DAVI"
                )
                if (showApplicationHelp) {
                    ComicButton(
                        "USAR A PISTA COM A LEIA",
                        {
                            interactionNonce++
                            repeatFeedback = "Você usou a pista para orientar Davi: procure a bola atrás da árvore."
                            applicationDone = true
                            onApplication(ResponseModality.TOUCH)
                            speak("Vamos dizer juntos: Davi, procure a bola atrás da árvore.")
                        },
                        color = Color.White,
                        leading = "🌳"
                    )
                } else {
                    ComicButton(
                        "PRECISO DE UMA PISTA",
                        {
                            interactionNonce++
                            showApplicationHelp = true
                            onHelp()
                            speak("Use duas ideias na sua orientação: bola e árvore.")
                        },
                        color = Color.White,
                        leading = "💡"
                    )
                }
            }
            if (!compact) Spacer(Modifier.weight(.65f))
        } else if (mode == "menu") {
            ComicPanel {
                Text("Escolha a figura", fontSize = 22.sp, fontWeight = FontWeight.Black)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PuzzleSubject.entries.forEach { option ->
                    ComicButton(
                        text = option.spokenWord.take(4).uppercase(),
                        onClick = {
                            subjectName = option.name
                            speak(option.spokenWord)
                        },
                        modifier = Modifier.weight(1f),
                        color = if (subject == option) ComicYellow else Color.White,
                        leading = option.icon
                    )
                }
                }
            }
            ComicPanel {
                Text("Escolha o tamanho", fontSize = 22.sp, fontWeight = FontWeight.Black)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ComicButton("2 × 2 • COMEÇAR", {
                    columns = 2
                    speak("Duas por duas. Quatro peças.")
                }, Modifier.weight(1f), color = if (columns == 2) ComicYellow else Color.White)
                ComicButton("3 × 2 • DESAFIO", {
                    columns = 3
                    speak("Três por duas. Seis peças.")
                }, Modifier.weight(1f), color = if (columns == 3) ComicYellow else Color.White)
                }
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
            val complete = PuzzleGame.isComplete(tiles)
            val showGuidance = rememberPuzzleGuidance(
                stageKey = "$round",
                interactionNonce = interactionNonce,
                busy = complete || listening || voiceBusy || showHint,
                speak = speak,
                onVoiceHint = onHelp
            )
            if (showHint) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(3.dp, ComicInk)
                ) {
                    Image(
                        painter = painterResource(subject.drawable),
                        contentDescription = "Figura completa de ${subject.spokenWord}",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                if (!complete) {
                    if (showGuidance) {
                        val guidance = if (selected < 0) "TOQUE OU ARRASTE UMA PEÇA" else "AGORA TOQUE EM OUTRA"
                        if (reducedStimuli) Pill("👇  $guidance", ComicYellow) else AttentionCue(guidance)
                    } else Text(
                        "Toque em duas peças ou arraste uma delas.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                PuzzleBoard(
                subject = subject,
                puzzleSize = puzzleSize,
                tiles = tiles,
                selected = selected,
                modifier = Modifier.weight(1f),
                onTileMove = move@ { first, second ->
                    if (complete || first == second) return@move
                    val updated = PuzzleGame.swap(tiles, first, second)
                    playSound(SoundCue.SWAP)
                    tiles = updated
                    selected = -1
                    moves++
                    interactionNonce++
                    if (PuzzleGame.isComplete(updated)) {
                        playSound(SoundCue.DISCOVERY)
                        val duration = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0)
                        speak("Você montou a bola! Bola. Bo-la. Bola começa com o som bê: b, b, bola.")
                        onCompleted(subject.spokenWord, puzzleSize.label, moves, duration)
                    }
                },
                onTileClick = click@ { position ->
                    if (complete) return@click
                    interactionNonce++
                    if (selected < 0) {
                        selected = position
                        speak("Peça escolhida. Agora toque em outra.")
                    } else if (selected == position) {
                        selected = -1
                        speak("Peça desmarcada.")
                    } else {
                        val updated = PuzzleGame.swap(tiles, selected, position)
                        playSound(SoundCue.SWAP)
                        tiles = updated
                        selected = -1
                        moves++
                        if (PuzzleGame.isComplete(updated)) {
                            playSound(SoundCue.DISCOVERY)
                            val duration = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0)
                            speak("Você montou a bola! Bola. Bo-la. Bola começa com o som bê: b, b, bola.")
                            onCompleted(subject.spokenWord, puzzleSize.label, moves, duration)
                        }
                    }
                }
                )
            }

            Text(
                if (complete) "Muito bem! Você montou a ${subject.spokenWord}."
                else "Movimentos: $moves",
                fontSize = 21.sp,
                fontWeight = FontWeight.Black
            )
            if (!complete) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ComicButton(
                if (showHint) "ESCONDER A FIGURA" else "VER A FIGURA",
                {
                    showHint = !showHint
                    interactionNonce++
                    if (showHint) {
                        onHelp()
                        speak("Esta é a figura de ${subject.spokenWord}. Observe e continue montando.")
                    }
                },
                modifier = Modifier.weight(1f),
                color = Color.White,
                leading = "👀"
            )
            ComicButton("OUVIR", {
                speak(subject.spokenWord)
            }, Modifier.weight(1f), color = ComicYellow, leading = "🔊")
            }
            if (!complete) ComicButton("MONTAR DE NOVO", {
                round++
                tiles = PuzzleGame.initialTiles(puzzleSize, round)
                selected = -1
                moves = 0
                showHint = false
                startedAt = SystemClock.elapsedRealtime()
                interactionNonce++
                speak("Vamos montar ${subject.spokenWord} de novo.")
            }, color = Color.White)
            if (complete) {
                ComicPanel(color = SoftGreen) {
                    Text("⚽  BOLA", fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Text("BO-LA  •  B de /b/", fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Text("Ouça, fale e perceba o começo da palavra.", Modifier.padding(top = 6.dp), fontSize = 16.sp)
                    if (repeatFeedback.isNotBlank()) Text(repeatFeedback, fontWeight = FontWeight.Bold)
                }
                if (guided) {
                    ComicButton(
                        "OUVIR B • BO-LA",
                        { speak("Bola. Bo-la. Bola começa com o som b: b, b, bola.") },
                        color = ComicYellow,
                        leading = "🔊"
                    )
                    GuidedComicButton(
                        "USAR NA HISTÓRIA",
                        {
                            interactionNonce++
                            repeatFeedback = ""
                            applicationDone = false
                            showApplicationHelp = false
                            mode = "apply"
                        },
                        color = ComicGreen,
                        leading = "💬",
                        cue = "AGORA DÊ UMA ORIENTAÇÃO"
                    )
                } else {
                    GuidedComicButton(
                        if (listening) "ESTOU OUVINDO..." else "FALAR ${subject.spokenWord.uppercase()}",
                        {
                            listen { heard ->
                                if (BallAnswerResolver.resolve(heard) == BallAnswer.BALL) {
                                    repeatFeedback = "Eu ouvi BOLA!"
                                    playSound(SoundCue.DISCOVERY)
                                    speak("Eu ouvi bola!")
                                } else {
                                    repeatFeedback = "Eu ouvi sua tentativa. Escute e tente outra vez: bola."
                                    speak("Eu ouvi sua tentativa. Escute comigo: bola.")
                                }
                            }
                        },
                        color = ComicBlue,
                        enabled = !listening,
                        leading = "🎤",
                        cue = "FALE EM VOZ ALTA"
                    )
                    ComicButton("ESCOLHER OUTRA FIGURA", { mode = "menu" }, color = ComicGreen, leading = "🧩")
                }
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
    onTileClick: (Int) -> Unit,
    onTileMove: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val image = ImageBitmap.imageResource(subject.drawable)
    ComicPanel(modifier = modifier, contentPadding = PaddingValues(6.dp)) {
        Column(Modifier.fillMaxSize()) {
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
                            onMove = { target -> onTileMove(position, target) },
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
    onMove: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val sourceWidth = image.width / columns
    val sourceHeight = image.height / rows
    val sourceColumn = sourceIndex % columns
    val sourceRow = sourceIndex / columns
    var tileSize by remember { mutableStateOf(IntSize.Zero) }
    var drag by remember { mutableStateOf(Offset.Zero) }
    Canvas(
        modifier
            .padding(2.dp)
            .border(if (selected) 5.dp else 2.dp, if (selected) ComicYellow else ComicInk)
            .semantics { contentDescription = "Peça na posição ${position + 1} de $total" }
            .onSizeChanged { tileSize = it }
            .graphicsLayer {
                translationX = drag.x
                translationY = drag.y
            }
            .pointerInput(position, columns, rows, tileSize) {
                detectDragGestures(
                    onDragStart = { drag = Offset.Zero },
                    onDragCancel = { drag = Offset.Zero },
                    onDragEnd = {
                        val columnDelta = if (tileSize.width == 0) 0 else (drag.x / tileSize.width).roundToInt()
                        val rowDelta = if (tileSize.height == 0) 0 else (drag.y / tileSize.height).roundToInt()
                        val originRow = position / columns
                        val originColumn = position % columns
                        val targetRow = (originRow + rowDelta).coerceIn(0, rows - 1)
                        val targetColumn = (originColumn + columnDelta).coerceIn(0, columns - 1)
                        val target = targetRow * columns + targetColumn
                        if (target != position && (abs(drag.x) > tileSize.width * .25f || abs(drag.y) > tileSize.height * .25f)) {
                            onMove(target)
                        }
                        drag = Offset.Zero
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        drag += amount
                    }
                )
            }
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
