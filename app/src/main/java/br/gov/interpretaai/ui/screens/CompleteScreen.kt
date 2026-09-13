package br.gov.interpretaai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.interpretaai.ui.ChildStageScaffold
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.ComicPanel
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.Pill
import br.gov.interpretaai.ui.LocalSoundEffect
import br.gov.interpretaai.platform.SoundCue
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicYellow
import br.gov.interpretaai.ui.theme.SoftBlue

@Composable
fun CompleteScreen(onSpeak: () -> Unit, onHome: () -> Unit) {
    val playSound = LocalSoundEffect.current
    LaunchedEffect(Unit) { playSound(SoundCue.CELEBRATE) }
    ChildStageScaffold { compact ->
        Pill("MISSÃO CONCLUÍDA", Color.White)
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🌟🏅🌟", fontSize = if (compact) 54.sp else 72.sp)
            Text(
                "VOCÊ AJUDOU A LEIA!",
                Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
                fontSize = if (compact) 25.sp else 31.sp,
                fontWeight = FontWeight.Black
            )
            Text("Você ouviu, falou, pensou e aplicou.", fontSize = 18.sp, textAlign = TextAlign.Center)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("👂 OUVIR", "💬 FALAR", "💡 PENSAR").forEach { achievement ->
                ComicPanel(modifier = Modifier.weight(1f)) {
                    Text(achievement, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
        }
        ComicPanel(color = SoftBlue) {
            Text("Conte ao colega qual ideia ajudou a história.", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        ComicButton("OUVIR PARABÉNS", onSpeak, color = ComicGreen, leading = "🔊")
        GuidedComicButton("VOLTAR ÀS HISTÓRIAS", onHome, color = ComicYellow, leading = "📖", cue = "CONTINUE COM A LEIA")
    }
}
