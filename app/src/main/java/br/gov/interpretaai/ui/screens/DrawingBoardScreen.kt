package br.gov.interpretaai.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.interpretaai.domain.DrawingHistory
import br.gov.interpretaai.domain.DrawingPoint
import br.gov.interpretaai.domain.DrawingPrompt
import br.gov.interpretaai.domain.DrawingStroke
import br.gov.interpretaai.ui.ChildStageScaffold
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.Pill
import br.gov.interpretaai.ui.StageHeader
import br.gov.interpretaai.ui.theme.ComicBlue
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicRed
import br.gov.interpretaai.ui.theme.ComicYellow

@Composable
fun DrawingBoardScreen(
    prompt: DrawingPrompt,
    speak: (String) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val history = remember { DrawingHistory() }
    val strokes = remember { mutableStateListOf<DrawingStroke>() }
    var active by remember { mutableStateOf<List<DrawingPoint>>(emptyList()) }
    var color by remember { mutableStateOf(ComicBlue) }
    var width by remember { mutableStateOf(12f) }

    fun refresh() { strokes.clear(); strokes.addAll(history.strokes) }
    LaunchedEffect(prompt) { speak("Vamos desenhar uma ${prompt.label}. Siga a pista ou crie do seu jeito.") }
    DisposableEffect(Unit) { onDispose { speak("") } }

    ChildStageScaffold { compact ->
        StageHeader("Meu quadro", "LEIA • APRENDER", onBack) {
            speak("Desenhe uma ${prompt.label}. Você pode arrastar o dedo e usar desfazer.")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Pill("${prompt.emoji} DESENHE: ${prompt.label.uppercase()}", ComicYellow)
            Text("arraste para desenhar", fontSize = if (compact) 13.sp else 16.sp)
        }
        Canvas(
            Modifier.weight(1f).fillMaxWidth()
                .background(Color.White, RoundedCornerShape(20.dp))
                .border(4.dp, Color.Black, RoundedCornerShape(20.dp))
                .pointerInput(color, width) {
                    detectDragGestures(
                        onDragStart = { active = listOf(DrawingPoint(it.x, it.y)) },
                        onDrag = { change, _ ->
                            change.consume()
                            active = active + DrawingPoint(change.position.x, change.position.y)
                        },
                        onDragEnd = {
                            history.add(DrawingStroke(active, color.value.toLong(), width))
                            active = emptyList(); refresh()
                        },
                        onDragCancel = { active = emptyList() }
                    )
                }
        ) {
            drawTemplate(prompt, size)
            (strokes + listOfNotNull(active.takeIf { it.isNotEmpty() }?.let {
                DrawingStroke(it, color.value.toLong(), width)
            })).forEach { stroke ->
                val points = stroke.points
                if (points.size == 1) drawCircle(Color(stroke.color.toULong()), stroke.width / 2, Offset(points[0].x, points[0].y))
                else {
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path, Color(stroke.color.toULong()), style = Stroke(stroke.width, cap = StrokeCap.Round))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(ComicBlue, ComicRed, ComicGreen, Color.Black).forEach { option ->
                IconButton(onClick = { color = option }, Modifier.weight(1f).background(option, RoundedCornerShape(12.dp))) {
                    Text(if (color == option) "✓" else "●", color = Color.White, fontSize = 22.sp)
                }
            }
            ComicButton("↶", { history.undo(); refresh() }, Modifier.weight(1f), color = Color.White, enabled = history.canUndo)
            ComicButton("↷", { history.redo(); refresh() }, Modifier.weight(1f), color = Color.White, enabled = history.canRedo)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ComicButton(if (width < 20f) "TRAÇO +" else "TRAÇO −", { width = if (width < 20f) 24f else 12f }, Modifier.weight(1f), color = Color.White)
            ComicButton("LIMPAR", { history.clear(); refresh() }, Modifier.weight(1f), color = ComicYellow)
            GuidedComicButton("TERMINEI", {
                speak("Que legal! Você criou uma ${prompt.label}. Agora conte para a turma como pensou no desenho.")
                onComplete()
            }, Modifier.weight(1.4f), color = ComicGreen, trailing = "✓")
        }
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
