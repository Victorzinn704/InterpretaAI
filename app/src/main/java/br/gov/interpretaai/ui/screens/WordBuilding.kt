package br.gov.interpretaai.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.gov.interpretaai.ui.*
import br.gov.interpretaai.ui.theme.*

@Composable
fun WordBuilding(speak: (String) -> Unit, onDone: () -> Unit) {
    var answer by rememberSaveable { mutableStateOf("") }
    val complete = answer == "BOLA"
    ComicPanel {
        Text("ESCREVER • Convite para brincar", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text("⚽  Traga a ____ para brincar!", fontSize = 20.sp)
        Text(answer.padEnd(4, '＿').toCharArray().joinToString(" "),
            Modifier.padding(vertical = 8.dp), fontSize = 32.sp, fontWeight = FontWeight.Black)
        if (!complete) AttentionCue("TOQUE NAS LETRAS")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf('A', 'L', 'B', 'O').forEach { letter ->
                ComicButton(letter.toString(), {
                    answer += letter
                    val name = when (letter) { 'B' -> "bê"; 'L' -> "ele"; 'O' -> "ó"; else -> "á" }
                    speak(if (answer == "BOLA") "Você montou bola! O bilhete diz: Traga a bola para brincar!"
                        else if (answer.length == 4) "$name. Vamos conferir com a palavra bola? Você pode ouvir a pista e montar de novo."
                        else name)
                }, Modifier.weight(1f), color = ComicYellow,
                    enabled = answer.length < 4 && letter !in answer)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ComicButton("PISTA", {
            speak("Bola. Para montar essa palavra, procure as letras: bê, ó, ele, á.")
        }, Modifier.weight(1f), color = Color.White, leading = "🔊")
        ComicButton("RECOMEÇAR", { answer = ""; speak("Vamos montar de novo. Bola.") }, Modifier.weight(1f), color = Color.White)
        }
    }
    GuidedComicButton(
        "APLICAR COM A TURMA",
        onDone,
        color = ComicGreen,
        enabled = complete,
        trailing = "→",
        cue = "CONVERSE COM O GRUPO"
    )
}
