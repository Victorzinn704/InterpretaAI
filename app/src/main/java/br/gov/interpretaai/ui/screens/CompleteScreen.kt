package br.gov.interpretaai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.ComicPanel
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.GuidedScrollScreen
import br.gov.interpretaai.ui.Pill
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicYellow
import br.gov.interpretaai.ui.theme.SoftBlue

@Composable
fun CompleteScreen(onSpeak: () -> Unit, onHome: () -> Unit) {
    GuidedScrollScreen(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalSpacing = 24.dp
    ) {
        Pill("⚙️ MODO ESCOLA", Color.White)
        ComicPanel {
            Text("🏅", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 68.sp)
            Text(
                "EBA! VOCÊ COMPLETOU A MISSÃO DA LETRINHA M!",
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 25.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black
            )
            Text("Sua aventura no gibi de hoje foi um sucesso!", Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        ComicButton("OUVIR SEUS PARABÉNS", onSpeak, color = ComicGreen, leading = "🔊")
        Text("📖 CONQUISTAS DO SEU GIBI", fontWeight = FontWeight.Black, fontSize = 18.sp)
        val achievements = listOf(
            "👂 1. Som /m/\nSomzinho da boca!",
            "✍️ 2. Letra M\nJuntou MA e ÇÃ!",
            "🛒 3. Mercado\nOnde achar maçã!",
            "👥 4. Amigos\nPartilha com a turma!"
        )
        achievements.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { achievement ->
                    ComicPanel(modifier = Modifier.weight(1f)) {
                        Text(achievement, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
                    }
                }
            }
        }
        ComicPanel(color = SoftBlue) {
            Text("🔒 Aguarde o professor liberar!", fontWeight = FontWeight.Black)
            Text("Conte baixinho ao colega o que mais gostou no gibi.")
        }
        GuidedComicButton(
            "VOLTAR PARA MEUS GIBIS",
            onHome,
            color = ComicYellow,
            leading = "📖",
            cue = "CONTINUE BRINCANDO"
        )
    }
}
