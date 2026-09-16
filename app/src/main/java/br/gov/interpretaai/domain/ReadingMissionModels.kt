package br.gov.interpretaai.domain

/** Conteúdo curto, revisável e determinístico; não é gerado pelo modelo durante a aula. */
enum class ReadingMissionPack(
    val title: String,
    val stageLabel: String,
    val intro: String,
    val texts: List<MissionText>,
    val question: String,
    val choices: List<String>,
    private val evidenceChoice: Int,
    val evidenceReply: String,
    val reflectionReply: String,
    val groupPrompt: String
) {
    FACT_OR_OPINION(
        title = "Notícia do recreio",
        stageLabel = "4º ANO • FATO E OPINIÃO",
        intro = "Duas frases contam o mesmo recreio. Ouça e descubra qual delas pode ser conferida.",
        texts = listOf(
            MissionText("NOTÍCIA", "Às dez horas, a chuva molhou a quadra. A turma brincou no pátio coberto."),
            MissionText("DAVI PENSA", "Foi o recreio mais sem graça do mundo!")
        ),
        question = "Qual frase apresenta uma informação que pode ser conferida?",
        choices = listOf("A QUADRA MOLHOU ÀS 10H", "FOI O PIOR RECREIO"),
        evidenceChoice = 0,
        evidenceReply = "Boa investigação! Horário, lugar e acontecimento podem ser conferidos.",
        reflectionReply = "Essa frase mostra a opinião de Davi. Agora procure a que informa horário, lugar e acontecimento.",
        groupPrompt = "Conte ao grupo: como você diferencia uma informação de uma opinião?"
    ),
    COMPARE_SOURCES(
        title = "Duas fontes, um plantio",
        stageLabel = "5º ANO • COMPARE AS FONTES",
        intro = "Dois textos falam do plantio da escola. Compare as pistas antes de escolher.",
        texts = listOf(
            MissionText("JORNAL DA ESCOLA", "Na terça-feira, dezoito estudantes plantaram doze mudas. A professora Ana acompanhou a ação."),
            MissionText("MENSAGEM", "Foi o maior plantio da cidade! Todo mundo adorou!")
        ),
        question = "Qual texto oferece mais pistas para conferir o que aconteceu?",
        choices = listOf("JORNAL DA ESCOLA", "MENSAGEM"),
        evidenceChoice = 0,
        evidenceReply = "Você encontrou uma fonte com data, quantidade e responsável. Essas pistas ajudam a conferir.",
        reflectionReply = "A mensagem traz uma avaliação, mas não mostra como conferir. Compare data, quantidade e responsável.",
        groupPrompt = "Conte ao grupo: qual detalhe tornou a fonte mais confiável para você?"
    );

    fun replyFor(choice: Int) = if (choice == evidenceChoice) evidenceReply else reflectionReply
    fun carriesEvidence(choice: Int) = choice == evidenceChoice
}

data class MissionText(val source: String, val text: String)
