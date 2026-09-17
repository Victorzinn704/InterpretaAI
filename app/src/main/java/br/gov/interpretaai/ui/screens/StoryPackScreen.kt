package br.gov.interpretaai.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.interpretaai.domain.ComicStoryNode
import br.gov.interpretaai.domain.EndStoryNode
import br.gov.interpretaai.domain.GroupHandoffStoryNode
import br.gov.interpretaai.domain.PuzzleGame
import br.gov.interpretaai.domain.PuzzleSize
import br.gov.interpretaai.domain.PuzzleStoryNode
import br.gov.interpretaai.domain.ResponseModality
import br.gov.interpretaai.domain.StoryNode
import br.gov.interpretaai.domain.WordBuilderStoryNode
import br.gov.interpretaai.R
import br.gov.interpretaai.platform.storycache.PreparedAssignedStory
import br.gov.interpretaai.ui.AttentionCue
import br.gov.interpretaai.ui.ChildStageScaffold
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.ComicPanel
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.Pill
import br.gov.interpretaai.ui.StageHeader
import br.gov.interpretaai.ui.rememberReengagementVisual
import br.gov.interpretaai.ui.theme.ComicBlue
import br.gov.interpretaai.ui.theme.ComicGreen
import br.gov.interpretaai.ui.theme.ComicInk
import br.gov.interpretaai.ui.theme.ComicYellow
import br.gov.interpretaai.ui.theme.SoftBlue
import br.gov.interpretaai.ui.theme.SoftGreen
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Executes only validated, locally prepared nodes. Content never supplies routes or UI code. */
@Composable
fun StoryPackScreen(
    story: PreparedAssignedStory,
    speak: (String) -> Unit,
    listen: ((String) -> Unit) -> Unit,
    isListening: Boolean,
    isSpeaking: Boolean,
    reducedStimuli: Boolean,
    onBack: () -> Unit,
    onHelpRequested: (String) -> Unit,
    onVoiceContribution: (String) -> Unit,
    onStageCompleted: (String, ResponseModality) -> Unit,
    onCompleted: () -> Unit
) {
    val pack = story.pack
    var nodeId by rememberSaveable(pack.packId) { mutableStateOf(pack.startNodeId) }
    val node = pack.nodes.firstOrNull { it.id == nodeId } ?: return
    var dialogueIndex by rememberSaveable(nodeId) { mutableIntStateOf(0) }
    var ideaHeard by rememberSaveable(nodeId) { mutableStateOf(false) }
    var interactionNonce by rememberSaveable(nodeId) { mutableIntStateOf(0) }
    val visualAssetId = when (node) {
        is ComicStoryNode -> node.visualAssetId
        is PuzzleStoryNode -> node.imageAssetId
        is WordBuilderStoryNode -> node.imageAssetId
        else -> null
    }
    val image = rememberCachedImage(visualAssetId?.let(story.assets::get))
    val currentSpeak by rememberUpdatedState(speak)
    val spoken = when (node) {
        is ComicStoryNode -> {
            val line = node.dialogue[dialogueIndex.coerceIn(node.dialogue.indices)]
            (if (dialogueIndex == 0) "${node.altText} " else "") +
                "${speakerName(line.speaker)} diz: ${line.text}" +
                if (dialogueIndex == node.dialogue.lastIndex) node.prompt?.let { " $it" }.orEmpty() else ""
        }
        is PuzzleStoryNode -> node.instruction
        is WordBuilderStoryNode -> node.instruction
        is GroupHandoffStoryNode -> node.instruction
        is EndStoryNode -> node.closingSpeech
    }
    LaunchedEffect(pack.packId, nodeId, dialogueIndex, image) {
        if (visualAssetId == null || image != null) currentSpeak(spoken)
    }
    DisposableEffect(story.assignmentId) { onDispose { currentSpeak("") } }
    BackHandler(onBack = onBack)
    val cue = rememberReengagementVisual(
        stageKey = "${pack.packId}-$nodeId-$dialogueIndex",
        interactionNonce = interactionNonce,
        busy = isListening || isSpeaking || (visualAssetId != null && image == null),
        reducedStimuli = reducedStimuli,
        speak = speak,
        spokenPrompt = "A história espera sua ideia. Quer continuar comigo?"
    )
    fun advance(modality: ResponseModality = ResponseModality.TOUCH) {
        onStageCompleted(node.id, modality)
        node.nextNodeId?.let { nodeId = it }
    }
    val tablet = LocalConfiguration.current.screenWidthDp >= 600

    ChildStageScaffold { compact ->
        StageHeader(pack.title, "LÉIA • ${pack.version} • LEIA", onBack, { speak(spoken) })
        when (node) {
            is ComicStoryNode -> {
                Pill("LER • OBSERVE E OUÇA", ComicYellow)
                if (image != null) {
                    Image(
                        image, contentDescription = node.altText,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                            .border(3.dp, ComicInk, RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else MissingStoryImage(Modifier.weight(1f))
                if (image != null) {
                    val line = node.dialogue[dialogueIndex.coerceIn(node.dialogue.indices)]
                    ComicPanel(color = SoftBlue) {
                        Text(speakerName(line.speaker), fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text(line.text, fontSize = if (compact) 17.sp else 20.sp,
                            fontWeight = FontWeight.Bold, maxLines = 3,
                            overflow = TextOverflow.Ellipsis)
                    }
                    if (dialogueIndex == node.dialogue.lastIndex && node.prompt != null) {
                    if (cue) AttentionCue("CONTE SUA IDEIA")
                    Text(node.prompt, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (!ideaHeard) {
                        GuidedComicButton(if (isListening) "ESTOU OUVINDO" else "CONTAR MINHA IDEIA", {
                            interactionNonce++
                            listen {
                                ideaHeard = true
                                interactionNonce++
                                onVoiceContribution(node.id)
                                speak("Obrigado por contar sua ideia! Vamos ver o que acontece na história.")
                            }
                        }, color = ComicBlue, enabled = !isListening, leading = "🎤")
                        ComicButton("PENSAR E CONTINUAR", { interactionNonce++; advance() },
                            color = Color.White, leading = "💭")
                    } else GuidedComicButton("CONTINUAR A HISTÓRIA", { advance(ResponseModality.VOICE) },
                        color = ComicGreen, trailing = "→")
                    } else GuidedComicButton("PRÓXIMA FALA", {
                        interactionNonce++
                        if (dialogueIndex < node.dialogue.lastIndex) dialogueIndex++ else advance()
                    }, color = ComicBlue, trailing = "→")
                }
            }
            is PuzzleStoryNode -> {
                Pill("ENTENDER • MONTE A PISTA", ComicYellow)
                Text(node.instruction, fontSize = if (compact) 17.sp else 20.sp,
                    fontWeight = FontWeight.Bold, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                val size = if (node.grid == "3x2") PuzzleSize.CHALLENGE else PuzzleSize.EASY
                var tiles by rememberSaveable(nodeId) { mutableStateOf(PuzzleGame.initialTiles(size)) }
                var selected by rememberSaveable(nodeId) { mutableIntStateOf(-1) }
                val complete = PuzzleGame.isComplete(tiles)
                LaunchedEffect(complete) { if (complete && image != null) currentSpeak(node.completionSpeech) }
                if (image != null) StoryPuzzleBoard(image, size, tiles, selected,
                    Modifier.fillMaxWidth()
                        .height(if (compact) 310.dp else if (tablet) 880.dp else 570.dp),
                    onTileInteraction = { first, second ->
                        if (!complete) {
                            interactionNonce++
                            if (first != second && "DRAG" in node.interactionModes) {
                                tiles = PuzzleGame.swap(tiles, first, second)
                                selected = -1
                            } else if ("TAP_SWAP" in node.interactionModes) {
                                if (selected < 0 || selected == first) selected = first
                                else {
                                    tiles = PuzzleGame.swap(tiles, selected, first)
                                    selected = -1
                                }
                            }
                        }
                    }) else MissingStoryImage(Modifier.weight(1f))
                if (image == null) Unit
                else if (complete) GuidedComicButton("CONTINUAR A HISTÓRIA", { advance() },
                    color = ComicGreen, trailing = "→")
                else {
                    if (cue) AttentionCue("TOQUE OU ARRASTE UMA PEÇA")
                    ComicButton("OUVIR PISTA", {
                        interactionNonce++
                        onHelpRequested(node.id)
                        speak(node.supports.firstOrNull()?.spokenHint ?: node.instruction)
                    }, color = Color.White, leading = "🔊")
                }
            }
            is WordBuilderStoryNode -> {
                Pill("INTERPRETAR • FORME A PALAVRA", ComicYellow)
                Text(node.instruction, fontSize = if (compact) 17.sp else 20.sp,
                    fontWeight = FontWeight.Bold, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                if (image != null) Image(image, contentDescription = "Figura da palavra ${node.targetWord}",
                    modifier = Modifier.fillMaxWidth()
                        .height(if (compact) 120.dp else if (tablet) 650.dp else 300.dp),
                    contentScale = ContentScale.Fit)
                else MissingStoryImage(Modifier.weight(1f))
                var chosen by rememberSaveable(nodeId) { mutableStateOf(emptyList<Int>()) }
                val answer = chosen.joinToString("") { node.letterTiles[it] }
                val complete = answer.equals(node.targetWord, ignoreCase = true)
                LaunchedEffect(complete) { if (complete && image != null) currentSpeak(node.completionSpeech) }
                ComicPanel(color = SoftBlue) {
                    Text(if (answer.isEmpty()) List(node.targetWord.length) { "_" }.joinToString(" ")
                        else answer.toList().joinToString(" "),
                        fontSize = 30.sp, fontWeight = FontWeight.Black)
                }
                if (image != null && !complete && chosen.size < node.targetWord.length) {
                    node.letterTiles.chunked(4).forEachIndexed { rowIndex, row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            row.forEachIndexed { index, tile ->
                                val tileIndex = rowIndex * 4 + index
                                ComicButton(tile, {
                                    chosen = chosen + tileIndex
                                    interactionNonce++
                                    speak(if (chosen.size == 1 && node.initialPhonemeCue.isNotBlank()
                                        && tile.equals(node.targetWord.first().toString(), true))
                                        "${node.initialLetterName}. ${node.initialPhonemeCue}" else tile)
                                }, Modifier.weight(1f), color = ComicYellow,
                                    enabled = tileIndex !in chosen)
                            }
                        }
                    }
                }
                if (image != null) when {
                    complete -> GuidedComicButton("CONTINUAR A HISTÓRIA", {
                        advance()
                    }, color = ComicGreen, trailing = "→")
                    chosen.size >= node.targetWord.length -> GuidedComicButton("OUVIR E TENTAR DE NOVO", {
                        chosen = emptyList()
                        interactionNonce++
                        onHelpRequested(node.id)
                        speak(node.supports.firstOrNull()?.spokenHint ?: node.initialPhonemeCue)
                    }, color = ComicBlue, leading = "🔊")
                    else -> ComicButton("OUVIR O SOM", {
                        interactionNonce++
                        onHelpRequested(node.id)
                        speak(node.initialPhonemeCue.ifBlank { node.instruction })
                    }, color = Color.White, leading = "🔊")
                }
            }
            is GroupHandoffStoryNode -> {
                Pill("APRENDER • COM A DUPLA", ComicYellow)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Image(painterResource(R.drawable.leia_and_dog_v1),
                        contentDescription = "LÉIA e seu cachorro convidam a turma a conversar",
                        modifier = Modifier.fillMaxWidth().height(if (compact) 112.dp else if (tablet) 300.dp else 220.dp),
                        contentScale = ContentScale.Fit)
                    ComicPanel(color = SoftGreen) {
                        Text("📱  →  👫", fontSize = 48.sp)
                        Text("Agora o aparelho descansa.", fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Text(node.instruction, fontSize = 19.sp, maxLines = 4,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
                GuidedComicButton("CONTINUAR DEPOIS DA CONVERSA", { advance() },
                    color = ComicGreen, trailing = "→")
            }
            is EndStoryNode -> {
                Pill("MISSÃO CONCLUÍDA", ComicYellow)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Image(painterResource(R.drawable.leia_and_dog_v1),
                        contentDescription = "LÉIA e seu cachorro comemoram a história",
                        modifier = Modifier.fillMaxWidth().height(if (compact) 140.dp else if (tablet) 340.dp else 240.dp),
                        contentScale = ContentScale.Fit)
                    ComicPanel(color = SoftGreen) {
                        Text("🌟", fontSize = 58.sp)
                        Text(node.closingSpeech, fontSize = 22.sp, fontWeight = FontWeight.Black,
                            maxLines = 4, overflow = TextOverflow.Ellipsis)
                    }
                }
                GuidedComicButton("VOLTAR AO INÍCIO", onCompleted,
                    color = ComicGreen, leading = "🏠")
            }
        }
    }
}

private fun speakerName(value: String) = when (value) {
    "LEIA_TEACHER" -> "LÉIA"
    "DOG" -> "Cachorro"
    "NARRATOR" -> "Narradora"
    else -> "Personagem"
}

@Composable
private fun MissingStoryImage(modifier: Modifier = Modifier) {
    ComicPanel(modifier = modifier, color = SoftBlue) {
        Text("A imagem está sendo preparada neste aparelho.", fontSize = 17.sp)
    }
}

@Composable
private fun rememberCachedImage(file: File?): ImageBitmap? {
    var image by remember(file) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(file) {
        val decoded = withContext(Dispatchers.IO) {
            if (file == null) null else runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) null else {
                    var sample = 1
                    while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2
                    BitmapFactory.decodeFile(file.absolutePath,
                        BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
                }
            }.getOrNull()
        }
        image = decoded
    }
    return image
}

@Composable
private fun StoryPuzzleBoard(
    image: ImageBitmap,
    size: PuzzleSize,
    tiles: List<Int>,
    selected: Int,
    modifier: Modifier = Modifier,
    onTileInteraction: (Int, Int) -> Unit
) {
    val currentMove by rememberUpdatedState(onTileInteraction)
    Canvas(modifier.testTag("story-puzzle-board")
        .border(3.dp, ComicInk, RoundedCornerShape(18.dp))
        .pointerInput(size, tiles) {
            awaitEachGesture {
                val down = awaitFirstDown()
                var last = down.position
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    last = change.position
                    if (!change.pressed) break
                }
                fun index(point: Offset): Int {
                    val column = (point.x / this.size.width * size.columns).toInt()
                        .coerceIn(0, size.columns - 1)
                    val row = (point.y / this.size.height * size.rows).toInt()
                        .coerceIn(0, size.rows - 1)
                    return row * size.columns + column
                }
                currentMove(index(down.position), index(last))
            }
        }) {
        val cellWidth = this.size.width / size.columns
        val cellHeight = this.size.height / size.rows
        tiles.forEachIndexed { position, source ->
            val sourceWidth = image.width / size.columns
            val sourceHeight = image.height / size.rows
            val x = position % size.columns
            val y = position / size.columns
            drawImage(image,
                srcOffset = IntOffset(source % size.columns * sourceWidth,
                    source / size.columns * sourceHeight),
                srcSize = IntSize(sourceWidth, sourceHeight),
                dstOffset = IntOffset((x * cellWidth).toInt(), (y * cellHeight).toInt()),
                dstSize = IntSize(cellWidth.toInt() + 1, cellHeight.toInt() + 1))
            drawRect(if (position == selected) ComicYellow else ComicInk,
                topLeft = Offset(x * cellWidth, y * cellHeight),
                size = androidx.compose.ui.geometry.Size(cellWidth, cellHeight),
                style = Stroke(width = if (position == selected) 7f else 3f))
        }
    }
}
