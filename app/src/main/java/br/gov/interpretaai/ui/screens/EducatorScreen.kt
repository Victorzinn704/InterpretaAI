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
import androidx.compose.material3.Checkbox
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
import br.gov.interpretaai.domain.DrawingPrompt
import br.gov.interpretaai.domain.AssignedActivity
import br.gov.interpretaai.domain.ClassroomAssignment
import br.gov.interpretaai.domain.LearnerAvatar
import br.gov.interpretaai.domain.LearnerAvatars
import br.gov.interpretaai.domain.PilotRoomParticipant
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
    onReducedStimuliChange: (Boolean) -> Unit,
    challengeMode: Boolean,
    onChallengeModeChange: (Boolean) -> Unit,
    drawingPrompt: DrawingPrompt,
    classroomLabel: String,
    learnerAlias: String,
    activeAvatar: LearnerAvatar,
    assignedActivity: AssignedActivity,
    syncDeviceId: String,
    syncStatus: String,
    roomSyncStatus: String,
    isSyncing: Boolean,
    onPublishAssignment: (ClassroomAssignment) -> Unit,
    onConfigurePilotReceiver: (String, String) -> Unit,
    onRefreshPilotAssignment: () -> Unit,
    onPublishRemoteAssignment: (String, String, ClassroomAssignment) -> Unit,
    onPublishRoomAssignment: (
        String, String, List<PilotRoomParticipant>, Set<String>, ClassroomAssignment
    ) -> Unit
) {
    var unlocked by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var showClear by remember { mutableStateOf(false) }
    var classroomDraft by remember(classroomLabel) { mutableStateOf(classroomLabel) }
    var learnerAliasDraft by remember(learnerAlias) { mutableStateOf(learnerAlias) }
    var avatarDraft by remember(activeAvatar) { mutableStateOf(activeAvatar) }
    var activityDraft by remember(assignedActivity) { mutableStateOf(assignedActivity) }
    var drawingDraft by remember(drawingPrompt) { mutableStateOf(drawingPrompt) }
    var receiverIdDraft by remember(syncDeviceId) { mutableStateOf(syncDeviceId) }
    var receiverTokenDraft by remember { mutableStateOf("") }
    var targetDeviceDraft by remember(syncDeviceId) { mutableStateOf(syncDeviceId) }
    var teacherTokenDraft by remember { mutableStateOf("") }
    var roomIdDraft by remember { mutableStateOf("turma-1a") }
    var roomParticipants by remember { mutableStateOf(emptyList<PilotRoomParticipant>()) }
    var selectedRoomAliases by remember { mutableStateOf(emptySet<String>()) }
    var roomEditorMessage by remember { mutableStateOf<String?>(null) }
    val learnerAliasValid = learnerAliasDraft.matches(
        Regex("${avatarDraft.id}-[0-9]{2,3}")
    )
    val targetDeviceValid = targetDeviceDraft.matches(Regex("[a-zA-Z0-9_-]{6,64}"))
    val roomIdValid = roomIdDraft.matches(Regex("[a-z0-9_-]{3,64}"))

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
        ComicPanel(color = SoftBlue) {
            Text("ENVIAR ATIVIDADE AO TABLET", fontWeight = FontWeight.Black, fontSize = 18.sp)
            OutlinedTextField(
                value = classroomDraft,
                onValueChange = { classroomDraft = it.take(30) },
                label = { Text("Turma") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = learnerAliasDraft,
                onValueChange = {
                    learnerAliasDraft = it.lowercase().filter { character ->
                        character in 'a'..'z' || character.isDigit() || character == '-'
                    }.take(12)
                },
                label = { Text("Código pseudônimo • ex.: pipa-07") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text("O código diferencia participantes. Nome e matrícula não entram no tablet.")
            if (!learnerAliasValid) {
                Text("Use ${avatarDraft.id}-01 até ${avatarDraft.id}-999.", color = ComicRed)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LearnerAvatars.available.forEach { avatar ->
                    Button(
                        onClick = {
                            avatarDraft = avatar
                            val slot = learnerAliasDraft.substringAfterLast('-')
                                .filter(Char::isDigit).take(3).ifBlank { "01" }.padStart(2, '0')
                            learnerAliasDraft = "${avatar.id}-$slot"
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (avatarDraft == avatar) "✓${avatar.emoji}" else avatar.emoji) }
                }
            }
            AssignedActivity.entries.forEach { activity ->
                Button(onClick = { activityDraft = activity }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (activityDraft == activity) "✓ ${activity.emoji} ${activity.label}" else "${activity.emoji} ${activity.label}")
                }
            }
            ComicPanel(color = ComicYellow) {
                Text("O QUE ESTA MISSÃO TRABALHA", fontWeight = FontWeight.Black)
                Text(activityDraft.supportRange, fontWeight = FontWeight.Bold)
                Text(activityDraft.pedagogicalFocus)
                Text("Observe: ${activityDraft.teacherEvidence}")
                Text("Referências: ${activityDraft.bnccReferences}", fontSize = 14.sp)
                Text(
                    "A faixa orienta a mediação; não classifica a criança nem substitui o planejamento docente.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (activityDraft == AssignedActivity.DRAWING) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DrawingPrompt.entries.forEach { prompt ->
                        Button(onClick = { drawingDraft = prompt }, modifier = Modifier.weight(1f)) {
                            Text(if (drawingDraft == prompt) "✓${prompt.emoji}" else prompt.emoji)
                        }
                    }
                }
            }
            ComicButton("ENVIAR PARA ESTE TABLET", {
                val label = classroomDraft.trim().ifBlank { "Turma" }
                val alias = learnerAliasDraft.ifBlank { "${avatarDraft.id}-01" }
                onPublishAssignment(ClassroomAssignment(
                    label, avatarDraft, activityDraft, drawingDraft, alias
                ))
            }, color = ComicGreen, leading = "📤", enabled = learnerAliasValid)
            Text(
                "Publicado: $learnerAlias • ${activeAvatar.emoji} ${assignedActivity.label} • $classroomLabel",
                fontWeight = FontWeight.Bold
            )
        }
        ComicPanel(color = SoftBlue) {
            Text("SINCRONIZAÇÃO DO PILOTO", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(syncStatus, fontWeight = FontWeight.Bold)
            Text("Configure cada tablet uma vez. Tokens não aparecem na área infantil.", fontSize = 14.sp)
            OutlinedTextField(
                value = receiverIdDraft,
                onValueChange = { receiverIdDraft = it.take(64) },
                label = { Text("ID deste tablet") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = receiverTokenDraft,
                onValueChange = { receiverTokenDraft = it.take(160) },
                label = { Text("Token do tablet") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            ComicButton("CONECTAR ESTE TABLET", {
                onConfigurePilotReceiver(receiverIdDraft, receiverTokenDraft)
                receiverTokenDraft = ""
            }, color = ComicBlue, enabled = !isSyncing, leading = "🔗")
            ComicButton(
                "BUSCAR AGORA", onRefreshPilotAssignment,
                color = Color.White, enabled = !isSyncing, leading = "↻"
            )

            Text("DESTINO ONLINE: TABLET OU SALA", fontWeight = FontWeight.Black)
            OutlinedTextField(
                value = targetDeviceDraft,
                onValueChange = { targetDeviceDraft = it.take(64) },
                label = { Text("ID do tablet selecionado") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            ComicButton("ADICIONAR SELEÇÃO À SALA", {
                val participant = runCatching {
                    PilotRoomParticipant(learnerAliasDraft, avatarDraft, targetDeviceDraft.trim())
                }.getOrNull()
                when {
                    participant == null -> roomEditorMessage = "Confira o alias, avatar e ID do tablet."
                    roomParticipants.any { it.learnerAlias == participant.learnerAlias } ->
                        roomEditorMessage = "Esse alias já está na sala."
                    roomParticipants.any { it.deviceId == participant.deviceId } ->
                        roomEditorMessage = "Esse tablet já está na sala."
                    else -> {
                        roomParticipants = roomParticipants + participant
                        selectedRoomAliases = selectedRoomAliases + participant.learnerAlias
                        roomEditorMessage = "${participant.learnerAlias} adicionado."
                    }
                }
            }, color = ComicBlue, enabled = learnerAliasValid && targetDeviceValid, leading = "＋")
            roomEditorMessage?.let { Text(it, fontWeight = FontWeight.Bold) }
            roomParticipants.forEach { participant ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Checkbox(
                        checked = participant.learnerAlias in selectedRoomAliases,
                        onCheckedChange = { selected ->
                            selectedRoomAliases = if (selected) {
                                selectedRoomAliases + participant.learnerAlias
                            } else {
                                selectedRoomAliases - participant.learnerAlias
                            }
                        }
                    )
                    Text(
                        "${participant.avatar.emoji} ${participant.learnerAlias} • ${participant.deviceId}",
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = {
                        roomParticipants = roomParticipants - participant
                        selectedRoomAliases = selectedRoomAliases - participant.learnerAlias
                        roomEditorMessage = "${participant.learnerAlias} removido."
                    }) { Text("Remover") }
                }
            }
            OutlinedTextField(
                value = roomIdDraft,
                onValueChange = {
                    roomIdDraft = it.lowercase().filter { character ->
                        character in 'a'..'z' || character.isDigit() || character == '-' || character == '_'
                    }.take(64)
                },
                label = { Text("Código da sala • ex.: turma-1a") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (!roomIdValid) Text("Use ao menos 3 letras, números, hífen ou sublinhado.", color = ComicRed)
            OutlinedTextField(
                value = teacherTokenDraft,
                onValueChange = { teacherTokenDraft = it.take(160) },
                label = { Text("Token do professor") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            ComicButton(if (isSyncing) "AGUARDE…" else "ENVIAR AO TABLET ONLINE", {
                val assignment = ClassroomAssignment(
                    classroomDraft.trim().ifBlank { "Turma" }, avatarDraft, activityDraft, drawingDraft,
                    learnerAliasDraft.ifBlank { "${avatarDraft.id}-01" }
                )
                onPublishRemoteAssignment(targetDeviceDraft, teacherTokenDraft, assignment)
                teacherTokenDraft = ""
            }, color = ComicGreen, enabled = !isSyncing && learnerAliasValid && targetDeviceValid &&
                teacherTokenDraft.length >= 16, leading = "☁️")
            Text(
                "Selecionados: ${selectedRoomAliases.size} de ${roomParticipants.size}",
                fontWeight = FontWeight.Bold
            )
            val roomActionLabel = when {
                isSyncing -> "AGUARDE…"
                selectedRoomAliases.isEmpty() -> "SELECIONE QUEM VAI RECEBER"
                selectedRoomAliases.size == roomParticipants.size -> "SALVAR SALA E ENVIAR PARA TODOS"
                else -> "SALVAR SALA E ENVIAR PARA ${selectedRoomAliases.size}"
            }
            ComicButton(roomActionLabel, {
                val assignment = ClassroomAssignment(
                    classroomDraft.trim().ifBlank { "Turma" }, avatarDraft, activityDraft, drawingDraft,
                    learnerAliasDraft.ifBlank { "${avatarDraft.id}-01" }
                )
                onPublishRoomAssignment(
                    roomIdDraft, teacherTokenDraft, roomParticipants, selectedRoomAliases, assignment
                )
                teacherTokenDraft = ""
            }, color = ComicYellow, enabled = !isSyncing && selectedRoomAliases.isNotEmpty() &&
                roomIdValid && teacherTokenDraft.length >= 16, leading = "🏫")
            Text(roomSyncStatus, fontWeight = FontWeight.Bold)
            Text(
                "Canal de demonstração: não substitui login institucional e RBAC.",
                fontSize = 13.sp
            )
        }
        ComicPanel(color = SoftBlue) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("NÍVEL DE MEDIAÇÃO", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text(if (challengeMode) "Desafio leitor • quebra-cabeça 3 × 2" else "Apoio inicial • quebra-cabeça 2 × 2")
                    Text("O professor escolhe pelo momento da turma, não pela idade.", fontSize = 14.sp)
                }
                Switch(checked = challengeMode, onCheckedChange = onChallengeModeChange)
            }
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
