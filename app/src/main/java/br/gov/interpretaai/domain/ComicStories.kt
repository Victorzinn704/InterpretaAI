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
            append("$question ")
            choices.forEachIndexed { index, choice -> append("Opção ${index + 1}: ${choice.label}. ") }
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
                ComicChoice("🤝", "Procurar juntos", "Uma companhia pode ajudar! Davi e Lia procuram juntos e encontram a bola atrás da árvore.", "decidiu procurar a bola junto com Lia"),
                ComicChoice("💬", "Perguntar como ela está", "Escutar também é uma forma de cuidar. Lia conta o que aconteceu e pede companhia para procurar a bola.", "primeiro escutou como Lia estava se sentindo")
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
                ComicChoice("🔄", "Combinar as vezes", "Eles combinam uma vez para cada um. Davi pode sentir raiva e ainda conversar sem machucar ninguém.", "combinou uma vez para cada criança"),
                ComicChoice("🙋", "Pedir ajuda a um adulto", "Eles chamam o professor para ajudar a combinar as vezes. Pedir ajuda também faz parte da brincadeira.", "pediu ajuda para organizar as vezes")
            )
        ),
        ComicScene(
            title = "O goleiro diferente",
            expression = Expression.LAUGHING,
            imageDescription = "Lia e Davi riem porque um cachorro dormiu abraçado à bola dentro do gol.",
            dialogue = listOf(
                ComicLine("Lia", "Nosso goleiro chegou!"),
                ComicLine("Davi", "Esse goleiro quer tirar uma soneca!")
            ),
            question = "Por que a cena é engraçada?",
            choices = listOf(
                ComicChoice("🐶", "O goleiro é um cachorro", "Isso! Eles chamaram o cachorro de goleiro, mas ele quer dormir. A surpresa faz a graça do quadrinho.", "percebeu que o goleiro era um cachorro dorminhoco"),
                ComicChoice("⚽", "A bola está parada", "A bola está parada mesmo. Olhe também para o goleiro: é um cachorro dorminhoco! A surpresa faz a graça da cena.", "observou a bola e depois descobriu o cachorro dorminhoco")
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
                ComicChoice("🤝", "Eles se ajudaram", "Eles procuraram a bola, conversaram e dividiram as vezes. O sorriso acompanha esse reencontro.", "entendeu que os amigos se ajudaram"),
                ComicChoice("💬", "Eles conversaram", "Conversar ajudou a entender o que cada um queria. Nem sempre sentimos igual; podemos perguntar e escutar.", "entendeu que conversar resolveu o conflito")
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
                ComicChoice("🚌", "De ônibus", "Davi contou que veio de ônibus. Agora contem ao grupo: como vocês costumam chegar à escola?", "escutou que Davi chegou de ônibus"),
                ComicChoice("🚲", "De bicicleta", "Há uma bicicleta na imagem! Mas Davi falou em ônibus. Ouça de novo e procure a diferença entre o diálogo e a cena.", "comparou a bicicleta da imagem com o ônibus falado por Davi")
            )
        )
    )
}
