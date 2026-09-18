package br.gov.interpretaai.domain

import br.gov.interpretaai.platform.OfflineLeiaMediator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChildLanguageAuditTest {
    private val systemWords = listOf(
        "opção 1", "opção 2", "processando", "resposta gerada", "modelo de linguagem",
        "inteligência artificial", "seu cachorro", "o cachorro", "um cachorro"
    )

    @Test
    fun comicSpeechNamesAlfaAndDoesNotReadInterfaceOrSystemLanguage() {
        val speech = ComicStories.scenes.joinToString(" ") { scene ->
            scene.narration + " " + scene.choices.joinToString(" ") { it.reply }
        }.lowercase()

        assertTrue(speech.contains("alfa"))
        systemWords.forEach { forbidden -> assertFalse("fala contém: $forbidden", speech.contains(forbidden)) }
    }

    @Test
    fun offlineMediatorStaysConversational() {
        val speech = (1..5).joinToString(" ") { index ->
            OfflineLeiaMediator.reply("gallery-$index", 1).replyText
        }.lowercase()

        systemWords.forEach { forbidden -> assertFalse("fallback contém: $forbidden", speech.contains(forbidden)) }
        assertFalse(speech.contains("resposta correta"))
        assertFalse(speech.contains("você errou"))
    }
}
