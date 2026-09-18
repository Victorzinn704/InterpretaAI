package br.gov.interpretaai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.interpretaai.ui.ChildStageScaffold
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.ComicPanel
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.Pill
import br.gov.interpretaai.ui.LocalSoundEffect
import br.gov.interpretaai.platform.SoundCue
import br.gov.interpretaai.domain.ReadingMissionCompletion
import br.gov.interpretaai.domain.AssignedLearner
import br.gov.interpretaai.domain.CollaborativeMoment
import br.gov.interpretaai.ui.CollaborativeTurnCue
import br.gov.interpretaai.ui.LeiaReactionScene
import br.gov.interpretaai.ui.LeiaReactionTone
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicYellow
import br.gov.interpretaai.ui.theme.SoftBlue

@Composable
fun CompleteScreen(
    onSpeak: () -> Unit,
    onHome: () -> Unit,
    learners: List<AssignedLearner> = emptyList(),
    completion: ReadingMissionCompletion? = null,
    title: String = "VOCÊ AJUDOU A LÉIA!",
    summary: String = "Você ouviu a história, contou sua ideia e resolveu o desafio.",
    groupPrompt: String = "Conte ao colega qual ideia ajudou a história.",
    reducedStimuli: Boolean = false
) {
    val playSound = LocalSoundEffect.current
    LaunchedEffect(Unit) {
        playSound(SoundCue.CELEBRATE)
        onSpeak()
    }
    ChildStageScaffold(showCompanions = false) { compact ->
        Pill("MISSÃO CONCLUÍDA", Color.White)
        CollaborativeTurnCue(learners, CollaborativeMoment.SHARE)
        LeiaReactionScene(
            message = completion?.summary ?: summary,
            modifier = Modifier.weight(1f),
            label = "LÉIA • COM VOCÊ",
            headline = completion?.title ?: title,
            tone = LeiaReactionTone.CELEBRATE,
            reducedStimuli = reducedStimuli
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("👂 OUVIR", "💬 FALAR", "💡 PENSAR").forEach { achievement ->
                ComicPanel(modifier = Modifier.weight(1f)) {
                    Text(achievement, Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
        }
        ComicPanel(color = SoftBlue) {
            Text(completion?.offScreenPrompt ?: groupPrompt, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        ComicButton("OUVIR PARABÉNS", onSpeak, color = ComicGreen, leading = "🔊")
        GuidedComicButton("VOLTAR AO INÍCIO", onHome, color = ComicYellow, leading = "🏠", cue = "ESCOLHA A PRÓXIMA MISSÃO")
    }
}
