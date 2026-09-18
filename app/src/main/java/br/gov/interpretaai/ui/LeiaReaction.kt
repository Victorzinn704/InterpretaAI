package br.gov.interpretaai.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.interpretaai.R
import br.gov.interpretaai.ui.theme.ComicBlue
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicInk
import br.gov.interpretaai.ui.theme.ComicYellow
import br.gov.interpretaai.ui.theme.SoftGreen
import br.gov.interpretaai.ui.theme.SoftBlue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class LeiaReactionTone { ENCOURAGE, DISCOVERY, CELEBRATE }

/**
 * Presença discreta e constante de LÉIA e Alfa nas etapas infantis.
 * A aba fica na borda para acompanhar a jornada sem competir com a atividade.
 */
@Composable
fun LeiaCompanionTab(
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(if (compact) 68.dp else 92.dp)
            .height(if (compact) 92.dp else 126.dp)
            .testTag("leia-alfa-companion"),
        color = ComicYellow,
        shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp),
        border = androidx.compose.foundation.BorderStroke(3.dp, ComicInk),
        shadowElevation = 5.dp
    ) {
        Image(
            painter = painterResource(R.drawable.leia_and_alfa_v1),
            contentDescription = "LÉIA e Alfa acompanham esta etapa",
            modifier = Modifier.fillMaxSize().padding(start = 3.dp, top = 4.dp, end = 3.dp, bottom = 2.dp),
            contentScale = ContentScale.Fit,
            alignment = Alignment.BottomCenter
        )
    }
}

/**
 * Uma pequena cena de reação, não uma chuva de confetes permanente.
 * A animação acontece uma vez e os elementos decorativos somem no modo de estímulos reduzidos.
 */
@Composable
fun LeiaReactionScene(
    message: String,
    modifier: Modifier = Modifier,
    label: String = "LÉIA • DESCOBRIU COM VOCÊ",
    headline: String? = null,
    tone: LeiaReactionTone = LeiaReactionTone.DISCOVERY,
    reducedStimuli: Boolean = false
) {
    val entrance = remember(message, tone) { Animatable(if (reducedStimuli) 1f else 0f) }
    LaunchedEffect(message, tone, reducedStimuli) {
        if (reducedStimuli) entrance.snapTo(1f)
        else {
            entrance.snapTo(0f)
            entrance.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
        }
    }
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .background(SoftBlue.copy(alpha = .32f), RoundedCornerShape(26.dp))
            .border(3.dp, ComicInk, RoundedCornerShape(26.dp))
            .padding(8.dp)
            .testTag("leia-reaction-scene")
    ) {
        val compact = maxWidth < 500.dp
        if (!reducedStimuli) {
            ReactionBurst(
                tone = tone,
                progress = entrance.value,
                modifier = Modifier.fillMaxSize().testTag("leia-reaction-motion")
            )
        }
        Image(
            painter = painterResource(
                if (tone == LeiaReactionTone.CELEBRATE) R.drawable.leia_and_alfa_celebrate_v2
                else R.drawable.leia_and_alfa_v1
            ),
            contentDescription = "LÉIA e Alfa reagem à descoberta",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(if (compact) .44f else .38f)
                .fillMaxHeight(.82f)
                .graphicsLayer {
                    val scale = if (reducedStimuli) 1f else .88f + entrance.value * .12f
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                },
            contentScale = ContentScale.Fit
        )
        ComicPanel(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxWidth(.64f),
            color = SoftGreen,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = if (compact) 12.dp else 20.dp,
                vertical = if (compact) 10.dp else 14.dp
            )
        ) {
            Text(label, fontSize = if (compact) 12.sp else 13.sp, fontWeight = FontWeight.Black, color = ComicGreen)
            headline?.let {
                Text(
                    it,
                    modifier = Modifier.padding(top = 4.dp),
                    fontSize = if (compact) 18.sp else 24.sp,
                    lineHeight = if (compact) 20.sp else 28.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                message,
                modifier = Modifier.padding(top = if (headline == null) 5.dp else 3.dp),
                fontSize = if (compact) 16.sp else 21.sp,
                lineHeight = if (compact) 20.sp else 27.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LeiaReactionBanner(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    tone: LeiaReactionTone = LeiaReactionTone.DISCOVERY,
    reducedStimuli: Boolean = false
) {
    Box(modifier.fillMaxWidth().height(104.dp).testTag("leia-reaction-banner")) {
        if (!reducedStimuli) {
            ReactionBurst(tone, 1f, Modifier.fillMaxSize().testTag("leia-reaction-motion"))
        }
        Image(
            painter = painterResource(
                if (tone == LeiaReactionTone.CELEBRATE) R.drawable.leia_and_alfa_celebrate_v2
                else R.drawable.leia_and_alfa_v1
            ),
            contentDescription = "LÉIA comemora a descoberta",
            modifier = Modifier.align(Alignment.BottomStart).width(92.dp).fillMaxHeight(),
            contentScale = ContentScale.Fit
        )
        ComicPanel(
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxWidth(.78f),
            color = SoftGreen,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(title, fontSize = 16.sp, lineHeight = 18.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(message, fontSize = 16.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun LeiaWelcomePortrait(
    height: Dp,
    reducedStimuli: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxWidth().height(height).testTag("leia-welcome-portrait")) {
        if (!reducedStimuli) {
            ReactionBurst(LeiaReactionTone.ENCOURAGE, 1f, Modifier.fillMaxSize().clearAndSetSemantics { })
        }
        Image(
            painter = painterResource(R.drawable.leia_and_alfa_v1),
            contentDescription = "LÉIA, uma educadora sorridente, ao lado de Alfa",
            modifier = Modifier.align(Alignment.Center).fillMaxHeight(),
            contentScale = ContentScale.Fit
        )
        LearningToken("A", ComicYellow, Modifier.align(Alignment.TopStart).offset(x = 18.dp, y = 8.dp))
        LearningToken("?", ComicBlue, Modifier.align(Alignment.TopEnd).offset(x = (-18).dp, y = 20.dp))
        LearningToken("♪", ComicGreen, Modifier.align(Alignment.BottomEnd).offset(x = (-28).dp, y = (-8).dp))
    }
}

@Composable
private fun LearningToken(text: String, color: Color, modifier: Modifier) {
    Surface(modifier = modifier.size(42.dp).clearAndSetSemantics { }, color = color, shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(2.dp, ComicInk), shadowElevation = 3.dp) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, color = if (color == ComicBlue || color == ComicGreen) Color.White else ComicInk,
                fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ReactionBurst(tone: LeiaReactionTone, progress: Float, modifier: Modifier = Modifier) {
    val primary = when (tone) {
        LeiaReactionTone.ENCOURAGE -> ComicBlue
        LeiaReactionTone.DISCOVERY -> ComicYellow
        LeiaReactionTone.CELEBRATE -> ComicGreen
    }
    val secondary = when (tone) {
        LeiaReactionTone.ENCOURAGE -> ComicYellow
        LeiaReactionTone.DISCOVERY -> ComicBlue
        LeiaReactionTone.CELEBRATE -> ComicYellow
    }
    Canvas(modifier.clearAndSetSemantics { }.alpha(progress)) {
        val origin = androidx.compose.ui.geometry.Offset(size.width * .25f, size.height * .54f)
        val radius = size.minDimension * (.18f + .12f * progress)
        repeat(8) { index ->
            val angle = (index * PI / 4).toFloat()
            val inner = radius * .72f
            val outer = radius
            drawLine(
                color = if (index % 2 == 0) primary else secondary,
                start = androidx.compose.ui.geometry.Offset(origin.x + cos(angle) * inner, origin.y + sin(angle) * inner),
                end = androidx.compose.ui.geometry.Offset(origin.x + cos(angle) * outer, origin.y + sin(angle) * outer),
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        drawStar(primary, androidx.compose.ui.geometry.Offset(size.width * .08f, size.height * .2f), 13.dp.toPx() * progress)
        drawStar(secondary, androidx.compose.ui.geometry.Offset(size.width * .47f, size.height * .12f), 10.dp.toPx() * progress)
        drawStar(primary, androidx.compose.ui.geometry.Offset(size.width * .53f, size.height * .82f), 8.dp.toPx() * progress)
        drawCircle(secondary, 5.dp.toPx() * progress, androidx.compose.ui.geometry.Offset(size.width * .12f, size.height * .78f))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar(
    color: Color,
    center: androidx.compose.ui.geometry.Offset,
    radius: Float
) {
    if (radius <= 0f) return
    val path = Path()
    repeat(10) { index ->
        val angle = (-PI / 2 + index * PI / 5).toFloat()
        val pointRadius = if (index % 2 == 0) radius else radius * .43f
        val x = center.x + cos(angle) * pointRadius
        val y = center.y + sin(angle) * pointRadius
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}
