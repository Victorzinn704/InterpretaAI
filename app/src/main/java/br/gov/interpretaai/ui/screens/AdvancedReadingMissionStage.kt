package br.gov.interpretaai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.interpretaai.domain.ReadingMissionPack
import br.gov.interpretaai.domain.AssignedLearner
import br.gov.interpretaai.domain.CollaborativeMoment
import br.gov.interpretaai.domain.CollaborativeTurnPlanner
import br.gov.interpretaai.ui.ChildStageScaffold
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.ComicPanel
import br.gov.interpretaai.ui.CollaborativeTurnCue
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.Pill
import br.gov.interpretaai.ui.StageHeader
import br.gov.interpretaai.ui.theme.ComicBlue
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicYellow
import br.gov.interpretaai.ui.theme.SoftBlue
import br.gov.interpretaai.ui.theme.SoftGreen

/** Reutiliza a rota do gibi: ouvir/comparar, escolher uma pista e explicar ao grupo. */
@Composable
fun AdvancedReadingMissionStage(
    pack: ReadingMissionPack,
    learners: List<AssignedLearner> = emptyList(),
    speak: (String) -> Unit,
    onBack: () -> Unit,
    onChoice: (Int) -> Unit,
    onComplete: (String) -> Unit
) {
    var phase by rememberSaveable(pack.name) { mutableIntStateOf(0) }
    var selected by rememberSaveable(pack.name) { mutableIntStateOf(-1) }
    var completionSubmitted by rememberSaveable(pack.name) { mutableStateOf(false) }
    val currentSpeak by rememberUpdatedState(speak)
    val collaborativeMoment = when (phase) {
        0 -> CollaborativeMoment.OBSERVE
        1 -> CollaborativeMoment.RESPOND
        else -> CollaborativeMoment.SHARE
    }
    val collaborativeTurn = CollaborativeTurnPlanner.turn(learners, collaborativeMoment)
    val narration = when (phase) {
        0 -> buildString {
            append(pack.intro)
            pack.texts.forEach { append(" ${it.source}. ${it.text}") }
        }
        1 -> pack.question
        else -> "${pack.replyFor(selected)} ${pack.groupPrompt}"
    }.let { base -> collaborativeTurn?.let { "$base ${it.spokenPrompt}" } ?: base }

    BackHandler { currentSpeak(""); onBack() }
    LaunchedEffect(pack, phase, selected) { currentSpeak(narration) }
    DisposableEffect(Unit) { onDispose { currentSpeak("") } }

    ChildStageScaffold { compact ->
        StageHeader(pack.title, "LEIA • ${pack.stageLabel}", onBack) { currentSpeak(narration) }
        CollaborativeTurnCue(learners, collaborativeMoment)
        when (phase) {
            0 -> {
                Pill("LER • OUÇA AS PISTAS", ComicYellow)
                Text(pack.intro, fontSize = if (compact) 17.sp else 20.sp, fontWeight = FontWeight.Bold)
                pack.texts.forEachIndexed { index, item ->
                    ComicPanel(color = if (index == 0) SoftBlue else Color.White) {
                        Text(item.source, fontWeight = FontWeight.Black)
                        Text(item.text, fontSize = if (compact) 16.sp else 18.sp)
                    }
                }
                GuidedComicButton(
                    "JÁ OBSERVEI", { phase = 1 }, color = ComicBlue,
                    leading = "👀", cue = "AGORA ESCOLHA UMA PISTA"
                )
            }
            1 -> {
                Pill("ENTENDER • ESCOLHA UMA PISTA", ComicYellow)
                ComicPanel(color = SoftBlue) {
                    Text(pack.question, fontSize = if (compact) 19.sp else 22.sp, fontWeight = FontWeight.Black)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pack.choices.forEachIndexed { index, choice ->
                        ComicButton(
                            choice,
                            { selected = index; onChoice(index); phase = 2 },
                            Modifier.weight(1f),
                            color = Color.White,
                            leading = if (index == 0) "📰" else "💬"
                        )
                    }
                }
                ComicButton("OUVIR DE NOVO", { currentSpeak(narrationForTexts(pack)) }, color = ComicYellow, leading = "🔊")
            }
            else -> {
                Pill("INTERPRETAR • EXPLIQUE SUA PISTA", ComicYellow)
                ComicPanel(color = SoftGreen) {
                    Text(if (pack.carriesEvidence(selected)) "🔎 PISTA ENCONTRADA" else "💭 OBSERVE OUTRA VEZ", fontWeight = FontWeight.Black)
                    Text(pack.replyFor(selected), fontSize = if (compact) 17.sp else 20.sp)
                }
                ComicPanel(color = SoftBlue) {
                    Text(pack.groupPrompt, fontSize = if (compact) 18.sp else 21.sp, fontWeight = FontWeight.Black)
                    Text("O aparelho descansa enquanto a turma ouve sua justificativa.")
                }
                GuidedComicButton(
                    if (completionSubmitted) "MISSÃO CONCLUÍDA" else "CONTEI AO GRUPO", {
                        if (!completionSubmitted) {
                            completionSubmitted = true
                            onComplete("${pack.name.lowercase()}:choice_${selected + 1}")
                        }
                    },
                    color = ComicGreen,
                    enabled = !completionSubmitted,
                    leading = "🗣️",
                    trailing = "✓",
                    cue = "APRENDER COM A TURMA"
                )
            }
        }
    }
}

private fun narrationForTexts(pack: ReadingMissionPack) = buildString {
    pack.texts.forEach { append("${it.source}. ${it.text} ") }
    append(pack.question)
}
