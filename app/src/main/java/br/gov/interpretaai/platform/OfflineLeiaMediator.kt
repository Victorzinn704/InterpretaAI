package br.gov.interpretaai.platform

/** Respostas aprovadas que mantêm a atividade útil sem rede ou IA. */
object OfflineLeiaMediator {
    private val prompts = mapOf(
        "gallery-1" to "Sua ideia pode ajudar Lia. O que Davi pode fazer para procurar junto?",
        "gallery-2" to "Os dois querem brincar. Como eles podem combinar as vezes com calma?",
        "gallery-3" to "Olha quem está no gol! O que Alfa está fazendo com a bola?",
        "gallery-4" to "Lia e Davi voltaram a brincar juntos. O que ajudou essa mudança?",
        "gallery-5" to "A imagem mostra vários caminhos. Como Davi disse que chegou à escola?"
    )

    fun reply(sceneId: String, turn: Int): VoiceTurnResult {
        val text = if (turn >= 3) {
            "Sua ideia ajudou a LÉIA! Vamos ver o que acontece agora?"
        } else {
            prompts[sceneId] ?: "A internet sumiu por um instante, mas eu continuo aqui. O que você percebe nessa cena?"
        }
        return VoiceTurnResult(replyText = text, degraded = true)
    }
}
