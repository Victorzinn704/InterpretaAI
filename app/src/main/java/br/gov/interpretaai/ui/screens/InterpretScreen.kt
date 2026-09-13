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
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.ComicPanel
import br.gov.interpretaai.ui.ChildStageScaffold
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.AttentionCue
import br.gov.interpretaai.ui.Pill
import br.gov.interpretaai.ui.StageHeader
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicRed
import br.gov.interpretaai.ui.theme.ComicYellow
import br.gov.interpretaai.ui.theme.SoftBlue
import br.gov.interpretaai.ui.theme.SoftGreen

@Composable
fun InterpretScreen(
    state: AppUiState,
    onBack: () -> Unit,
    onSpeak: () -> Unit,
    onChoose: (String) -> Unit,
    onContinue: () -> Unit
) {
    ChildStageScaffold { compact ->
        StageHeader("Interpretação divertida", "Etapa 2 de 4", onBack, onSpeak)
        Pill("🔎 HORA DA INVESTIGAÇÃO", ComicYellow)
        ComicPanel(modifier = Modifier.weight(1f, fill = false), contentPadding = PaddingValues(if (compact) 12.dp else 18.dp)) {
            Text("📖 CAPÍTULO: ONDE ENCONTRAR?", color = ComicRed, fontWeight = FontWeight.Black)
            Text(
                "“João quer comprar uma 🍎 maçã fresquinha. Para onde ele deve ir?”",
                modifier = Modifier.padding(vertical = if (compact) 8.dp else 18.dp),
                fontSize = if (compact) 19.sp else 24.sp,
                lineHeight = if (compact) 25.sp else 32.sp,
                fontWeight = FontWeight.Black
            )
            Text("🧺  🍎", Modifier.fillMaxWidth(), fontSize = if (compact) 48.sp else 70.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }

        if (state.selectedPlace != "Mercado") {
            AttentionCue("ESCOLHA UM LUGAR")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Mercado" to "🛒", "Oficina" to "🔧", "Ônibus" to "🚌").forEach { (place, emoji) ->
                    ComicButton(
                        "$emoji\n${place.uppercase()}",
                        { onChoose(place) },
                        Modifier.weight(1f),
                        color = if (state.selectedPlace == place) ComicYellow else Color.White
                    )
                }
            }
        }
        state.selectedPlace?.let { place ->
            ComicPanel(color = if (place == "Mercado") SoftGreen else SoftBlue, contentPadding = PaddingValues(12.dp)) {
                Text(
                    if (place == "Mercado") "🎉 O mercado combina com a busca pela maçã!"
                    else "Você observou outro lugar. Onde encontramos frutas para comprar?",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp
                )
            }
        }
        if (state.selectedPlace == "Mercado") {
            GuidedComicButton("CONTINUAR PARA O MUNDO REAL", onContinue, color = ComicGreen)
        }
    }
}
