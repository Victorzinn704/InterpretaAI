package br.gov.interpretaai.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.interpretaai.platform.SoundCue
import br.gov.interpretaai.ui.theme.ComicInk
import br.gov.interpretaai.ui.theme.ComicYellow
import kotlinx.coroutines.delay

/** Uma pista tocável dentro do próprio quadro, sem depender de seta ou texto na ilustração. */
@Composable
fun InteractiveComicPortrait(
    @DrawableRes drawableRes: Int,
    description: String,
    focusLabel: String,
    focusX: Float,
    focusY: Float,
    onFocusFound: () -> Unit,
    modifier: Modifier = Modifier,
    imageAspectRatio: Float = 4f / 3f,
    reducedStimuli: Boolean = false,
    tag: String
) {
    var discovered by remember(drawableRes, tag) { mutableStateOf(false) }
    var hintVisible by remember(drawableRes, tag) { mutableStateOf(false) }
    val playSound = LocalSoundEffect.current
    val pulse = rememberInfiniteTransition(label = "comic-focus")
    val hintScale by pulse.animateFloat(
        initialValue = .94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
        label = "comic-focus-scale"
    )

    LaunchedEffect(drawableRes, tag, discovered, reducedStimuli) {
        hintVisible = false
        if (!discovered && !reducedStimuli) {
            delay(20_000)
            hintVisible = true
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(3.dp, ComicInk)
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(imageAspectRatio)) {
            Image(
                painter = painterResource(drawableRes),
                contentDescription = description,
                modifier = Modifier.matchParentSize().clip(RoundedCornerShape(17.dp)),
                contentScale = ContentScale.Crop
            )

            val targetSize = if (maxHeight < 240.dp) 56.dp else 72.dp
            val x = (maxWidth * focusX - targetSize / 2)
                .coerceIn(0.dp, (maxWidth - targetSize).coerceAtLeast(0.dp))
            val y = (maxHeight * focusY - targetSize / 2)
                .coerceIn(0.dp, (maxHeight - targetSize).coerceAtLeast(0.dp))
            val visible = discovered || hintVisible

            Box(
                Modifier
                    .offset(x, y)
                    .size(targetSize)
                    .graphicsLayer {
                        val scale = if (hintVisible && !discovered && !reducedStimuli) hintScale else 1f
                        scaleX = scale
                        scaleY = scale
                    }
                    .semantics {
                        contentDescription = focusLabel
                        role = Role.Button
                    }
                    .testTag(tag)
                    .clip(CircleShape)
                    .clickable {
                        discovered = true
                        hintVisible = false
                        playSound(SoundCue.DISCOVERY)
                        onFocusFound()
                    }
                    .border(if (visible) 4.dp else 0.dp, ComicYellow, CircleShape)
                    .background(
                        if (discovered) Color.White.copy(alpha = .30f)
                        else Color.Transparent
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (visible) Text(if (discovered) "✨" else "👀", fontSize = 22.sp)
            }
        }
    }
}
