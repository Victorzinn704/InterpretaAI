package br.gov.interpretaai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.interpretaai.domain.MetricsSnapshot
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.ComicPanel
import br.gov.interpretaai.ui.Pill
import br.gov.interpretaai.ui.theme.ComicBlue
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicRed
import br.gov.interpretaai.ui.theme.ComicYellow
import br.gov.interpretaai.ui.theme.SoftBlue

@Composable
fun EducatorScreen(
    metrics: MetricsSnapshot,
    isDeviceOwner: Boolean,
    hasDndAccess: Boolean,
    onBack: () -> Unit,
    onRequestDnd: () -> Unit,
    onStartFocus: () -> Unit,
    onStopFocus: () -> Unit,
    onClearMetrics: () -> Unit,
    reducedStimuli: Boolean,
    onReducedStimuliChange: (Boolean) -> Unit
) {
    var unlocked by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var showClear by remember { mutableStateOf(false) }

    if (!unlocked) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            ComicPanel {
                Text("🔐 ÁREA DO EDUCADOR", fontWeight = FontWeight.Black, fontSize = 23.sp)
                Text("Digite o PIN do piloto para acessar métricas e controles.", Modifier.padding(vertical = 14.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.take(4) },
                    label = { Text("PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                ComicButton("ENTRAR", { unlocked = pin == "2468" }, Modifier.padding(top = 14.dp), color = ComicBlue)
                if (pin.length == 4 && pin != "2468") Text("PIN incorreto", color = ComicRed, fontWeight = FontWeight.Bold)
            }
            ComicButton("VOLTAR", onBack, Modifier.padding(top = 18.dp), color = Color.White)
        }
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Pill("📊 PROFESSOR • TURMA 1A", ComicYellow)
            Button(onClick = onBack) { Text("Sair") }
        }
        Text("Painel pedagógico", fontSize = 28.sp, fontWeight = FontWeight.Black)
        Text("Dados deste tablet • sincronização com a secretaria é a próxima integração.")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Sessões", metrics.sessions.toString(), Modifier.weight(1f))
            MetricCard("Participações", (metrics.attempts + metrics.comicObservations).toString(), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Respostas", metrics.attempts.toString(), Modifier.weight(1f))
            MetricCard("Por voz", metrics.voiceResponses.toString(), Modifier.weight(1f))
        }
        ComicPanel(color = SoftBlue) {
            Text("SINAIS PARA O PROFESSOR", fontWeight = FontWeight.Black)
            Text("• Interpretações no gibi: ${metrics.comicObservations}")
            Text("• Ciclos LEIA concluídos: ${metrics.comicCyclesCompleted}")
            Text("• Quebra-cabeças concluídos: ${metrics.puzzlesCompleted}")
            Text("• Tempo médio no quebra-cabeça: ${metrics.averagePuzzleMs / 1000}s")
            Text("• Etapas concluídas: ${metrics.completedStages}")
            Text("• Pedidos de ajuda: ${metrics.helpRequests}")
            Text("• Tempo médio de resposta: ${metrics.averageResponseMs / 1000}s")
            Text("Use tentativas e ajuda para planejar intervenção; não como nota automática.", Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold)
        }
        ComicPanel {
            Text("MODO TOTEM", fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text(if (isDeviceOwner) "✅ Tablet gerenciado: bloqueio completo disponível." else "⚠️ Tablet comum: apenas modo imersivo/fixação de tela.")
            Text(if (hasDndAccess) "✅ Acesso a Não Perturbe concedido." else "⚠️ Acesso a Não Perturbe ainda não concedido.")
        }
        ComicPanel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("REDUZIR ESTÍMULOS", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("Remove partículas e efeitos; preserva voz, contraste e orientação.")
                }
                Switch(checked = reducedStimuli, onCheckedChange = onReducedStimuliChange)
            }
        }
        ComicPanel(color = SoftBlue) {
            Text("🌱 BEM-ESTAR DIGITAL", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text("• sessão curta, com começo e encerramento;")
            Text("• foco sem notificações em tablet gerenciado;")
            Text("• convite para o aparelho descansar e a turma conversar;")
            Text("• acolhimento sem culpa, ranking ou diagnóstico.")
            Text(
                "São proteções pedagógicas e socioemocionais, não avaliação clínica.",
                Modifier.padding(top = 8.dp),
                fontWeight = FontWeight.Bold
            )
        }
        if (!hasDndAccess) ComicButton("AUTORIZAR NÃO PERTURBE", onRequestDnd, color = ComicYellow)
        ComicButton("INICIAR FOCO", onStartFocus, color = ComicGreen, leading = "🔒")
        ComicButton("ENCERRAR FOCO", onStopFocus, color = ComicBlue, leading = "🔓")
        ComicButton("LIMPAR DADOS DE DEMONSTRAÇÃO", { showClear = true }, color = Color.White)
        Text(
            "Secretaria (próxima fase): visão agregada por escola, turma e professor; sem ranking individual de crianças e sem áudio bruto.",
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
        )
    }

    if (showClear) {
        AlertDialog(
            onDismissRequest = { showClear = false },
            title = { Text("Limpar métricas locais?") },
            text = { Text("Esta ação remove os eventos deste tablet e não pode ser desfeita.") },
            confirmButton = { Button(onClick = { onClearMetrics(); showClear = false }) { Text("Limpar") } },
            dismissButton = { Button(onClick = { showClear = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    ComicPanel(modifier = modifier) {
        Text(value, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 30.sp, fontWeight = FontWeight.Black, color = ComicBlue)
        Text(label, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
    }
}
