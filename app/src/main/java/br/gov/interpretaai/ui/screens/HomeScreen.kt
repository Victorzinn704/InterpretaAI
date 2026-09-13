package br.gov.interpretaai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import br.gov.interpretaai.ui.theme.ComicBlue
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicRed
import br.gov.interpretaai.ui.theme.ComicYellow

@Composable
fun HomeScreen(
    onSchool: () -> Unit,
    onEducator: () -> Unit,
    onSpeak: () -> Unit,
    onFocus: () -> Unit
) {
    ChildStageScaffold { compact ->
        ComicPanel(color = ComicRed) {
            Text("INTERPRETA AI", color = Color.White, fontSize = if (compact) 25.sp else 30.sp, fontWeight = FontWeight.Black)
            Text("LEIA • Ler, Entender, Interpretar e Aprender", color = Color.White, fontSize = 16.sp)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("👋", fontSize = if (compact) 62.sp else 82.sp)
            Pill("SUA AJUDANTE LEIA", ComicYellow)
            Spacer(Modifier.height(if (compact) 8.dp else 16.dp))
            Text(
                "Ajude a LEIA a descobrir histórias, sons e palavras.",
                fontSize = if (compact) 19.sp else 23.sp,
                lineHeight = if (compact) 24.sp else 29.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Com foco na tela. Com a turma fora dela.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = ComicGreen
            )
            Spacer(Modifier.height(if (compact) 10.dp else 20.dp))
            GuidedComicButton(
                "COMEÇAR COM A LEIA", onSchool, Modifier.fillMaxWidth(),
                color = ComicBlue, leading = "🎒", cue = "TOQUE PARA COMEÇAR"
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ComicButton("OUVIR", onSpeak, Modifier.weight(1f), color = ComicYellow, leading = "🔊")
            ComicButton("FOCO", onFocus, Modifier.weight(1f), color = ComicGreen, leading = "🔒")
            ComicButton("PROF.", onEducator, Modifier.weight(1f), color = Color.White, leading = "📊")
        }
    }
}
