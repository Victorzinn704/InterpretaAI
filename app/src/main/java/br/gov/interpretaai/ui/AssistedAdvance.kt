package br.gov.interpretaai.ui

import android.os.SystemClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.gov.interpretaai.R
import br.gov.interpretaai.domain.ActivityProgressPolicy
import br.gov.interpretaai.domain.AssistedAdvanceReason
import br.gov.interpretaai.ui.theme.ComicBlue
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicInk
import br.gov.interpretaai.ui.theme.ComicYellow
import kotlinx.coroutines.delay

/** Counts only foreground, unblocked participation time for the current stage. */
@Composable
fun rememberAssistedAdvanceReason(
    stageKey: Any,
    unsuccessfulAttempts: Int,
    hasCheckableAnswer: Boolean,
    busy: Boolean,
    timeLimitMs: Long = ActivityProgressPolicy.ACTIVE_TIME_LIMIT_MS
): AssistedAdvanceReason? {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var inForeground by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    var activeElapsedMs by rememberSaveable(stageKey) { mutableLongStateOf(0L) }
    var reason by rememberSaveable(stageKey) { mutableStateOf<AssistedAdvanceReason?>(null) }
    val latestReason by rememberUpdatedState(reason)

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            inForeground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(stageKey, unsuccessfulAttempts, hasCheckableAnswer, activeElapsedMs) {
        reason = reason ?: ActivityProgressPolicy.reason(
            activeElapsedMs,
            unsuccessfulAttempts,
            hasCheckableAnswer
        )
    }
    LaunchedEffect(stageKey, inForeground, busy, reason, timeLimitMs) {
        if (!inForeground || busy || reason != null) return@LaunchedEffect
        val startedAt = SystemClock.elapsedRealtime()
        val remaining = (timeLimitMs - activeElapsedMs).coerceAtLeast(0L)
        try {
            delay(remaining)
            activeElapsedMs = timeLimitMs
            reason = AssistedAdvanceReason.TIME_LIMIT
        } finally {
            if (latestReason == null && activeElapsedMs < timeLimitMs) {
                activeElapsedMs = (activeElapsedMs +
                    (SystemClock.elapsedRealtime() - startedAt)).coerceAtMost(timeLimitMs)
            }
        }
    }
    return reason
}

@Composable
fun AssistedAdvanceStage(
    reason: AssistedAdvanceReason,
    speak: (String) -> Unit,
    onContinue: () -> Unit
) {
    val message = when (reason) {
        AssistedAdvanceReason.TIME_LIMIT ->
            "Esta parte terminou. LÉIA e Alfa seguem com você. Vamos para a próxima?"
        AssistedAdvanceReason.ATTEMPT_LIMIT ->
            "Não foi dessa vez, e tudo bem. LÉIA e Alfa tentam de novo com você na próxima."
    }
    var continued by rememberSaveable(reason) { mutableStateOf(false) }
    fun continueOnce() {
        if (!continued) {
            continued = true
            onContinue()
        }
    }
    LaunchedEffect(reason) { speak(message) }
    LaunchedEffect(reason) {
        delay(8_000)
        continueOnce()
    }
    ChildStageScaffold(showCompanions = false) { compact ->
        Pill("LÉIA • VAMOS SEGUIR", ComicYellow)
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painterResource(R.drawable.leia_and_alfa_support_v2),
                contentDescription = "LÉIA e Alfa convidam a criança a continuar",
                modifier = Modifier.fillMaxWidth().heightIn(max = if (compact) 185.dp else 420.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                message,
                color = ComicInk,
                fontSize = if (compact) 17.sp else 21.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
        }
        GuidedComicButton(
            text = "VAMOS CONTINUAR",
            onClick = ::continueOnce,
            color = ComicGreen,
            trailing = "→",
            cue = "A PRÓXIMA PARTE ESTÁ PRONTA"
        )
        if (!compact) {
            ComicButton("OUVIR DE NOVO", { speak(message) }, color = ComicBlue, leading = "🔊")
        }
    }
}
