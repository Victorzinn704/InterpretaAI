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
    STORY_SEQUENCE(
        title = "A manhã da horta",
        stageLabel = "2º ANO • ORDEM DA HISTÓRIA",
        intro = "Duas ações contam como a turma começou um plantio. Ouça e descubra qual veio primeiro.",
        texts = listOf(
            MissionText("UMA AÇÃO", "A turma colocou as sementes na terra."),
            MissionText("OUTRA AÇÃO", "Lia preparou o vaso com terra.")
        ),
        question = "Qual ação aconteceu primeiro para o plantio começar?",
        choices = listOf("PREPARAR O VASO", "COLOCAR AS SEMENTES"),
        evidenceChoice = 0,
        evidenceReply = "Boa sequência! Primeiro Lia preparou o vaso; depois a turma colocou as sementes.",
        reflectionReply = "As sementes vieram depois. Escute as duas ações e procure o que preparou o plantio.",
        groupPrompt = "Conte ao grupo: o que aconteceu primeiro e o que aconteceu depois?"
    ),
    CAUSE_AND_EFFECT(
        title = "O recreio mudou",
        stageLabel = "3º ANO • CAUSA E CONSEQUÊNCIA",
        intro = "Duas pistas contam uma mudança no recreio. Ouça e descubra por que a turma saiu da quadra.",
        texts = listOf(
            MissionText("ACONTECEU PRIMEIRO", "A chuva começou e molhou a quadra."),
            MissionText("RESULTADO", "A turma levou a brincadeira para o pátio coberto.")
        ),
        question = "Qual pista explica por que a turma saiu da quadra?",
        choices = listOf("COMEÇOU A CHOVER", "A TURMA MUDOU DE LUGAR"),
        evidenceChoice = 0,
        evidenceReply = "Você encontrou a causa: a chuva molhou a quadra. Mudar de lugar foi a consequência.",
        reflectionReply = "Mudar de lugar foi o resultado. Agora procure o acontecimento que provocou essa mudança.",
        groupPrompt = "Conte ao grupo: qual foi a causa e qual foi a consequência?"
    ),
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

    val completion: ReadingMissionCompletion
        get() = when (this) {
            STORY_SEQUENCE -> ReadingMissionCompletion(
                "VOCÊ ORGANIZOU A HISTÓRIA!",
                "Você ouviu as ações, encontrou a ordem e recontou antes e depois.",
                "Agora o tablet descansa. A turma pode registrar a sequência com desenho ou escrita.",
                "Parabéns! Você organizou os acontecimentos e explicou a sequência ao grupo."
            )
            CAUSE_AND_EFFECT -> ReadingMissionCompletion(
                "VOCÊ CONECTOU CAUSA E RESULTADO!",
                "Você investigou o que aconteceu primeiro e explicou a mudança no recreio.",
                "Agora o tablet descansa. A turma pode criar outro exemplo de causa e consequência.",
                "Parabéns! Você conectou a causa ao resultado e justificou sua ideia."
            )
            FACT_OR_OPINION -> ReadingMissionCompletion(
                "VOCÊ INVESTIGOU A INFORMAÇÃO!",
                "Você comparou uma informação verificável com uma opinião e explicou a pista.",
                "Agora o tablet descansa. Procurem na sala outro exemplo de fato e opinião.",
                "Parabéns! Você usou pistas para diferenciar uma informação de uma opinião."
            )
            COMPARE_SOURCES -> ReadingMissionCompletion(
                "VOCÊ COMPAROU AS FONTES!",
                "Você procurou data, quantidade e responsável antes de justificar sua escolha.",
                "Agora o tablet descansa. A turma pode listar quais pistas tornam uma fonte verificável.",
                "Parabéns! Você comparou as fontes e sustentou sua escolha com evidências."
            )
        }
}

data class MissionText(val source: String, val text: String)

data class ReadingMissionCompletion(
    val title: String,
    val summary: String,
    val offScreenPrompt: String,
    val spokenCelebration: String
)
