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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.interpretaai.domain.MetricsSnapshot
import br.gov.interpretaai.domain.DrawingPrompt
import br.gov.interpretaai.domain.AssignedActivity
import br.gov.interpretaai.domain.ClassroomAssignment
import br.gov.interpretaai.domain.LearnerAvatar
import br.gov.interpretaai.domain.LearnerAvatars
import br.gov.interpretaai.domain.PilotRoomParticipant
import br.gov.interpretaai.platform.TabletCapabilityReport
import br.gov.interpretaai.platform.TabletReadiness
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.ComicPanel
import br.gov.interpretaai.ui.Pill
import br.gov.interpretaai.ui.theme.ComicBlue
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicRed
import br.gov.interpretaai.ui.theme.ComicYellow
import br.gov.interpretaai.ui.theme.SoftBlue

private enum class EducatorSection { MISSION, CLASSROOM, TABLET }

@Composable
fun EducatorScreen(
    metrics: MetricsSnapshot,
    isDeviceOwner: Boolean,
    hasDndAccess: Boolean,
    tabletReport: TabletCapabilityReport,
    onCopyTabletReport: (TabletCapabilityReport) -> Unit,
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
    pairingServerUrl: String,
    pairedV2DeviceId: String,
    devicePairingStatus: String,
    isPairingDevice: Boolean,
    onPublishAssignment: (ClassroomAssignment) -> Unit,
    onConfigurePilotReceiver: (String, String) -> Unit,
    onRefreshPilotAssignment: () -> Unit,
    onPairV2Device: (String, String) -> Unit,
    onSyncPreparedStories: () -> Unit,
    onPublishRemoteAssignment: (String, String, ClassroomAssignment) -> Unit,
    onPublishRoomAssignment: (
        String, String, List<PilotRoomParticipant>, Set<String>, ClassroomAssignment
    ) -> Unit
) {
    var unlocked by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var showClear by remember { mutableStateOf(false) }
    var section by remember { mutableStateOf(EducatorSection.MISSION) }
    var showPilotSetup by remember { mutableStateOf(false) }
    var showLearningSignals by remember { mutableStateOf(false) }
    var showMissionEditor by remember { mutableStateOf(false) }
    var showMissionSettings by remember { mutableStateOf(false) }
    var showV2Pairing by remember { mutableStateOf(false) }
    var v2ServerDraft by remember(pairingServerUrl) { mutableStateOf(pairingServerUrl) }
    var pairingCodeDraft by remember { mutableStateOf("") }
    var classroomDraft by remember(classroomLabel) { mutableStateOf(classroomLabel) }
    var learnerAliasDraft by remember(learnerAlias) { mutableStateOf(learnerAlias) }
    var avatarDraft by remember(activeAvatar) { mutableStateOf(activeAvatar) }
    var activityDraft by remember(assignedActivity) { mutableStateOf(assignedActivity) }
    var selectedYear by remember { mutableStateOf<Int?>(null) }
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
    val visibleActivities = AssignedActivity.entries.filter { activity ->
        selectedYear == null || activity.supportsYear(selectedYear!!)
    }

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

    val tabletLayout = LocalConfiguration.current.screenWidthDp >= 720
    val pagePadding = if (tabletLayout) 28.dp else 16.dp
    Column(
        Modifier.fillMaxSize()
            .testTag("educator-scroll")
            .verticalScroll(rememberScrollState())
            .padding(horizontal = pagePadding, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Pill("CADERNO DA PROFESSORA • ${classroomDraft.uppercase()}", ComicYellow)
            Button(onClick = onBack) { Text("Sair") }
        }
        Text("Acompanhar a aula", fontSize = if (tabletLayout) 32.sp else 27.sp, fontWeight = FontWeight.Black)
        Text("A mesma história do APK, da preparação à conversa com a turma.")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ComicButton(
                "MISSÃO", { section = EducatorSection.MISSION }, Modifier.weight(1f),
                color = if (section == EducatorSection.MISSION) ComicYellow else Color.White,
                leading = "🎯",
                tag = "educator-tab-mission"
            )
            ComicButton(
                "TURMA", { section = EducatorSection.CLASSROOM },
                Modifier.weight(1f),
                color = if (section == EducatorSection.CLASSROOM) ComicYellow else Color.White,
                leading = "👥",
                tag = "educator-tab-classroom"
            )
            ComicButton(
                "TABLET", { section = EducatorSection.TABLET },
                Modifier.weight(1f),
                color = if (section == EducatorSection.TABLET) ComicYellow else Color.White,
                leading = "📱",
                tag = "educator-tab-tablet"
            )
        }
        Text(
            when (section) {
                EducatorSection.MISSION -> "1. Escolha o que ensinar e confira os sinais da aprendizagem."
                EducatorSection.CLASSROOM -> "2. Forme a turma e envie a missão escolhida."
                EducatorSection.TABLET -> "3. Prepare foco, acessibilidade e compatibilidade do aparelho."
            },
            fontWeight = FontWeight.Bold
        )
        if (section == EducatorSection.MISSION) {
            ComicButton(
                if (showLearningSignals) "OCULTAR SINAIS DA TURMA" else "VER SINAIS DA TURMA",
                { showLearningSignals = !showLearningSignals },
                color = Color.White,
                leading = "📊"
            )
            if (showLearningSignals) {
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
            }
        }
        if (section == EducatorSection.TABLET) {
            ComicPanel(color = SoftBlue) {
                Text("CONEXÃO 2.0 E MODO OFFLINE", fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(devicePairingStatus, fontWeight = FontWeight.Bold)
                if (pairedV2DeviceId.isNotBlank()) {
                    Text("ID deste aparelho: $pairedV2DeviceId", fontSize = 14.sp)
                    ComicButton(
                        "SINCRONIZAR HISTÓRIAS AGORA", onSyncPreparedStories,
                        color = ComicGreen, enabled = !isPairingDevice, leading = "↻"
                    )
                }
                if (showV2Pairing) {
                    Text(
                        "No Estúdio, gere um código temporário para a turma. Digite somente o código; " +
                            "a credencial permanente será protegida pelo Android.",
                        fontSize = 14.sp
                    )
                    OutlinedTextField(
                        value = v2ServerDraft,
                        onValueChange = { v2ServerDraft = it.take(200) },
                        label = { Text("Servidor HTTPS") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("v2-pairing-server")
                    )
                    OutlinedTextField(
                        value = pairingCodeDraft,
                        onValueChange = { value ->
                            pairingCodeDraft = value.uppercase().filter { character ->
                                character.isLetterOrDigit() || character == '-'
                            }.take(9)
                        },
                        label = { Text("Código temporário • XXXX-XXXX") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("v2-pairing-code")
                    )
                    ComicButton("PAREAR E PREPARAR OFFLINE", {
                        onPairV2Device(v2ServerDraft, pairingCodeDraft)
                        pairingCodeDraft = ""
                    }, color = ComicBlue, leading = "🔗", enabled = !isPairingDevice
                        && v2ServerDraft.isNotBlank() && pairingCodeDraft.length in 8..9,
                        tag = "v2-pairing-submit")
                } else {
                    ComicButton(
                        if (pairedV2DeviceId.isBlank()) "PAREAR ESTE TABLET" else "TROCAR PAREAMENTO",
                        { showV2Pairing = true }, color = Color.White, leading = "⚙️",
                        tag = "v2-pairing-open"
                    )
                }
                Text(
                    "Sem internet, o conteúdo já verificado continua funcionando. A próxima conexão " +
                        "busca atualizações e retiradas da professora.",
                    fontSize = 14.sp
                )
            }
            ComicPanel {
                Text("MODO TOTEM", fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(if (isDeviceOwner) "✅ Tablet gerenciado: bloqueio completo disponível." else "⚠️ Tablet comum: apenas modo imersivo/fixação de tela.")
                Text(if (hasDndAccess) "✅ Acesso a Não Perturbe concedido." else "⚠️ Acesso a Não Perturbe ainda não concedido.")
            }
            TabletDiagnosticCard(tabletReport, onCopyTabletReport)
        }
        if (section == EducatorSection.MISSION) {
        ComicPanel(color = ComicYellow) {
            Text("HISTÓRIA EM FOCO", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text("${activityDraft.emoji} ${activityDraft.label}", fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text("${classroomDraft.trim().ifBlank { "Turma" }} • ${avatarDraft.emoji} ${learnerAliasDraft.ifBlank { "${avatarDraft.id}-01" }}")
            Text("Preparar → enviar → acompanhar", fontWeight = FontWeight.Bold)
            if (!showMissionEditor) {
                ComicButton("USAR NESTE TABLET", {
                    val label = classroomDraft.trim().ifBlank { "Turma" }
                    val alias = learnerAliasDraft.ifBlank { "${avatarDraft.id}-01" }
                    onPublishAssignment(ClassroomAssignment(
                        label, avatarDraft, activityDraft, drawingDraft, alias
                    ))
                }, color = ComicGreen, leading = "📤", enabled = learnerAliasValid)
                ComicButton(
                    "ALTERAR HISTÓRIA E TURMA", { showMissionEditor = true },
                    color = Color.White, leading = "✏️"
                )
            } else {
                ComicButton(
                    "FECHAR EDIÇÃO", { showMissionEditor = false },
                    color = Color.White, leading = "✓"
                )
            }
        }
        if (showMissionEditor) {
        ComicPanel(color = SoftBlue) {
            Text("EDITAR HISTÓRIA E TURMA", fontWeight = FontWeight.Black, fontSize = 18.sp)
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
            Text("ANO / RECORTE DA TURMA", fontWeight = FontWeight.Black)
            Text(
                "Use como filtro de planejamento; TODAS preserva missões de recomposição.",
                fontSize = 14.sp
            )
            listOf(null, 1, 2, 3, 4, 5).chunked(3).forEach { yearRow ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    yearRow.forEach { year ->
                        val label = year?.let { "${it}º" } ?: "TODAS"
                        Button(
                            onClick = {
                                selectedYear = year
                                if (year != null && !activityDraft.supportsYear(year)) {
                                    activityDraft = AssignedActivity.entries.first { it.supportsYear(year) }
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("mission-year-${year ?: "all"}")
                        ) {
                            Text(if (selectedYear == year) "✓ $label" else label)
                        }
                    }
                }
            }
            Text(
                "${visibleActivities.size} missões disponíveis neste recorte",
                fontWeight = FontWeight.Bold
            )
            visibleActivities.forEach { activity ->
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
            ComicButton("USAR NESTE TABLET", {
                val label = classroomDraft.trim().ifBlank { "Turma" }
                val alias = learnerAliasDraft.ifBlank { "${avatarDraft.id}-01" }
                onPublishAssignment(ClassroomAssignment(
                    label, avatarDraft, activityDraft, drawingDraft, alias
                ))
            }, color = ComicGreen, leading = "📤", enabled = learnerAliasValid,
                tag = "mission-publish")
            Text(
                "Publicado: $learnerAlias • ${activeAvatar.emoji} ${assignedActivity.label} • $classroomLabel",
                fontWeight = FontWeight.Bold
            )
        }
        }
        }
        if (section == EducatorSection.CLASSROOM) {
        ComicPanel(color = ComicYellow) {
            Text("MISSÃO PRONTA PARA ENVIO", fontWeight = FontWeight.Black)
            Text("${activityDraft.emoji} ${activityDraft.label}", fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text("Turma: ${classroomDraft.trim().ifBlank { "Turma" }}")
            Text("Para mudar atividade, avatar ou pista, use a aba MISSÃO.", fontWeight = FontWeight.Bold)
        }
        ComicPanel(color = SoftBlue) {
            Text("2 • FORMAR TURMA E ENVIAR", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(syncStatus, fontWeight = FontWeight.Bold)
            if (showPilotSetup) {
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
            } else {
                ComicButton(
                    "CONFIGURAR CONEXÃO", { showPilotSetup = true },
                    color = Color.White, leading = "⚙️"
                )
            }
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
            Text(
                "Para dupla ou grupo, repita o ID do mesmo tablet com avatares diferentes. " +
                    "A participação será coletiva, nunca atribuída a uma criança.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            ComicButton("ADICIONAR AVATAR AO TABLET", {
                val participant = runCatching {
                    PilotRoomParticipant(learnerAliasDraft, avatarDraft, targetDeviceDraft.trim())
                }.getOrNull()
                when {
                    participant == null -> roomEditorMessage = "Confira o alias, avatar e ID do tablet."
                    roomParticipants.any { it.learnerAlias == participant.learnerAlias } ->
                        roomEditorMessage = "Esse alias já está na sala."
                    roomParticipants.count { it.deviceId == participant.deviceId } >= 4 ->
                        roomEditorMessage = "Um tablet compartilhado aceita até quatro avatares no piloto."
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
                    val aliasesOnTablet = roomParticipants
                        .filter { it.deviceId == participant.deviceId }
                        .map { it.learnerAlias }
                        .toSet()
                    Checkbox(
                        checked = participant.learnerAlias in selectedRoomAliases,
                        onCheckedChange = { selected ->
                            selectedRoomAliases = if (selected) {
                                selectedRoomAliases + aliasesOnTablet
                            } else {
                                selectedRoomAliases - aliasesOnTablet
                            }
                        }
                    )
                    Text(
                        "${participant.avatar.emoji} ${participant.learnerAlias} • ${participant.deviceId}" +
                            if (aliasesOnTablet.size > 1) " • grupo ${aliasesOnTablet.size}" else "",
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
        }
        if (section == EducatorSection.MISSION) {
        ComicButton(
            if (showMissionSettings) "OCULTAR MEDIAÇÃO E ACESSIBILIDADE" else "MEDIAÇÃO E ACESSIBILIDADE",
            { showMissionSettings = !showMissionSettings },
            color = Color.White,
            leading = "⚙️"
        )
        if (showMissionSettings) {
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
        }
        if (section == EducatorSection.TABLET) {
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
            "A API do piloto entrega agregados à secretaria, sem alias ou áudio. Painel e acesso institucional são a próxima fase.",
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
        )
        }
    }
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
fun TabletDiagnosticCard(
    tabletReport: TabletCapabilityReport,
    onCopyTabletReport: (TabletCapabilityReport) -> Unit
) {
    ComicPanel(color = when (tabletReport.readiness) {
        TabletReadiness.READY -> ComicGreen
        TabletReadiness.LIMITED -> ComicYellow
        TabletReadiness.INCOMPATIBLE -> ComicRed
    }) {
        Text("DIAGNÓSTICO DESTE TABLET", fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(tabletReport.readiness.label, fontWeight = FontWeight.Black)
        Text("${tabletReport.manufacturer} ${tabletReport.model} • Android ${tabletReport.androidRelease} (SDK ${tabletReport.androidSdk})")
        Text("Tela ${tabletReport.screenPixels} • menor lado ${tabletReport.shortestSideDp}dp • ${tabletReport.densityDpi} dpi")
        Text("${if (tabletReport.microphone) "✅" else "⚠️"} Microfone  ${if (tabletReport.camera) "✅" else "⚠️"} Câmera  ${if (tabletReport.touchscreen) "✅" else "⚠️"} Toque")
        Text("${if (tabletReport.speechRecognitionAvailable) "✅" else "⚠️"} Reconhecimento de voz  ${if (tabletReport.stylusActive) "✅" else "ℹ️"} Caneta ativa")
        Text("${if (tabletReport.lockTaskPermitted) "✅" else "⚠️"} Lock Task total  ${if (tabletReport.lockTaskActive) "✅" else "ℹ️"} Foco ativo")
        tabletReport.recommendations.forEach { recommendation -> Text("• $recommendation", fontSize = 14.sp) }
        Text(
            "A caneta pode não aparecer até tocar a tela. O diagnóstico não coleta serial, IMEI, conta, IP, token ou dado infantil.",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        ComicButton(
            "COPIAR DIAGNÓSTICO", { onCopyTabletReport(tabletReport) },
            color = Color.White, leading = "📋"
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
