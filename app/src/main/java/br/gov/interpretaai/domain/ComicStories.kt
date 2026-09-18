package br.gov.interpretaai.domain

enum class Expression { SAD, ANGRY, LAUGHING, HAPPY }

data class ComicLine(val speaker: String, val text: String)

data class ComicChoice(
    val icon: String,
    val label: String,
    val reply: String,
    val pathSummary: String
)

data class ComicScene(
    val title: String,
    val expression: Expression,
    val imageDescription: String,
    val dialogue: List<ComicLine>,
    val question: String,
    val choices: List<ComicChoice>
) {
    val narration: String
        get() = buildString {
            append("$title. ")
            dialogue.forEach { append("${it.speaker} diz: ${it.text} ") }
            append(question)
        }.trim()
}

object ComicStories {
    val scenes = listOf(
        ComicScene(
            title = "A brincadeira parou",
            expression = Expression.SAD,
            imageDescription = "Lia está triste no pátio; Davi oferece ajuda e um objeto redondo aparece parcialmente atrás da árvore.",
            dialogue = listOf(
                ComicLine("Lia", "Eu queria brincar, mas não encontro o que preciso."),
                ComicLine("Davi", "Quer que eu procure com você?")
            ),
            question = "O que pode ajudar Lia nessa cena?",
            choices = listOf(
                ComicChoice("🤝", "Procurar juntos", "Boa ideia! Davi e Lia podem procurar a bola juntos.", "decidiu procurar a bola junto com Lia"),
                ComicChoice("💬", "Perguntar como ela está", "Boa ideia! Davi pode ouvir a Lia antes de começar a busca.", "primeiro escutou como Lia estava se sentindo")
            )
        ),
        ComicScene(
            title = "Um chute e uma conversa",
            expression = Expression.ANGRY,
            imageDescription = "Davi demonstra raiva ao lado da bola; Lia conversa com calma no pátio.",
            dialogue = listOf(
                ComicLine("Davi", "Eu ainda estava jogando! Você pegou a bola!"),
                ComicLine("Lia", "Eu achei que fosse minha vez.")
            ),
            question = "Como os dois podem combinar a brincadeira?",
            choices = listOf(
                ComicChoice("🔄", "Combinar as vezes", "Isso pode funcionar: uma vez para cada um!", "combinou uma vez para cada criança"),
                ComicChoice("🙋", "Pedir ajuda a um adulto", "Boa saída! Um adulto pode ajudar os dois a combinar a brincadeira.", "pediu ajuda para organizar as vezes")
            )
        ),
        ComicScene(
            title = "Alfa virou goleiro",
            expression = Expression.LAUGHING,
            imageDescription = "Lia e Davi riem porque Alfa dormiu abraçado à bola dentro do gol.",
            dialogue = listOf(
                ComicLine("Lia", "Nosso goleiro chegou!"),
                ComicLine("Davi", "Esse goleiro quer tirar uma soneca!")
            ),
            question = "Por que a cena é engraçada?",
            choices = listOf(
                ComicChoice("🐶", "Alfa virou goleiro", "Isso! Alfa virou goleiro, mas resolveu cochilar com a bola. Que surpresa!", "percebeu que Alfa virou um goleiro dorminhoco"),
                ComicChoice("⚽", "A bola está parada", "A bola está parada mesmo. E olha quem dormiu com ela: o Alfa!", "observou a bola e depois encontrou Alfa no gol")
            )
        ),
        ComicScene(
            title = "Juntos de novo",
            expression = Expression.HAPPY,
            imageDescription = "Lia e Davi sorriem enquanto passam a bola um para o outro.",
            dialogue = listOf(
                ComicLine("Lia", "Conseguimos brincar juntos!"),
                ComicLine("Davi", "E todo mundo teve sua vez!")
            ),
            question = "O que mudou desde o começo da história?",
            choices = listOf(
                ComicChoice("🤝", "Eles se ajudaram", "Isso! Eles procuraram a bola juntos e voltaram a brincar.", "entendeu que os amigos se ajudaram"),
                ComicChoice("💬", "Eles conversaram", "Isso! Quando conversaram, os dois conseguiram se entender.", "entendeu que conversar resolveu o conflito")
            )
        ),
        ComicScene(
            title = "Cada um chega de um jeito",
            expression = Expression.HAPPY,
            imageDescription = "Lia chega andando com a avó; Davi desce do ônibus com o pai e outra criança passa de bicicleta.",
            dialogue = listOf(
                ComicLine("Lia", "Vim andando com minha avó."),
                ComicLine("Davi", "Eu vim de ônibus com meu pai.")
            ),
            question = "Como Davi contou que chegou?",
            choices = listOf(
                ComicChoice("🚌", "De ônibus", "Isso! Davi contou que veio de ônibus. E você, como chega à escola?", "escutou que Davi chegou de ônibus"),
                ComicChoice("🚲", "De bicicleta", "A bicicleta aparece na cena, mas escute o Davi: ele veio de ônibus.", "comparou a bicicleta da imagem com o ônibus falado por Davi")
            )
        )
    )
}
