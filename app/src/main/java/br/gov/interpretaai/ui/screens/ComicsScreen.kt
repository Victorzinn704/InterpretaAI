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
import br.gov.interpretaai.domain.ComicStories
import br.gov.interpretaai.R
import br.gov.interpretaai.domain.BallAnswer
import br.gov.interpretaai.domain.BallClueAnswer
import br.gov.interpretaai.domain.AssignedActivity
import br.gov.interpretaai.domain.AssignedLearner
import br.gov.interpretaai.domain.AssistedAdvanceReason
import br.gov.interpretaai.domain.CollaborativeMoment
import br.gov.interpretaai.platform.VoiceTurnResult
import br.gov.interpretaai.ui.AttentionCue
import br.gov.interpretaai.ui.AssistedAdvanceStage
import br.gov.interpretaai.ui.ChildStageScaffold
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.ComicPanel
import br.gov.interpretaai.ui.ComicPortrait
import br.gov.interpretaai.ui.CollaborativeTurnCue
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.Pill
import br.gov.interpretaai.ui.SpeechBubble
import br.gov.interpretaai.ui.StageHeader
import br.gov.interpretaai.ui.rememberReengagementVisual
import br.gov.interpretaai.ui.rememberAssistedAdvanceReason
import br.gov.interpretaai.ui.LeiaReactionBanner
import br.gov.interpretaai.ui.LeiaReactionScene
import br.gov.interpretaai.ui.LeiaReactionTone
import br.gov.interpretaai.ui.InteractiveComicPortrait
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
    assignedActivity: AssignedActivity = AssignedActivity.COMIC,
    learners: List<AssignedLearner> = emptyList(),
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
    ballClueAnswer: BallClueAnswer? = null,
    unsuccessfulAttemptNonce: Int = 0,
    onBallAnswer: () -> Unit = {},
    onBallClueAnswer: () -> Unit = {},
    onBallClueOther: () -> Unit = {},
    onBallJourneyStarted: () -> Unit = {},
    onMission: () -> Unit = {},
    onSceneAnswered: (Int, Int) -> Unit = { _, _ -> },
    onWordBuilt: () -> Unit = {},
    onAssistedAdvance: (AssistedAdvanceReason) -> Unit = {},
    onCompleted: (String) -> Unit = {}
) {
    assignedActivity.readingPack?.let { pack ->
        AdvancedReadingMissionStage(
            pack = pack,
            learners = learners,
            speak = speak,
            onBack = onBack,
            onChoice = { choice -> onSceneAnswered(20 + pack.ordinal, choice) },
            onAssistedAdvance = onAssistedAdvance,
            onComplete = onCompleted
        )
        return
    }
    var mode by rememberSaveable { mutableStateOf("story") }
    var phase by rememberSaveable { mutableIntStateOf(0) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    var interactionNonce by rememberSaveable { mutableIntStateOf(0) }
    var initialCalled by rememberSaveable { mutableStateOf(false) }
    var showBallChoice by rememberSaveable { mutableStateOf(false) }
    var showClueOptions by rememberSaveable { mutableStateOf(false) }
    var storyChoices by rememberSaveable { mutableStateOf(List(ComicStories.scenes.size) { -1 }) }
    val scenes = ComicStories.scenes
    val scene = scenes[page]
    val collaborativeMoment = when {
        mode == "write" -> CollaborativeMoment.BUILD
        mode == "apply" -> CollaborativeMoment.SHARE
        phase == 0 -> CollaborativeMoment.OBSERVE
        else -> CollaborativeMoment.RESPOND
    }
    val collaborativeTurn = br.gov.interpretaai.domain.CollaborativeTurnPlanner.turn(
        learners,
        collaborativeMoment
    )
    val ballNarration = "Lia chegou para brincar e encontrou o espaço vazio perto do gol. O que está faltando para a brincadeira começar?"
    val clueQuestion = "Davi encontrou marcas redondas e molhadas no chão. Onde Davi deve procurar?"
    val currentSpeak by rememberUpdatedState(speak)
    val path = storyChoices.mapIndexedNotNull { index, choice ->
        scenes[index].choices.getOrNull(choice)?.pathSummary
    }.joinToString("; ")

    val leave = {
        speak("")
        if (mode == "menu") onBack() else { mode = "menu"; phase = 0; page = 0; selected = -1 }
    }
    BackHandler(onBack = leave)

    if (mode == "rain") {
        RainStoryScreen(
            speak = speak,
            onBack = { mode = "menu" },
            onCompleted = { mode = "menu" },
            reducedStimuli = reducedStimuli
        )
        return
    }

    val narration = when (mode) {
        "menu" -> "Eu sou a LÉIA, e este é o Alfa. Qual aventura vamos viver?"
        "galleryMenu" -> "Qual cena você quer descobrir?"
        "write" -> "Vamos montar a palavra bola, letra por letra."
        "apply" -> "Agora a história é de vocês. Qual cena a turma vai representar?"
        else -> when {
            mode == "story" && phase == 0 -> ballNarration
            mode == "story" && phase == 1 -> "O que está faltando para Lia brincar?"
            mode == "story" && phase == 2 -> clueQuestion
            phase == 0 -> scene.narration
            phase == 1 -> scene.question
            else -> scene.choices.getOrNull(selected)?.reply.orEmpty()
        }
    }.let { base -> collaborativeTurn?.let { "$base ${it.spokenPrompt}" } ?: base }
    LaunchedEffect(mode, page, phase) {
        if (mode == "story" && page == 0 && phase == 0 && !initialCalled) {
            currentSpeak("Oi! Eu sou a LÉIA, e este é o Alfa. Temos um mistério para resolver. $ballNarration")
            initialCalled = true
        } else currentSpeak(narration)
    }
    LaunchedEffect(leiaReply) {
        leiaReply?.takeUnless { it.audioPending }?.let { response ->
            response.audio?.let { playAudio(it, response.audioMimeType) { currentSpeak(response.replyText) } }
                ?: currentSpeak(response.replyText)
        }
    }
    DisposableEffect(Unit) { onDispose { currentSpeak("") } }
    val reconnecting = rememberReengagementVisual(
        "$mode-$page-$phase", interactionNonce, isListening || isResponding || isSpeaking,
        reducedStimuli, speak
    )
    val stageKey = "$mode-$page-$phase"
    val attemptBase = rememberSaveable(stageKey) { unsuccessfulAttemptNonce }
    var localUnsuccessfulAttempts by rememberSaveable(stageKey) { mutableIntStateOf(0) }
    val assistedReason = rememberAssistedAdvanceReason(
        stageKey = stageKey,
        unsuccessfulAttempts = (unsuccessfulAttemptNonce - attemptBase).coerceAtLeast(0) +
            localUnsuccessfulAttempts,
        hasCheckableAnswer = (mode == "story" && phase in 1..2) || mode == "write",
        busy = mode in setOf("menu", "galleryMenu", "apply") ||
            isListening || isResponding || isSpeaking ||
            (mode == "story" && phase == 1 && ballAnswer == BallAnswer.BALL) ||
            (mode == "story" && phase == 2 && ballClueAnswer == BallClueAnswer.TREE)
    )
    if (assistedReason != null) {
        AssistedAdvanceStage(assistedReason, speak) {
            onAssistedAdvance(assistedReason)
            when {
                mode == "story" && phase < 2 -> phase++
                mode == "story" -> onGuidedPuzzle()
                mode == "gallery" && phase == 0 -> phase = 1
                mode == "gallery" -> { mode = "galleryMenu"; selected = -1 }
                mode == "write" -> mode = "apply"
            }
        }
        return
    }

    ChildStageScaffold { compact ->
        StageHeader(
            title = when {
                mode == "menu" -> "Histórias com a LÉIA"
                mode.startsWith("gallery") -> "Cenas expressivas"
                mode == "story" && ballAnswer == null -> "Mistério no pátio"
                else -> "Mistério da bola"
            },
            stage = "LEIA • INTERPRETAAI",
            onBack = leave,
            onSpeak = { speak(narration) }
        )
        if (mode !in listOf("menu", "galleryMenu")) {
            CollaborativeTurnCue(learners, collaborativeMoment)
        }
        voiceMessage?.let { ComicPanel(color = ComicYellow) { Text(it, fontWeight = FontWeight.Bold) } }

        when (mode) {
            "menu" -> {
                ComicPanel(color = SoftBlue) {
                    Text("Você é o ajudante da história!", fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("Lia, Davi, LÉIA e Alfa esperam a sua ideia.", fontSize = 17.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ComicButton("MISTÉRIO DA BOLA", {
                        onBallJourneyStarted()
                        page = 0
                        phase = 0
                        selected = -1
                        showBallChoice = false
                        showClueOptions = false
                        mode = "story"
                        interactionNonce++
                    }, Modifier.weight(1f), color = ComicBlue, leading = "⚽")
                    ComicButton(
                        "ÁGUA DA CHUVA",
                        { mode = "rain"; interactionNonce++ },
                        Modifier.weight(1f),
                        color = ComicYellow,
                        leading = "🌧️"
                    )
                }
                ComicPortrait(
                    sceneIndex = 0,
                    description = "LÉIA apresenta Lia, Davi e Alfa no pátio da escola",
                    imageAspectRatio = if (compact) 16f / 7f else 4f / 3f,
                    drawableRes = R.drawable.comic_ball_opening_v1
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ComicButton("CENAS", { page = 0; mode = "galleryMenu" }, Modifier.weight(1f), color = ComicYellow, leading = "🎭")
                    ComicButton("MONTAR A BOLA", { speak(""); onPuzzle() }, Modifier.weight(1f), color = ComicGreen, leading = "🧩")
                }
            }
            "galleryMenu" -> {
                listOf("😢 Choro", "😠 Raiva", "😆 Riso", "😊 Felicidade", "🚌 Locomoção")
                    .forEachIndexed { index, label ->
                        ComicButton(label, { page = index; phase = 0; selected = -1; mode = "gallery" }, color = Color.White)
                    }
            }
            "write" -> WordBuilding(
                speak = speak,
                onDone = { onWordBuilt(); mode = "apply" },
                onUnsuccessfulAttempt = { localUnsuccessfulAttempts++ }
            )
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
                    Pill(
                        if (mode == "story") "LER • OBSERVE E OUÇA" else "LER • OBSERVE E OUÇA • ${page + 1}/${scenes.size}",
                        ComicYellow
                    )
                    Text(scene.title, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    if (mode == "story") {
                        InteractiveComicPortrait(
                            description = "Lia percebe o espaço vazio da bola, enquanto Davi e Alfa observam a brincadeira",
                            focusLabel = "Marca redonda no espaço vazio",
                            focusX = .64f,
                            focusY = .79f,
                            onFocusFound = {
                                interactionNonce++
                                speak("Você encontrou uma marca redonda. O que deveria estar aqui?")
                            },
                            modifier = if (compact) Modifier.weight(1f) else Modifier,
                            imageAspectRatio = if (compact) 16f / 9f else 4f / 3f,
                            drawableRes = R.drawable.comic_ball_story_01_missing_v2,
                            reducedStimuli = reducedStimuli,
                            tag = "ball-missing-focus"
                        )
                    } else {
                        ComicPortrait(page, scene.imageDescription, Modifier.weight(1f))
                    }
                    scene.dialogue.forEachIndexed { index, line ->
                        SpeechBubble(line.speaker, line.text, if (index % 2 == 0) SoftBlue else SoftGreen)
                    }
                    GuidedComicButton("EU OBSERVEI", { phase = 1; interactionNonce++ }, color = ComicBlue, cue = "AJUDE A LÉIA")
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
                    if (reconnecting) AttentionCue("CONTE SUA IDEIA PARA A LÉIA")
                    if (mode != "story" || ballAnswer != null) leiaReply?.let { response ->
                        LeiaReactionBanner(
                            title = when {
                                response.audioPending -> "LÉIA VAI FALAR COM VOCÊ"
                                response.degraded -> "LÉIA CONTINUA COM VOCÊ"
                                else -> "LÉIA OUVIU SUA IDEIA"
                            },
                            message = response.replyText,
                            tone = if (response.degraded) LeiaReactionTone.ENCOURAGE else LeiaReactionTone.DISCOVERY,
                            reducedStimuli = reducedStimuli
                        )
                    }
                    if (mode == "story" && ballAnswer == BallAnswer.BALL) {
                        GuidedComicButton(
                            "SEGUIR AS PISTAS",
                            { phase = 2; interactionNonce++ },
                            color = ComicGreen,
                            leading = "🔎",
                            cue = "AGORA INVESTIGUE"
                        )
                    } else {
                        GuidedComicButton(
                            when { isListening -> "ESTOU OUVINDO..."; isResponding -> "LÉIA ESTÁ PENSANDO..."; else -> "FALAR COM A LÉIA" },
                            {
                                interactionNonce++
                                listen(if (mode == "story") "comic-ball" else "gallery-${page + 1}")
                            }, color = ComicBlue,
                            enabled = !isListening && !isResponding,
                            leading = if (isResponding) "💭" else "🎤",
                            trailing = "",
                            cue = when {
                                isListening -> "ESTOU OUVINDO SUA IDEIA"
                                isResponding -> "ESTOU JUNTANDO AS PISTAS"
                                else -> "RESPONDA COM A VOZ"
                            },
                            attention = !reducedStimuli
                        )
                        if (mode == "story") {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (showBallChoice) {
                                    ComicButton("BOLA", {
                                        interactionNonce++
                                        onBallAnswer()
                                        speak("Bola")
                                    }, Modifier.weight(1f), color = Color.White, leading = "⚽")
                                } else {
                                    ComicButton("RESPONDER COM FIGURA", {
                                        interactionNonce++
                                        showBallChoice = true
                                        speak("Olhe a cena. O que está faltando para a Lia brincar?")
                                    }, Modifier.weight(1f), color = Color.White, leading = "👀")
                                }
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
                else -> if (mode == "story") {
                    Pill("INTERPRETAR • SIGA AS MARCAS", ComicYellow)
                    ComicPanel(color = SoftBlue) {
                        Text(
                            if (ballClueAnswer in listOf(BallClueAnswer.OTHER, BallClueAnswer.EMPTY)) {
                                leiaReply?.replyText.orEmpty()
                            } else clueQuestion,
                            fontWeight = FontWeight.Black,
                            fontSize = if (compact) 18.sp else 21.sp
                        )
                    }
                    InteractiveComicPortrait(
                        description = "Lia, Davi e Alfa seguem marcas circulares molhadas até a árvore",
                        focusLabel = "Última marca molhada perto da árvore",
                        focusX = .73f,
                        focusY = .76f,
                        onFocusFound = {
                            interactionNonce++
                            speak("As marcas molhadas chegam até a árvore. Onde você procuraria?")
                        },
                        imageAspectRatio = if (compact) 16f / 9f else 4f / 3f,
                        drawableRes = R.drawable.comic_ball_story_02_trail_v2,
                        reducedStimuli = reducedStimuli,
                        tag = "ball-trail-focus"
                    )
                    if (ballClueAnswer == BallClueAnswer.TREE) leiaReply?.let { response ->
                        LeiaReactionBanner(
                            title = "LÉIA INVESTIGA COM VOCÊ",
                            message = response.replyText,
                            tone = LeiaReactionTone.DISCOVERY,
                            reducedStimuli = reducedStimuli
                        )
                    }
                    if (ballClueAnswer == BallClueAnswer.TREE) {
                        GuidedComicButton(
                            "MONTAR A BOLA",
                            { interactionNonce++; speak(""); onGuidedPuzzle() },
                            color = ComicGreen,
                            leading = "🧩",
                            cue = "CONSOLIDE A DESCOBERTA"
                        )
                    } else {
                        if (reconnecting) AttentionCue("MOSTRE ONDE DAVI DEVE PROCURAR")
                        GuidedComicButton(
                            when { isListening -> "ESTOU OUVINDO..."; isResponding -> "LÉIA ESTÁ PENSANDO..."; else -> "CONTAR SUA PISTA" },
                            {
                                interactionNonce++
                                listen("comic-ball-clue")
                            },
                            color = ComicBlue,
                            enabled = !isListening && !isResponding,
                            leading = "🎤",
                            trailing = "",
                            cue = when {
                                isListening -> "ESTOU OUVINDO SUA PISTA"
                                isResponding -> "ESTOU JUNTANDO AS PISTAS"
                                else -> "CONTE SUA INVESTIGAÇÃO"
                            },
                            attention = !reducedStimuli
                        )
                        if (showClueOptions) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ComicButton(
                                    "ÁRVORE",
                                    { interactionNonce++; onBallClueAnswer() },
                                    Modifier.weight(1f),
                                    color = Color.White,
                                    leading = "🌳"
                                )
                                ComicButton(
                                    "MOCHILA",
                                    { interactionNonce++; onBallClueOther() },
                                    Modifier.weight(1f),
                                    color = Color.White,
                                    leading = "🎒"
                                )
                            }
                        } else {
                            ComicButton(
                                "RESPONDER COM FIGURAS",
                                { interactionNonce++; showClueOptions = true; speak("Siga as marcas molhadas. Onde você procuraria?") },
                                color = ComicYellow,
                                leading = "👀"
                            )
                        }
                    }
                } else {
                    Pill("INTERPRETAR • LÉIA REAGE À SUA IDEIA", ComicYellow)
                    LeiaReactionScene(
                        message = scene.choices[selected].reply,
                        modifier = Modifier.weight(1f),
                        label = "LÉIA • REAGE À SUA IDEIA",
                        tone = LeiaReactionTone.DISCOVERY,
                        reducedStimuli = reducedStimuli
                    )
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
