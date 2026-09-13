package br.gov.interpretaai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.interpretaai.AppUiState
import br.gov.interpretaai.domain.ResponseModality
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.ComicPanel
import br.gov.interpretaai.ui.ChildStageScaffold
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.AttentionCue
import br.gov.interpretaai.ui.StageHeader
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicYellow
import br.gov.interpretaai.ui.theme.SoftGreen

@Composable
fun ApplyScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onSpeak: () -> Unit,
    onChoose: (ResponseModality) -> Unit,
    onContinue: () -> Unit,
    onHelp: () -> Unit
) {
    ChildStageScaffold { compact ->
        StageHeader("Mundo Real • Letra M", "Etapa 3 de 4", onBack, onSpeak)
        ComicPanel(modifier = Modifier.weight(1f), contentPadding = PaddingValues(if (compact) 12.dp else 18.dp)) {
            Text("🎯 APLICAÇÃO PRÁTICA", fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text(
                "Detetive, ache ao seu redor algo com o som da letrinha M!",
                modifier = Modifier.padding(top = if (compact) 6.dp else 12.dp),
                fontSize = if (compact) 19.sp else 23.sp,
                lineHeight = if (compact) 25.sp else 30.sp,
                fontWeight = FontWeight.Black
            )
            Text("Exemplos: maçã 🍎, meia 🧦, mochila 🎒", Modifier.padding(top = 10.dp))
            Text(
                "Depois, o celular descansa e a descoberta continua com a dupla.",
                Modifier.padding(top = 10.dp),
                fontWeight = FontWeight.Bold
            )
        }
        AttentionCue("ESCOLHA COMO PARTICIPAR")
        val options = listOf(
            Triple(ResponseModality.CAMERA, "📸", "USAR CÂMERA"),
            Triple(ResponseModality.VOICE, "🗣️", "CONTAR À DUPLA")
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (modality, emoji, title) ->
                ComicButton(
                    "$emoji  $title",
                    { onChoose(modality) },
                    Modifier.weight(1f),
                    color = if (state.selectedModality == modality) SoftGreen else Color.White
                )
            }
        }
        state.selectedModality?.let { modality ->
            GuidedComicButton(
                if (modality == ResponseModality.CAMERA) "ABRIR A CÂMERA" else "COMEÇAR A CONVERSA",
                onContinue,
                color = ComicGreen,
                cue = "VAMOS CONTINUAR"
            )
        } ?: ComicButton("PEDIR AJUDA", onHelp, color = ComicYellow, leading = "🙋")
        state.message?.let { Text(it, Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
    }
}
