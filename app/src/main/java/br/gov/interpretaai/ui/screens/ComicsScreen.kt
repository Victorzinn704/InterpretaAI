package br.gov.interpretaai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import kotlinx.coroutines.delay
import br.gov.interpretaai.domain.ComicStories
import br.gov.interpretaai.domain.BallAnswer
import br.gov.interpretaai.platform.VoiceTurnResult
import br.gov.interpretaai.ui.AttentionCue
import br.gov.interpretaai.ui.ChildStageScaffold
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.ComicPanel
import br.gov.interpretaai.ui.ComicPortrait
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.Pill
import br.gov.interpretaai.ui.SpeechBubble
import br.gov.interpretaai.ui.StageHeader
import br.gov.interpretaai.ui.rememberReengagementVisual
import br.gov.interpretaai.ui.theme.ComicBlue
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicYellow
import br.gov.interpretaai.ui.theme.SoftBlue
import br.gov.interpretaai.ui.theme.SoftGreen

/** Jornada infantil em estados: observar/ouvir, responder e receber reação. */
@Composable
fun ComicsScreen(
    speak: (String) -> Unit,
    onBack: () -> Unit,
    playAudio: (ByteArray, String, () -> Unit) -> Unit = { _, _, fallback -> fallback() },
    listen: (String) -> Unit = {},
    isListening: Boolean = false,
    isResponding: Boolean = false,
    isSpeaking: Boolean = false,
    leiaReply: VoiceTurnResult? = null,
    reducedStimuli: Boolean = false,
    voiceMessage: String? = null,
    onPuzzle: () -> Unit = {},
    onGuidedPuzzle: () -> Unit = {},
    ballAnswer: BallAnswer? = null,
    onBallAnswer: () -> Unit = {},
    onMission: () -> Unit = {},
    onSceneAnswered: (Int, Int) -> Unit = { _, _ -> },
    onWordBuilt: () -> Unit = {},
    onCompleted: (String) -> Unit = {}
) {
    var mode by rememberSaveable { mutableStateOf("menu") }
    var phase by rememberSaveable { mutableIntStateOf(0) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    var interactionNonce by rememberSaveable { mutableIntStateOf(0) }
    var initialCalled by rememberSaveable { mutableStateOf(false) }
    var storyChoices by rememberSaveable { mutableStateOf(List(ComicStories.scenes.size) { -1 }) }
    val scenes = ComicStories.scenes
    val scene = scenes[page]
    val ballNarration = "Lia queria brincar no pátio, mas parou e disse: Eu queria brincar, mas não encontro o que preciso. Davi perguntou: O que está faltando para Lia brincar?"
    val currentSpeak by rememberUpdatedState(speak)
    val path = storyChoices.mapIndexedNotNull { index, choice ->
        scenes[index].choices.getOrNull(choice)?.pathSummary
    }.joinToString("; ")

    val leave = {
        speak("")
        if (mode == "menu") onBack() else { mode = "menu"; phase = 0; page = 0; selected = -1 }
    }
    BackHandler(onBack = leave)

    val narration = when (mode) {
        "menu" -> "Eu sou a LEIA. Escolha uma história para me ajudar."
        "galleryMenu" -> "Escolha uma cena: choro, raiva, riso, felicidade ou locomoção."
        "write" -> "Toque nas letras e monte a palavra bola."
        "apply" -> "Agora ajude seu grupo a representar a história."
        else -> when {
            mode == "story" && phase == 0 -> ballNarration
            mode == "story" && phase == 1 -> "O que está faltando para Lia brincar?"
            phase == 0 -> scene.narration
            phase == 1 -> scene.question
            else -> scene.choices.getOrNull(selected)?.reply.orEmpty()
        }
    }
    LaunchedEffect(mode, page, phase) {
        if (mode == "story" && page == 0 && phase == 0 && !initialCalled) {
            currentSpeak("Oi! Eu sou a LEIA. Quer me ajudar a descobrir o que aconteceu?")
            initialCalled = true
            delay(2_400)
            currentSpeak(ballNarration)
        } else currentSpeak(narration)
    }
    LaunchedEffect(leiaReply) {
        leiaReply?.let { response ->
            response.audio?.let { playAudio(it, response.audioMimeType) { currentSpeak(response.replyText) } }
                ?: currentSpeak(response.replyText)
        }
    }
    DisposableEffect(Unit) { onDispose { currentSpeak("") } }
    val reconnecting = rememberReengagementVisual(
        "$mode-$page-$phase", interactionNonce, isListening || isResponding || isSpeaking,
        reducedStimuli, speak
    )

    ChildStageScaffold { compact ->
        StageHeader(
            title = when {
                mode == "menu" -> "Histórias com a LEIA"
                mode.startsWith("gallery") -> "Cenas expressivas"
                else -> "A bola e os amigos"
            },
            stage = "LEIA • INTERPRETAAI",
            onBack = leave,
            onSpeak = { speak(narration) }
        )
        voiceMessage?.let { ComicPanel(color = ComicYellow) { Text(it, fontWeight = FontWeight.Bold) } }

        when (mode) {
            "menu" -> {
                ComicPanel(color = SoftBlue) {
                    Text("Você é o ajudante da história!", fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("Observe, conte sua ideia e ajude os personagens.", fontSize = 17.sp)
                }
                GuidedComicButton("A BOLA E OS AMIGOS", {
                    page = 0; phase = 0; selected = -1; mode = "story"; interactionNonce++
                }, color = ComicBlue, leading = "⚽", cue = "COMECE A HISTÓRIA")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ComicButton("CENAS", { page = 0; mode = "galleryMenu" }, Modifier.weight(1f), color = ComicYellow, leading = "🎭")
                    ComicButton("PUZZLE", { speak(""); onPuzzle() }, Modifier.weight(1f), color = ComicGreen, leading = "🧩")
                }
                ComicButton("MISSÃO DO SOM M", { speak(""); onMission() }, color = Color.White, leading = "🎤")
            }
            "galleryMenu" -> {
                listOf("😢 Choro", "😠 Raiva", "😆 Riso", "😊 Felicidade", "🚌 Locomoção")
                    .forEachIndexed { index, label ->
                        ComicButton(label, { page = index; phase = 0; selected = -1; mode = "gallery" }, color = Color.White)
                    }
            }
            "write" -> WordBuilding(speak) { onWordBuilt(); mode = "apply" }
            "apply" -> {
                ComicPanel(color = SoftGreen) {
                    Text("APRENDER • Nossa história", fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("A turma criou um caminho e agora vai representar uma cena.", Modifier.padding(top = 8.dp), fontSize = 17.sp)
                }
                ComicPanel(color = SoftBlue) {
                    Text("MISSÃO DO GRUPO", fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("Combinem as vezes, representem a cena e contem como chegaram à ideia.", fontSize = 18.sp)
                    Text("Não existe uma única emoção certa.", fontWeight = FontWeight.Bold)
                }
                GuidedComicButton("CONCLUÍMOS COM A TURMA", {
                    onCompleted(path); mode = "menu"
                }, color = ComicGreen, trailing = "✓", cue = "DEPOIS DA CONVERSA")
            }
            else -> when (phase) {
                0 -> {
                    Pill("LER • OBSERVE E OUÇA • ${page + 1}/${scenes.size}", ComicYellow)
                    Text(scene.title, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    ComicPortrait(page, scene.imageDescription, Modifier.weight(1f))
                    scene.dialogue.forEachIndexed { index, line ->
                        SpeechBubble(line.speaker, line.text, if (index % 2 == 0) SoftBlue else SoftGreen)
                    }
                    GuidedComicButton("EU OBSERVEI", { phase = 1; interactionNonce++ }, color = ComicBlue, cue = "AJUDE A LEIA")
                }
                1 -> {
                    Pill("ENTENDER • CONTE SUA IDEIA", ComicYellow)
                    ComicPanel(color = SoftBlue) {
                        Text(
                            if (mode == "story") "O que está faltando para Lia brincar?" else scene.question,
                            fontWeight = FontWeight.Black,
                            fontSize = if (compact) 19.sp else 22.sp
                        )
                    }
                    if (reconnecting) AttentionCue("CONTE SUA IDEIA PARA A LEIA")
                    leiaReply?.let { response ->
                        ComicPanel(color = SoftGreen) {
                            Text(if (response.degraded) "LEIA • CONTINUA COM VOCÊ" else "LEIA • OUVIU VOCÊ", fontWeight = FontWeight.Black)
                            Text(response.replyText, fontSize = 17.sp)
                        }
                    }
                    if (mode == "story" && ballAnswer == BallAnswer.BALL) {
                        GuidedComicButton(
                            "MONTAR A BOLA",
                            { interactionNonce++; speak(""); onGuidedPuzzle() },
                            color = ComicGreen,
                            leading = "🧩",
                            cue = "AGORA AJUDE LIA"
                        )
                    } else {
                        GuidedComicButton(
                            when { isListening -> "ESTOU OUVINDO..."; isResponding -> "LEIA ESTÁ PENSANDO..."; else -> "FALAR COM A LEIA" },
                            {
                                interactionNonce++
                                listen(if (mode == "story") "comic-ball" else "gallery-${page + 1}")
                            }, color = ComicBlue,
                            enabled = !isListening && !isResponding, leading = "🎤", trailing = "", cue = "RESPONDA COM A VOZ"
                        )
                        if (mode == "story") {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ComicButton("BOLA", {
                                    interactionNonce++
                                    onBallAnswer()
                                    speak("Bola")
                                }, Modifier.weight(1f), color = Color.White, leading = "⚽")
                                ComicButton("OUVIR", {
                                    interactionNonce++
                                    speak(ballNarration)
                                }, Modifier.weight(1f), color = ComicYellow, leading = "🔊")
                            }
                        } else scene.choices.forEachIndexed { index, choice ->
                            ComicButton(choice.label, {
                                selected = index; interactionNonce++
                                phase = 2
                            }, color = Color.White, leading = choice.icon)
                        }
                    }
                }
                else -> {
                    Pill("INTERPRETAR • LEIA REAGE À SUA IDEIA", ComicYellow)
                    ComicPanel(color = SoftGreen) {
                        Text("🌟", fontSize = 48.sp)
                        Text(scene.choices[selected].reply, fontSize = 19.sp, lineHeight = 25.sp)
                    }
                    GuidedComicButton(
                        if (mode == "gallery") "ESCOLHER OUTRA CENA"
                        else if (page < scenes.lastIndex) "PRÓXIMO QUADRINHO" else "MONTAR NOSSO BILHETE",
                        {
                            interactionNonce++
                            if (mode == "gallery") { mode = "galleryMenu"; selected = -1 }
                            else if (page < scenes.lastIndex) { page++; selected = -1; phase = 0 }
                            else mode = "write"
                        }, color = ComicBlue, trailing = "→", cue = "CONTINUE A HISTÓRIA"
                    )
                }
            }
        }
    }
}
