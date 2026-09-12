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
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.ComicPanel
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.GuidedScrollScreen
import br.gov.interpretaai.ui.Pill
import br.gov.interpretaai.ui.theme.ComicBlue
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicInk
import br.gov.interpretaai.ui.theme.ComicRed
import br.gov.interpretaai.ui.theme.ComicYellow
import br.gov.interpretaai.ui.theme.SoftGreen

@Composable
fun HomeScreen(
    onSchool: () -> Unit,
    onEducator: () -> Unit,
    onSpeak: () -> Unit,
    onFocus: () -> Unit
) {
    GuidedScrollScreen(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalSpacing = 18.dp
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ComicPanel(modifier = Modifier.weight(1f), color = ComicRed) {
                Text("INTERPRETA AI", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("Aprender é entender o mundo! 💬", color = Color.White, fontWeight = FontWeight.Bold)
                Text("LEIA • Ler, Escrever, Interpretar e Aplicar", color = Color.White)
            }
        }

        ComicPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔊", fontSize = 34.sp)
                Column(Modifier.padding(start = 12.dp)) {
                    Text("INSTRUÇÃO POR VOZ!", fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text("Toque para ouvir as dicas sem precisar ler.")
                }
            }
            Spacer(Modifier.height(12.dp))
            ComicButton("OUVIR A TELA", onSpeak, color = ComicYellow, leading = "🔊")
        }

        ComicPanel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Pill("🎒 FOCO NA SALA DE AULA", ComicYellow)
                Pill("● Em aula", SoftGreen)
            }
            Spacer(Modifier.height(18.dp))
            Text("🏫  Modo Escola", fontWeight = FontWeight.Black, fontSize = 27.sp)
            Text(
                "Gibis falados, quebra-cabeças e cenas expressivas para descobrir palavras, conversar e aprender com a turma.",
                fontSize = 17.sp,
                lineHeight = 24.sp
            )
            Spacer(Modifier.height(18.dp))
            GuidedComicButton(
                "ENTRAR NO MODO ESCOLA",
                onSchool,
                color = ComicBlue,
                cue = "COMECE POR AQUI"
            )
        }

        ComicPanel(color = SoftGreen) {
            Pill("🏡 PARA LER EM CASA", ComicYellow)
            Spacer(Modifier.height(14.dp))
            Text("Modo Casa", fontWeight = FontWeight.Black, fontSize = 25.sp)
            Text("Próxima entrega: missões curtas para fazer com a família.", fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            ComicButton("EM BREVE", {}, enabled = false, color = ComicGreen)
        }

        Text(
            "A TIRINHA DO SABER",
            Modifier.fillMaxWidth(),
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("👁\n1. LER", "✏️\n2. ESCREVER", "💡\n3. INTERPRETAR", "✅\n4. APLICAR").forEach {
                ComicPanel(modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp)) {
                    Text(it, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }

        ComicButton("ATIVAR MODO FOCO", onFocus, color = ComicGreen, leading = "🔒")
        ComicButton("ÁREA DO EDUCADOR", onEducator, color = Color.White, leading = "📊")
        Text(
            "Privacidade infantil: o MVP salva somente eventos pedagógicos no aparelho; não salva áudio bruto.",
            color = ComicInk.copy(alpha = .72f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )
    }
}
