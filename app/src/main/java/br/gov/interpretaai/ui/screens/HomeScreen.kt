package br.gov.interpretaai.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.interpretaai.ui.ChildStageScaffold
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.ComicPanel
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.Pill
import br.gov.interpretaai.ui.LeiaWelcomePortrait
import br.gov.interpretaai.ui.theme.ComicBlue
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicRed
import br.gov.interpretaai.ui.theme.ComicYellow
import br.gov.interpretaai.domain.AssignedActivity
import br.gov.interpretaai.domain.AssignedLearner

@Composable
fun HomeScreen(
    onSchool: () -> Unit,
    classroomLabel: String,
    learners: List<AssignedLearner>,
    assignedActivity: AssignedActivity,
    onSpeak: () -> Unit,
    onFocus: () -> Unit,
    readyStoryTitle: String? = null,
    reducedStimuli: Boolean = false
) {
    val tablet = LocalConfiguration.current.screenWidthDp >= 600
    ChildStageScaffold { compact ->
        ComicPanel(color = ComicRed) {
            Text("INTERPRETA AI", color = Color.White, fontSize = if (compact) 25.sp else 30.sp, fontWeight = FontWeight.Black)
            Text("LEIA • Ler, Entender, Interpretar e Aprender", color = Color.White, fontSize = 16.sp)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LeiaWelcomePortrait(
                height = if (tablet) 300.dp else if (compact) 122.dp else 170.dp,
                reducedStimuli = reducedStimuli
            )
            val avatars = learners.joinToString("") { it.avatar.emoji }
            val identity = if (learners.size == 1) learners.first().avatar.label.uppercase()
                else "GRUPO DE ${learners.size}"
            Pill("$avatars $identity • $classroomLabel", ComicYellow)
            Spacer(Modifier.height(if (compact) 8.dp else 16.dp))
            Text(
                "Investigue histórias, descubra pistas e use as palavras.",
                fontSize = if (tablet) 27.sp else if (compact) 19.sp else 23.sp,
                lineHeight = if (tablet) 34.sp else if (compact) 24.sp else 29.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Com foco na tela. Com a turma fora dela.",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = ComicGreen
            )
            Spacer(Modifier.height(if (compact) 10.dp else 20.dp))
            GuidedComicButton(
                if (readyStoryTitle != null) "COMEÇAR HISTÓRIA" else assignedActivity.label.uppercase(),
                onSchool, Modifier.fillMaxWidth(),
                color = ComicBlue, leading = if (readyStoryTitle != null) "📖" else assignedActivity.emoji,
                cue = "MISSÃO ENVIADA PELO PROFESSOR"
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ComicButton("OUVIR", onSpeak, Modifier.weight(1f), color = ComicYellow, leading = "🔊")
            ComicButton("FOCO", onFocus, Modifier.weight(1f), color = ComicGreen, leading = "🔒")
        }
    }
}
