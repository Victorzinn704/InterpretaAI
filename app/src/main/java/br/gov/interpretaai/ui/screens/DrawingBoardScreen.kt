package br.gov.interpretaai.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.interpretaai.domain.DrawingHistory
import br.gov.interpretaai.domain.DrawingPoint
import br.gov.interpretaai.domain.DrawingPrompt
import br.gov.interpretaai.domain.DrawingStroke
import br.gov.interpretaai.domain.DrawingTool
import br.gov.interpretaai.domain.AssignedLearner
import br.gov.interpretaai.domain.CollaborativeMoment
import br.gov.interpretaai.domain.CollaborativeTurnPlanner
import br.gov.interpretaai.ui.ChildStageScaffold
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.CollaborativeTurnCue
import br.gov.interpretaai.ui.Pill
import br.gov.interpretaai.ui.StageHeader
import br.gov.interpretaai.ui.theme.ComicBlue
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicRed
import br.gov.interpretaai.ui.theme.ComicYellow

@Composable
fun DrawingBoardScreen(
    prompt: DrawingPrompt,
    learners: List<AssignedLearner> = emptyList(),
    speak: (String) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val history = remember { DrawingHistory() }
    val strokes = remember { mutableStateListOf<DrawingStroke>() }
    val active = remember { mutableStateListOf<DrawingPoint>() }
    var color by remember { mutableStateOf(ComicBlue) }
    var width by remember { mutableStateOf(12f) }
    var tool by remember { mutableStateOf(DrawingTool.BRUSH) }
    var historyRevision by remember { mutableIntStateOf(0) }
    val collaborativeTurn = CollaborativeTurnPlanner.turn(learners, CollaborativeMoment.CREATE)

    fun refresh() {
        strokes.clear()
        strokes.addAll(history.strokes)
        historyRevision++
    }
    val historyControls = remember(historyRevision) { history.canUndo to history.canRedo }
    LaunchedEffect(prompt, learners) {
        val base = "Vamos desenhar uma ${prompt.label}. Siga a pista ou crie do seu jeito."
        speak(collaborativeTurn?.let { "$base ${it.spokenPrompt}" } ?: base)
    }
    DisposableEffect(Unit) { onDispose { speak("") } }

    ChildStageScaffold { compact ->
        StageHeader("Meu quadro", "LEIA • APRENDER", onBack) {
            speak("Desenhe uma ${prompt.label}. Você pode arrastar o dedo e usar desfazer.")
        }
        CollaborativeTurnCue(learners, CollaborativeMoment.CREATE)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Pill("${prompt.emoji} DESENHE: ${prompt.label.uppercase()}", ComicYellow)
            Text("arraste para desenhar", fontSize = if (compact) 13.sp else 16.sp)
        }
        Canvas(
            Modifier.weight(1f).fillMaxWidth()
                .background(Color.White, RoundedCornerShape(20.dp))
                .border(4.dp, Color.Black, RoundedCornerShape(20.dp))
                .testTag("drawing-canvas")
                .semantics { contentDescription = "Área de desenho" }
                .pointerInput(color, width, tool) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        active.clear()
                        active.add(DrawingPoint(down.position.x, down.position.y))
                        down.consume()
                        var pressed = true
                        while (pressed) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            val last = active.last()
                            val dx = change.position.x - last.x
                            val dy = change.position.y - last.y
                            if (dx * dx + dy * dy >= 1f) {
                                active.add(DrawingPoint(change.position.x, change.position.y))
                            }
                            pressed = change.pressed
                            change.consume()
                        }
                        history.add(DrawingStroke(
                            active.toList(),
                            color.value.toLong(),
                            if (tool == DrawingTool.ERASER) 40f else width,
                            tool
                        ))
                        active.clear()
                        refresh()
                    }
                }
        ) {
            drawTemplate(prompt, size)
            drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())
            (strokes + listOfNotNull(active.takeIf { it.isNotEmpty() }?.let {
                DrawingStroke(
                    it.toList(),
                    color.value.toLong(),
                    if (tool == DrawingTool.ERASER) 40f else width,
                    tool
                )
            })).forEach { stroke ->
                val points = stroke.points
                val blendMode = if (stroke.tool == DrawingTool.ERASER) BlendMode.Clear else BlendMode.SrcOver
                if (points.size == 1) drawCircle(
                    Color(stroke.color.toULong()), stroke.width / 2,
                    Offset(points[0].x, points[0].y), blendMode = blendMode
                )
                else {
                    val path = smoothPath(points)
                    drawPath(
                        path,
                        Color(stroke.color.toULong()),
                        style = Stroke(stroke.width, cap = StrokeCap.Round),
                        blendMode = blendMode
                    )
                }
            }
            drawContext.canvas.restore()
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                ComicBlue to "azul", ComicRed to "vermelha",
                ComicGreen to "verde", Color.Black to "preta"
            ).forEach { (option, name) ->
                IconButton(
                    onClick = { color = option; tool = DrawingTool.BRUSH },
                    Modifier.weight(1f)
                        .background(option, RoundedCornerShape(12.dp))
                        .semantics {
                            contentDescription = "Cor $name"
                            selected = color == option && tool == DrawingTool.BRUSH
                        }
                ) {
                    Text(if (color == option && tool == DrawingTool.BRUSH) "✓" else "●", color = Color.White, fontSize = 22.sp)
                }
            }
            ComicButton(
                "↶", { history.undo(); refresh() },
                Modifier.weight(1f).testTag("drawing-undo").semantics { contentDescription = "Desfazer" },
                color = Color.White, enabled = historyControls.first
            )
            ComicButton(
                "↷", { history.redo(); refresh() },
                Modifier.weight(1f).testTag("drawing-redo").semantics { contentDescription = "Refazer" },
                color = Color.White, enabled = historyControls.second
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ComicButton(if (width < 20f) "✏️＋" else "✏️−", {
                width = if (width < 20f) 24f else 12f
                tool = DrawingTool.BRUSH
                speak(if (width >= 20f) "Traço grosso" else "Traço fino")
            }, Modifier.weight(1f).testTag("drawing-width"), color = Color.White)
            ComicButton(if (compact) "APAGAR" else "BORRACHA", {
                tool = if (tool == DrawingTool.ERASER) DrawingTool.BRUSH else DrawingTool.ERASER
                speak(if (tool == DrawingTool.ERASER) "Borracha ligada. Arraste para apagar." else "Lápis ligado.")
            }, Modifier.weight(1.1f), color = if (tool == DrawingTool.ERASER) ComicYellow else Color.White,
                tag = "drawing-eraser")
            ComicButton("LIMPAR", {
                history.clear(); refresh(); speak("Quadro limpo")
            }, Modifier.weight(1f).testTag("drawing-clear"), color = Color.White, enabled = historyControls.first)
            GuidedComicButton("TERMINEI", {
                speak("Que legal! Você criou uma ${prompt.label}. Agora conte para a turma como pensou no desenho.")
                onComplete()
            }, Modifier.weight(1.35f), color = ComicGreen, trailing = "✓")
        }
    }
}

private fun smoothPath(points: List<DrawingPoint>): Path = Path().apply {
    moveTo(points.first().x, points.first().y)
    if (points.size == 2) {
        lineTo(points.last().x, points.last().y)
    } else {
        for (index in 1 until points.size) {
            val previous = points[index - 1]
            val current = points[index]
            quadraticTo(
                previous.x,
                previous.y,
                (previous.x + current.x) / 2f,
                (previous.y + current.y) / 2f
            )
        }
        lineTo(points.last().x, points.last().y)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTemplate(prompt: DrawingPrompt, size: Size) {
    val hint = Color(0x553F51B5)
    val stroke = Stroke(width = 7f)
    val cx = size.width / 2f
    val cy = size.height / 2f
    when (prompt) {
        DrawingPrompt.BALL -> drawCircle(hint, size.minDimension * .25f, Offset(cx, cy), style = stroke)
        DrawingPrompt.APPLE -> {
            drawCircle(hint, size.minDimension * .23f, Offset(cx, cy + 15f), style = stroke)
            drawLine(hint, Offset(cx, cy - size.minDimension * .22f), Offset(cx + 18f, cy - size.minDimension * .34f), 7f)
        }
        DrawingPrompt.HOUSE -> {
            drawRect(hint, Offset(size.width * .28f, size.height * .42f), Size(size.width * .44f, size.height * .38f), style = stroke)
            drawLine(hint, Offset(size.width * .24f, size.height * .44f), Offset(cx, size.height * .2f), 7f)
            drawLine(hint, Offset(cx, size.height * .2f), Offset(size.width * .76f, size.height * .44f), 7f)
        }
        DrawingPrompt.TREE -> {
            drawRect(hint, Offset(cx - 28f, size.height * .52f), Size(56f, size.height * .3f), style = stroke)
            drawCircle(hint, size.minDimension * .22f, Offset(cx, size.height * .38f), style = stroke)
        }
    }
}
