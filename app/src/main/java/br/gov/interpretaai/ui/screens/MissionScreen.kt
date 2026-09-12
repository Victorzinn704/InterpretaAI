package br.gov.interpretaai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.GuidedScrollScreen
import br.gov.interpretaai.ui.Pill
import br.gov.interpretaai.ui.StageHeader
import br.gov.interpretaai.ui.theme.ComicBlue
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicRed
import br.gov.interpretaai.ui.theme.ComicYellow
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
    GuidedScrollScreen(verticalSpacing = 18.dp) {
        StageHeader("Turminha Interpreta", "Etapa 1 de 4", onBack, onSpeak)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Pill("🔒 MODO ESCOLA • FOCO", ComicYellow)
            Pill("1 / 4", Color.White)
        }
        ComicPanel {
            Text("💬 OLÁ, AMIGUINHO!", fontWeight = FontWeight.Black, fontSize = 14.sp, color = ComicRed)
            Text(
                "Encontre e diga o nome de algo que começa com o som da letrinha M.",
                fontWeight = FontWeight.Black,
                fontSize = 23.sp,
                lineHeight = 31.sp
            )
        }
        ComicPanel(color = Color(0xFFFFFBEA)) {
            Text("M", Modifier.fillMaxWidth(), color = ComicRed, fontSize = 112.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Pill("FONEMA /M/", ComicYellow, Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(18.dp))
            Text("Som da boquinha: /m/ — “Mmmm...”", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
        }
        ComicPanel(color = SoftBlue) {
            Text("💡 PISTAS NOS QUADRINHOS", fontWeight = FontWeight.Black)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Text("🍎\nMAÇÃ", textAlign = TextAlign.Center, fontWeight = FontWeight.Black)
                Text("🎒\nMOCHILA", textAlign = TextAlign.Center, fontWeight = FontWeight.Black)
                Text("🪑\nMESA", textAlign = TextAlign.Center, fontWeight = FontWeight.Black)
            }
        }
        GuidedComicButton(
            if (state.isListening) "ESTOU OUVINDO..." else "RESPONDER COM A VOZ",
            onListen,
            color = if (state.isListening) ComicRed else ComicGreen,
            leading = "🎙️",
            trailing = "",
            cue = "FALE AGORA"
        )
        if (state.spokenAnswer.isNotBlank()) {
            ComicPanel(color = if (state.answerCorrect == true) SoftGreen else Color(0xFFFFE4E6)) {
                Text("Você disse: “${state.spokenAnswer}”", fontWeight = FontWeight.Bold)
                Text(state.message.orEmpty(), fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
        } else if (state.message != null) {
            Text(state.message, color = ComicRed, fontWeight = FontWeight.Bold)
        }
        GuidedComicButton(
            "CONTINUAR PARA A INTERPRETAÇÃO",
            onContinue,
            color = ComicBlue,
            enabled = state.answerCorrect == true
        )
        ComicButton("PEDIR AJUDA AO PROFESSOR", onHelp, color = ComicYellow, leading = "🙋")
    }
}
