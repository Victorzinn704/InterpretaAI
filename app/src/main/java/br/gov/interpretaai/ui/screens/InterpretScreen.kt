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
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.ComicPanel
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.GuidedScrollScreen
import br.gov.interpretaai.ui.Pill
import br.gov.interpretaai.ui.StageHeader
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicInk
import br.gov.interpretaai.ui.theme.ComicRed
import br.gov.interpretaai.ui.theme.ComicYellow
import br.gov.interpretaai.ui.theme.SoftGreen

@Composable
fun InterpretScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onSpeak: () -> Unit,
    onChoose: (String) -> Unit,
    onContinue: () -> Unit
) {
    GuidedScrollScreen(verticalSpacing = 18.dp) {
        StageHeader("Interpretação divertida", "Etapa 2 de 4", onBack, onSpeak)
        Pill("🔎 HORA DA INVESTIGAÇÃO", ComicYellow)
        ComicPanel {
            Text("📖 CAPÍTULO: ONDE ENCONTRAR?", color = ComicRed, fontWeight = FontWeight.Black)
            Text(
                "“João quer comprar uma 🍎 maçã fresquinha. Para onde ele deve ir?”",
                modifier = Modifier.padding(vertical = 18.dp),
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black
            )
            Text("🧺  🍎", Modifier.fillMaxWidth(), fontSize = 70.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
        Text("ESCOLHA A VINHETA", fontWeight = FontWeight.Black)
        listOf("Mercado" to "🛒", "Oficina" to "🔧", "Ônibus" to "🚌").forEach { (place, emoji) ->
            val selected = state.selectedPlace == place
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onChoose(place) },
                color = if (selected && place == "Mercado") ComicYellow else Color.White,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(3.dp, ComicInk)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 34.sp)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(place, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(
                            when (place) {
                                "Mercado" -> "Frutas e comidas gostosas"
                                "Oficina" -> "Carros e ferramentas"
                                else -> "Passeios pela cidade"
                            }
                        )
                    }
                    Text(if (selected) "✓" else "○", fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        if (state.selectedPlace != null) {
            ComicPanel(color = if (state.selectedPlace == "Mercado") SoftGreen else Color(0xFFFFE4E6)) {
                Text(
                    if (state.selectedPlace == "Mercado") "🎉 MUITO BEM! No mercado compramos frutas como a maçã."
                    else "Vamos pensar de novo: em qual lugar compramos frutas?",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp
                )
            }
        }
        GuidedComicButton(
            "CONTINUAR PARA O MUNDO REAL",
            onContinue,
            color = ComicGreen,
            enabled = state.selectedPlace == "Mercado"
        )
    }
}
