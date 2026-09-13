package br.gov.interpretaai.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.interpretaai.ui.theme.ComicBlue
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicInk
import br.gov.interpretaai.ui.theme.ComicYellow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.runtime.staticCompositionLocalOf
import br.gov.interpretaai.platform.SoundCue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

val LocalSoundEffect = staticCompositionLocalOf<(SoundCue) -> Unit> { {} }

@Composable
fun ChildStageScaffold(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(compact: Boolean) -> Unit
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val compact = maxHeight < 720.dp
        Column(
            Modifier.fillMaxSize().padding(if (compact) 10.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp)
        ) { content(compact) }
    }
}

@Composable
fun rememberReengagementVisual(
    stageKey: String,
    interactionNonce: Int,
    busy: Boolean,
    reducedStimuli: Boolean,
    speak: (String) -> Unit
): Boolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    var foreground by remember { mutableStateOf(true) }
    var visual by remember(stageKey) { mutableStateOf(false) }
    var spoken by remember(stageKey) { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            foreground = event == Lifecycle.Event.ON_RESUME ||
                (foreground && event !in listOf(Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP))
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(stageKey, interactionNonce, busy, foreground) {
        visual = false
        if (busy || !foreground) return@LaunchedEffect
        delay(20_000)
        visual = true
        if (!spoken) {
            delay(20_000)
            speak("Ei, detetive! A história está esperando a sua ideia. Vamos juntos?")
            spoken = true
        }
    }
    return visual && !reducedStimuli
}

@Composable
fun rememberPuzzleGuidance(
    stageKey: String,
    interactionNonce: Int,
    busy: Boolean,
    speak: (String) -> Unit,
    onVoiceHint: () -> Unit
): Boolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    var foreground by remember { mutableStateOf(true) }
    var visual by remember(stageKey) { mutableStateOf(false) }
    var spoken by remember(stageKey) { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            foreground = event == Lifecycle.Event.ON_RESUME ||
                (foreground && event !in listOf(Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP))
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(stageKey, interactionNonce, busy, foreground) {
        visual = false
        if (busy || !foreground) return@LaunchedEffect
        if (!spoken) {
            delay(br.gov.interpretaai.domain.PuzzleGuidancePolicy.VOICE_AFTER_MS)
            speak("Vamos ajudar Lia? Toque em uma peça e depois em outra, ou arraste uma peça.")
            onVoiceHint()
            spoken = true
            delay(
                br.gov.interpretaai.domain.PuzzleGuidancePolicy.VISUAL_AFTER_MS -
                    br.gov.interpretaai.domain.PuzzleGuidancePolicy.VOICE_AFTER_MS
            )
        } else {
            delay(br.gov.interpretaai.domain.PuzzleGuidancePolicy.VISUAL_AFTER_MS)
        }
        visual = true
    }
    return visual
}

@Composable
fun ComicPanel(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier.padding(end = 6.dp, bottom = 6.dp)) {
        Box(
            Modifier.matchParentSize()
                .offset(6.dp, 6.dp)
                .background(ComicInk, RoundedCornerShape(24.dp))
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = color,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(3.dp, ComicInk)
        ) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

@Composable
fun ComicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    leading: String = "",
    trailing: String = ""
) {
    val playSound = LocalSoundEffect.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) .96f else 1f, tween(100), label = "button-press")
    Box(modifier.padding(end = 4.dp, bottom = 4.dp).graphicsLayer { scaleX = pressScale; scaleY = pressScale }) {
        Box(
            Modifier.matchParentSize()
                .offset(4.dp, 4.dp)
                .background(ComicInk, RoundedCornerShape(18.dp))
        )
        Button(
            onClick = { playSound(SoundCue.TAP); onClick() },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            interactionSource = interactionSource,
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color),
            border = BorderStroke(3.dp, ComicInk),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 15.dp)
        ) {
            Text(
                text = listOf(leading, text, trailing).filter { it.isNotBlank() }.joinToString(" "),
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = if (color == Color.White || color.luminance() > .6f) ComicInk else Color.White
            )
        }
    }
}

@Composable
fun GuidedComicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = ComicBlue,
    enabled: Boolean = true,
    leading: String = "",
    trailing: String = "→",
    cue: String = "TOQUE AQUI"
) {
    val transition = rememberInfiniteTransition(label = "guided-action")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "guided-scale"
    )
    val animatedColor by transition.animateColor(
        initialValue = color,
        targetValue = when (color) {
            ComicBlue -> Color(0xFF0EA5E9)
            ComicGreen -> Color(0xFF22C55E)
            ComicYellow -> Color(0xFFFFE36A)
            else -> color
        },
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "guided-color"
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (enabled) {
            Text("👇  $cue", fontWeight = FontWeight.Black, fontSize = 14.sp, color = ComicInk)
        }
        ComicButton(
            text = text,
            onClick = onClick,
            modifier = Modifier.graphicsLayer {
                scaleX = if (enabled) scale else 1f
                scaleY = if (enabled) scale else 1f
            },
            color = if (enabled) animatedColor else color,
            enabled = enabled,
            leading = leading,
            trailing = trailing
        )
    }
}

@Composable
fun AttentionCue(text: String, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "attention-cue")
    val offset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
        label = "attention-cue-offset"
    )
    Pill(
        text = "👇  $text",
        color = ComicYellow,
        modifier = modifier.graphicsLayer { translationY = offset }
    )
}

@Composable
fun GuidedScrollScreen(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    contentPadding: PaddingValues = PaddingValues(18.dp),
    verticalSpacing: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    Box(modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize()
                .verticalScroll(scrollState)
                .padding(contentPadding)
                .padding(bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing),
            content = content
        )
        if (scrollState.canScrollForward) {
            val transition = rememberInfiniteTransition(label = "scroll-cue")
            val offset by transition.animateFloat(
                initialValue = 0f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
                label = "scroll-cue-offset"
            )
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 10.dp)
                    .graphicsLayer { translationY = offset }
                    .clickable {
                        scope.launch {
                            scrollState.animateScrollTo(
                                (scrollState.value + 520).coerceAtMost(scrollState.maxValue)
                            )
                        }
                    },
                color = ComicYellow,
                shape = RoundedCornerShape(100),
                border = BorderStroke(3.dp, ComicInk)
            ) {
                Text(
                    "👇  VER MAIS",
                    Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

private fun Color.luminance(): Float =
    (.299f * red + .587f * green + .114f * blue)

@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color,
        shape = RoundedCornerShape(100),
        border = BorderStroke(2.dp, ComicInk)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun SpeechBubble(speaker: String, text: String, color: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = color,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, ComicInk)
    ) {
        Row(Modifier.padding(12.dp)) {
            Text(speaker.uppercase(), fontWeight = FontWeight.Black, fontSize = 13.sp)
            Spacer(Modifier.width(10.dp))
            Text(text, fontSize = 18.sp, lineHeight = 25.sp)
        }
    }
}

@Composable
fun StageHeader(title: String, stage: String, onBack: () -> Unit, onSpeak: () -> Unit) {
    ComicPanel(color = br.gov.interpretaai.ui.theme.ComicYellow, contentPadding = PaddingValues(12.dp)) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, ComicInk),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                contentPadding = PaddingValues(10.dp)
            ) { Text("←", color = ComicInk, fontSize = 24.sp, fontWeight = FontWeight.Black) }
            androidx.compose.foundation.layout.Column(
                Modifier.weight(1f).padding(horizontal = 10.dp)
            ) {
                Text(stage.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black)
                Text(title, fontSize = 21.sp, fontWeight = FontWeight.Black)
            }
            Button(
                onClick = onSpeak,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, ComicInk),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                contentPadding = PaddingValues(10.dp)
            ) { Text("🔊", fontSize = 20.sp) }
        }
    }
}
