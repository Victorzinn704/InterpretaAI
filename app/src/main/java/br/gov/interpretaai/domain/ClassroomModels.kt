package br.gov.interpretaai.domain

enum class AssignedActivity(
    val label: String,
    val emoji: String,
    val supportedYears: IntRange,
    val supportRange: String,
    val pedagogicalFocus: String,
    val teacherEvidence: String,
    val bnccReferences: String,
    val readingPack: ReadingMissionPack? = null
) {
    COMIC(
        "Mistério da bola", "📖", 1..3, "1º–3º • com mediação",
        "escuta, informação explícita e inferência por pista",
        "localiza o que falta e explica onde procuraria",
        "EF15LP03 • EF15LP14 • EF35LP04"
    ),
    PUZZLE(
        "Quebra-cabeça", "🧩", 1..2, "1º–2º • consolidação",
        "imagem, palavra, sílabas e som inicial",
        "relaciona a figura montada à palavra falada",
        "EF01LP06 • EF01LP08 • EF02LP04"
    ),
    DRAWING(
        "Quadro criativo", "🎨", 1..5, "1º–5º • autoria mediada",
        "vocabulário, representação visual e explicação oral",
        "desenha a partir da pista e conta o que representou",
        "EF15LP09 • EF15LP10 • apoio à produção"
    ),
    SOUND_M(
        "Missão do som M", "🎤", 1..2, "1º–2º • alfabetização",
        "som inicial e relação entre fonema e grafema",
        "encontra uma palavra iniciada pelo som solicitado",
        "EF01LP07 • EF01LP08 • EF02LP06"
    ),
    STORY_SEQUENCE_2(
        "Antes e depois", "🪴", 2..2, "2º • sequência narrativa",
        "ordem de acontecimentos e marcadores antes/depois",
        "reorganiza duas ações e reconta a sequência ao grupo",
        "EF15LP03 • EF15LP18 • EF02LP26",
        ReadingMissionPack.STORY_SEQUENCE
    ),
    CAUSE_AND_EFFECT_3(
        "Causa e consequência", "🌧️", 3..3, "3º • inferência mediada",
        "relação entre acontecimento, causa e consequência",
        "localiza a causa e justifica oralmente o resultado",
        "EF15LP03 • EF35LP04 • EF35LP26",
        ReadingMissionPack.CAUSE_AND_EFFECT
    ),
    FACT_OR_OPINION_4(
        "Fato ou opinião?", "📰", 4..4, "4º • leitura crítica",
        "fatos, participantes, lugar, tempo e opinião",
        "distingue informação verificável de avaliação e explica a pista",
        "EF04LP14 • EF04LP15 • EF35LP15",
        ReadingMissionPack.FACT_OR_OPINION
    ),
    COMPARE_SOURCES_5(
        "Duas fontes", "🔎", 5..5, "5º • leitura crítica",
        "comparação de informações, fonte e argumento oral",
        "compara versões, aponta evidências e justifica a confiança",
        "EF05LP15 • EF05LP16 • EF05LP19",
        ReadingMissionPack.COMPARE_SOURCES
    );

    /** Identificador fechado usado nos eventos; nunca contém texto livre do professor ou da criança. */
    val eventId: String
        get() = when (this) {
            COMIC -> "gibi-bola-amigos"
            PUZZLE -> "quebra-cabeca-palavras"
            DRAWING -> "quadro-criativo"
            SOUND_M -> "missao-letra-m"
            else -> "leitura-${name.lowercase().replace('_', '-')}"
        }

    val completesOnReadingClosure: Boolean get() = readingPack != null

    fun supportsYear(year: Int): Boolean = year in supportedYears
}

data class LearnerAvatar(val id: String, val label: String, val emoji: String)

data class AssignedLearner(val learnerAlias: String, val avatar: LearnerAvatar) {
    init {
        require(learnerAlias.matches(Regex("(sol|pipa|estrela|foguete)-[0-9]{2,3}")))
        require(learnerAlias.startsWith("${avatar.id}-"))
    }
}

enum class CollaborativeMoment {
    OBSERVE,
    RESPOND,
    BUILD,
    CREATE,
    SHARE
}

data class CollaborativeTurn(
    val lead: AssignedLearner,
    val supporters: List<AssignedLearner>,
    val leadAction: String,
    val supportAction: String
) {
    val visiblePrompt: String
        get() = "${lead.avatar.emoji} $leadAction • ${supporters.joinToString("") { it.avatar.emoji }} $supportAction"

    val spokenPrompt: String
        get() = "Agora, ${lead.avatar.label} $leadAction. Os outros avatares $supportAction."
}

object CollaborativeTurnPlanner {
    fun turn(members: List<AssignedLearner>, moment: CollaborativeMoment): CollaborativeTurn? {
        if (members.size !in 2..4) return null
        val leadIndex = moment.ordinal % members.size
        val actions = when (moment) {
            CollaborativeMoment.OBSERVE -> "procura pistas" to "ouvem e ajudam"
            CollaborativeMoment.RESPOND -> "conta a ideia" to "completam a resposta"
            CollaborativeMoment.BUILD -> "move as peças" to "conferem a figura"
            CollaborativeMoment.CREATE -> "faz o primeiro traço" to "sugerem detalhes"
            CollaborativeMoment.SHARE -> "explica a descoberta" to "acrescentam uma pista"
        }
        return CollaborativeTurn(
            lead = members[leadIndex],
            supporters = members.filterIndexed { index, _ -> index != leadIndex },
            leadAction = actions.first,
            supportAction = actions.second
        )
    }
}

object LearnerAvatars {
    val available = listOf(
        LearnerAvatar("sol", "Sol", "☀️"),
        LearnerAvatar("pipa", "Pipa", "🪁"),
        LearnerAvatar("estrela", "Estrela", "⭐"),
        LearnerAvatar("foguete", "Foguete", "🚀")
    )

    fun find(id: String?) = available.firstOrNull { it.id == id } ?: available.first()
}

data class ClassroomAssignment(
    val classroomLabel: String,
    val avatar: LearnerAvatar,
    val activity: AssignedActivity,
    val drawingPrompt: DrawingPrompt,
    val learnerAlias: String,
    val members: List<AssignedLearner> = listOf(AssignedLearner(learnerAlias, avatar))
) {
    init {
        require(classroomLabel.isNotBlank() && classroomLabel.length <= 30)
        require(learnerAlias.matches(Regex("(sol|pipa|estrela|foguete)-[0-9]{2,3}")))
        require(learnerAlias.startsWith("${avatar.id}-"))
        require(members.isNotEmpty() && members.size <= 4)
        require(members.first() == AssignedLearner(learnerAlias, avatar))
        require(members.distinctBy { it.learnerAlias }.size == members.size)
    }

    val isSharedTablet: Boolean get() = members.size > 1
}

data class PilotRoomParticipant(
    val learnerAlias: String,
    val avatar: LearnerAvatar,
    val deviceId: String
) {
    init {
        require(learnerAlias.matches(Regex("(sol|pipa|estrela|foguete)-[0-9]{2,3}")))
        require(learnerAlias.startsWith("${avatar.id}-"))
        require(deviceId.matches(Regex("[a-zA-Z0-9_-]{6,64}")))
    }
}
