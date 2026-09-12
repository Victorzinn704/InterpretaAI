package br.gov.interpretaai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import br.gov.interpretaai.ui.AttentionCue
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.ComicPanel
import br.gov.interpretaai.ui.ComicPortrait
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.GuidedScrollScreen
import br.gov.interpretaai.ui.Pill
import br.gov.interpretaai.ui.SpeechBubble
import br.gov.interpretaai.ui.StageHeader
import br.gov.interpretaai.ui.theme.ComicBlue
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicYellow
import br.gov.interpretaai.ui.theme.SoftBlue
import br.gov.interpretaai.ui.theme.SoftGreen

/** A small, narrated LEIA cycle: read images, interpret, build one word and apply in a group. */
@Composable
fun ComicsScreen(
    speak: (String) -> Unit,
    onBack: () -> Unit,
    voiceMessage: String? = null,
    onPuzzle: () -> Unit = {},
    onMission: () -> Unit = {},
    onSceneAnswered: (sceneIndex: Int, choiceIndex: Int) -> Unit = { _, _ -> },
    onWordBuilt: () -> Unit = {},
    onCompleted: (path: String) -> Unit = {}
) {
    var mode by rememberSaveable { mutableStateOf("menu") }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    var storyChoices by rememberSaveable { mutableStateOf(List(ComicStories.scenes.size) { -1 }) }
    val scenes = ComicStories.scenes
    val scene = scenes[page]
    val currentSpeak by rememberUpdatedState(speak)
    val scroll = rememberScrollState()
    val path = storyChoices.mapIndexedNotNull { index, choice ->
        scenes[index].choices.getOrNull(choice)?.pathSummary
    }.joinToString("; ")

    val leave = {
        speak("")
        if (mode == "menu") onBack() else {
            mode = "menu"
            page = 0
            selected = -1
        }
    }
    BackHandler(onBack = leave)

    val narration = when (mode) {
        "menu" -> "LEIA é o coração do Interpreta AI: Ler, Escrever, Interpretar e Aplicar. Escolha o gibi A bola e os amigos, ou as cenas expressivas."
        "write" -> "Vamos escrever um bilhete: Traga a bola para brincar! Complete com a palavra bola. Toque nas letras para montar. Cada letra será falada."
        "galleryMenu" -> "Escolha uma cena para ouvir e conversar: choro, raiva, riso, felicidade ou formas de chegar à escola."
        "apply" -> "Na história criada pela turma, $path. Agora combinem as vezes de brincar, representem uma cena e contem como os personagens se sentiram."
        else -> scene.narration
    }
    LaunchedEffect(mode, page) {
        scroll.scrollTo(0)
        currentSpeak(narration)
    }
    DisposableEffect(Unit) { onDispose { currentSpeak("") } }

    GuidedScrollScreen(scrollState = scroll) {
        StageHeader(
            title = if (mode == "menu") "Nosso mundo em quadrinhos"
                else if (mode.startsWith("gallery")) "Cenas expressivas" else "A bola e os amigos",
            stage = "LEIA • InterpretaAI",
            onBack = leave,
            onSpeak = { speak(narration) }
        )
        voiceMessage?.let {
            ComicPanel(color = ComicYellow) {
                Text(it, fontWeight = FontWeight.Bold)
            }
        }
        when (mode) {
            "menu" -> {
                ComicPanel {
                    Text("Ler • Escrever • Interpretar • Aplicar", fontWeight = FontWeight.Bold)
                    Text("Ouça, escolha e participe com a turma!", Modifier.padding(top = 8.dp))
                }
                GuidedComicButton("GIBI: A BOLA E OS AMIGOS", {
                    page = 0
                    selected = storyChoices.first()
                    mode = "story"
                }, color = ComicBlue, leading = "⚽", cue = "COMECE A HISTÓRIA")
                ComicButton("CENAS EXPRESSIVAS", {
                    page = 0
                    selected = -1
                    mode = "galleryMenu"
                }, color = ComicYellow, leading = "🎭")
                ComicButton("QUEBRA-CABEÇAS DE PALAVRAS", {
                    speak("")
                    onPuzzle()
                }, color = ComicGreen, leading = "🧩")
                ComicButton("MISSÃO DA LETRA M", {
                    speak("")
                    onMission()
                }, color = Color.White, leading = "🎤")
            }
            "galleryMenu" -> {
                listOf("😢 Choro", "😠 Raiva", "😆 Riso", "😊 Felicidade", "🚌 Locomoção")
                    .forEachIndexed { index, label ->
                        ComicButton(label, {
                            page = index
                            selected = -1
                            mode = "gallery"
                        }, color = Color.White)
                    }
            }
            "write" -> WordBuilding(speak) {
                onWordBuilt()
                mode = "apply"
            }
            "apply" -> {
                ComicPanel(color = SoftGreen) {
                    Text("APLICAR • Nossa história", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    storyChoices.forEachIndexed { index, choice ->
                        scenes[index].choices.getOrNull(choice)?.let {
                            Text("• A turma ${it.pathSummary}.", Modifier.padding(top = 8.dp), fontSize = 17.sp)
                        }
                    }
                }
                ComicPanel(color = SoftBlue) {
                    Text("MISSÃO DO GRUPO", fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("Combinem as vezes de brincar, representem uma cena e contem como Lia e Davi se sentiram.", Modifier.padding(top = 10.dp), fontSize = 18.sp)
                    Text("Não existe uma única emoção certa. O professor escuta como o grupo chegou à resposta.", Modifier.padding(top = 8.dp))
                }
                ComicButton("OUVIR A MISSÃO DO GRUPO", { speak(narration) }, color = ComicYellow, leading = "🔊")
                GuidedComicButton("CONCLUÍMOS COM A TURMA", {
                    onCompleted(path)
                    mode = "menu"
                }, color = ComicGreen, trailing = "✓", cue = "DEPOIS DA CONVERSA")
            }
            else -> {
                Pill(
                    if (mode == "gallery") "CENA EXPRESSIVA ${page + 1} DE ${scenes.size}"
                    else "LER E INTERPRETAR • CENA ${page + 1} DE ${scenes.size}",
                    ComicYellow
                )
                ComicPanel {
                    Text(scene.title, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(12.dp))
                    ComicPortrait(page, scene.imageDescription)
                    Spacer(Modifier.height(12.dp))
                    scene.dialogue.forEachIndexed { index, line ->
                        SpeechBubble(
                            speaker = line.speaker,
                            text = line.text,
                            color = if (index % 2 == 0) SoftBlue else SoftGreen
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                Text(scene.question, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                if (selected < 0) AttentionCue("ESCOLHA UMA RESPOSTA")
                scene.choices.forEachIndexed { index, choice ->
                    ComicButton(choice.label, {
                        selected = index
                        if (mode == "story" && storyChoices[page] != index) {
                            storyChoices = storyChoices.toMutableList().also { it[page] = index }
                            onSceneAnswered(page, index)
                        }
                        speak(choice.reply)
                    }, color = if (selected == index) ComicYellow else Color.White, leading = choice.icon)
                }
                scene.choices.getOrNull(selected)?.let { choice ->
                    ComicPanel(color = SoftGreen) {
                        Text(choice.reply, fontSize = 18.sp)
                        ComicButton("OUVIR A CONVERSA", { speak(choice.reply) }, color = ComicYellow, leading = "🔊")
                    }
                }
                GuidedComicButton(
                    text = if (mode == "gallery") "ESCOLHER OUTRA CENA"
                        else if (page < scenes.lastIndex) "PRÓXIMO QUADRINHO" else "ESCREVER NOSSO BILHETE",
                    onClick = {
                        if (mode == "gallery") {
                            mode = "galleryMenu"
                            selected = -1
                        } else if (page < scenes.lastIndex) {
                            page++
                            selected = storyChoices[page]
                        } else {
                            mode = "write"
                        }
                    },
                    color = ComicBlue,
                    enabled = selected >= 0,
                    trailing = "→"
                )
                if (page > 0 && mode == "story") {
                    ComicButton("QUADRINHO ANTERIOR", {
                        page--
                        selected = storyChoices[page]
                    }, color = Color.White)
                }
            }
        }
    }
}
