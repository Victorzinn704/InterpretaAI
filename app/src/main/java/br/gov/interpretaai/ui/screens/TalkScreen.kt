package br.gov.interpretaai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.ComicPanel
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.StageHeader
import br.gov.interpretaai.ui.CollaborativeTurnCue
import br.gov.interpretaai.domain.AssignedLearner
import br.gov.interpretaai.domain.CollaborativeMoment
import br.gov.interpretaai.domain.AssistedAdvanceReason
import br.gov.interpretaai.ui.AssistedAdvanceStage
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicYellow
import br.gov.interpretaai.ui.theme.SoftBlue
import kotlinx.coroutines.delay

@Composable
fun TalkScreen(
    onBack: () -> Unit,
    onSpeak: () -> Unit,
    onComplete: () -> Unit,
    speak: (String) -> Unit = {},
    onAssistedAdvance: (AssistedAdvanceReason) -> Unit = {},
    learners: List<AssignedLearner> = emptyList()
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var seconds by remember { mutableIntStateOf(180) }
    var running by remember { mutableStateOf(true) }
    var foreground by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(running, seconds, foreground) {
        if (running && foreground && seconds > 0) {
            delay(1_000)
            seconds--
        }
    }
    LaunchedEffect(Unit) { onSpeak() }
    if (seconds == 0) {
        AssistedAdvanceStage(AssistedAdvanceReason.TIME_LIMIT, speak) {
            onAssistedAdvance(AssistedAdvanceReason.TIME_LIMIT)
            onComplete()
        }
        return
    }
    Column(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        StageHeader("Hora do papo • Gibi", "Bem-estar digital", onBack, onSpeak)
        CollaborativeTurnCue(learners, CollaborativeMoment.SHARE)
        ComicPanel(color = ComicGreen) {
            Text("🌱 USO CONSCIENTE", fontWeight = FontWeight.Black)
            Text(
                "O celular já ajudou. Agora ele descansa na mesa!",
                modifier = Modifier.padding(top = 12.dp),
                fontWeight = FontWeight.Black,
                fontSize = 21.sp,
                lineHeight = 29.sp
            )
            Text("A história continua na vida real entre vocês.", fontWeight = FontWeight.Bold)
        }
        ComicPanel(color = SoftBlue) {
            Text("🤝 APRENDER COM A DUPLA", fontWeight = FontWeight.Black)
            Text("Deite o celular na mesa e conte para sua dupla qual palavra com M você descobriu!", fontSize = 19.sp, lineHeight = 27.sp)
        }
        ComicPanel(modifier = Modifier.fillMaxWidth(), color = ComicYellow) {
            Text("⏱️ TEMPO SEM DISTRAÇÕES", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(18.dp))
            Text(
                "%02d:%02d".format(seconds / 60, seconds % 60),
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 58.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.height(18.dp))
            ComicButton(if (running) "PAUSAR" else "CONTINUAR", { running = !running }, color = ComicGreen)
        }
        Spacer(Modifier.weight(1f))
        GuidedComicButton(
            "ENCERRAR MISSÃO",
            onComplete,
            color = ComicGreen,
            trailing = "🏆",
            cue = "QUANDO TERMINAREM"
        )
    }
}
