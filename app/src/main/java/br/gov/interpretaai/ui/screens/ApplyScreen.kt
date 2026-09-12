package br.gov.interpretaai.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.interpretaai.AppUiState
import br.gov.interpretaai.domain.ResponseModality
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.ComicPanel
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.GuidedScrollScreen
import br.gov.interpretaai.ui.StageHeader
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicInk
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
    GuidedScrollScreen(verticalSpacing = 18.dp) {
        StageHeader("Mundo Real • Letra M", "Etapa 3 de 4", onBack, onSpeak)
        ComicPanel {
            Text("🎯 APLICAÇÃO PRÁTICA", fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text(
                "Detetive, ache ao seu redor algo com o som da letrinha M!",
                modifier = Modifier.padding(top = 12.dp),
                fontSize = 23.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Black
            )
            Text("Exemplos: maçã 🍎, meia 🧦, mochila 🎒", Modifier.padding(top = 10.dp))
        }
        Text("👉 ESCOLHA COMO RESPONDER", fontWeight = FontWeight.Black, fontSize = 17.sp)
        val options = listOf(
            Triple(ResponseModality.CAMERA, "📸", "Tirar foto") to "Aponte a câmera para o objeto",
            Triple(ResponseModality.VOICE, "🎤", "Falar com a voz") to "Diga o nome do objeto bem alto",
            Triple(ResponseModality.DRAWING, "✏️", "Desenhar na tela") to "Faça um desenho com seu dedinho"
        )
        options.forEach { pair ->
            val (details, subtitle) = pair
            val (modality, emoji, title) = details
            val selected = state.selectedModality == modality
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onChoose(modality) },
                color = if (selected) SoftGreen else Color.White,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(if (selected) 4.dp else 3.dp, if (selected) ComicGreen else ComicInk)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 36.sp)
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                        Text(title, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Text(subtitle)
                    }
                    Text(if (selected) "●" else "○", fontSize = 28.sp)
                }
            }
        }
        GuidedComicButton(
            "CONTINUAR",
            onContinue,
            color = ComicGreen,
            enabled = state.selectedModality != null
        )
        ComicButton("PEDIR AJUDA AO PROFESSOR", onHelp, color = ComicYellow, leading = "🙋")
        state.message?.let { Text(it, fontWeight = FontWeight.Bold) }
    }
}
