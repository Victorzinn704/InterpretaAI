package br.gov.interpretaai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.interpretaai.AppUiState
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.ComicPanel
import br.gov.interpretaai.ui.ChildStageScaffold
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.Pill
import br.gov.interpretaai.ui.StageHeader
import br.gov.interpretaai.ui.theme.ComicBlue
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicYellow
import br.gov.interpretaai.ui.theme.ComicRed
import br.gov.interpretaai.ui.theme.SoftBlue
import br.gov.interpretaai.ui.theme.SoftGreen

@Composable
fun MissionScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onSpeak: () -> Unit,
    onListen: () -> Unit,
    onContinue: () -> Unit,
    onHelp: () -> Unit
) {
    ChildStageScaffold { compact ->
        StageHeader("Turminha Interpreta", "Etapa 1 de 4", onBack, onSpeak)
        if (!compact) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Pill("🔒 MODO ESCOLA • FOCO", ComicYellow)
                Pill("1 / 4", Color.White)
            }
        }
        ComicPanel(
            modifier = Modifier.weight(1f),
            color = Color(0xFFFFFBEA),
            contentPadding = PaddingValues(if (compact) 12.dp else 18.dp)
        ) {
            Text("💬 OLÁ, AMIGUINHO!", fontWeight = FontWeight.Black, fontSize = 14.sp, color = ComicRed)
            Text(
                "Encontre e diga o nome de algo que começa com o som da letrinha M.",
                fontWeight = FontWeight.Black,
                fontSize = if (compact) 16.sp else 23.sp,
                lineHeight = if (compact) 20.sp else 31.sp
            )
            Text(
                "M",
                Modifier.fillMaxWidth(),
                color = ComicRed,
                fontSize = if (compact) 48.sp else 96.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Pill("FONEMA /M/", ComicYellow, Modifier.align(Alignment.CenterHorizontally))
            Text("Som da boquinha: “Mmmm...”", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            if (!compact) {
                Text("🍎 MAÇÃ   •   🎒 MOCHILA   •   🪑 MESA", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 16.sp)
            }
        }

        when {
            state.spokenAnswer.isBlank() -> {
                state.message?.let { Text(it, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                GuidedComicButton(
                    if (state.isListening) "ESTOU OUVINDO..." else "RESPONDER COM A VOZ",
                    onListen,
                    color = if (state.isListening) ComicBlue else ComicGreen,
                    leading = "🎙️",
                    trailing = "",
                    cue = "FALE AGORA"
                )
                ComicButton("PEDIR AJUDA", onHelp, color = ComicYellow, leading = "🙋")
            }
            state.answerCorrect == true -> {
                ComicPanel(color = SoftGreen, contentPadding = PaddingValues(12.dp)) {
                    Text("Você disse: “${state.spokenAnswer}”", fontWeight = FontWeight.Bold)
                    Text(state.message.orEmpty(), fontSize = 17.sp, fontWeight = FontWeight.Black)
                }
                GuidedComicButton("CONTINUAR PARA A INTERPRETAÇÃO", onContinue, color = ComicBlue)
            }
            else -> {
                ComicPanel(color = SoftBlue, contentPadding = PaddingValues(12.dp)) {
                    Text("Você disse: “${state.spokenAnswer}”", fontWeight = FontWeight.Bold)
                    Text(state.message.orEmpty(), fontSize = 17.sp, fontWeight = FontWeight.Black)
                }
                GuidedComicButton(
                    "CONTAR OUTRA PALAVRA",
                    onListen,
                    color = ComicGreen,
                    leading = "🎙️",
                    cue = "VAMOS TENTAR JUNTOS"
                )
            }
        }
    }
}
