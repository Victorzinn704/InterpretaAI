package br.gov.interpretaai.domain

enum class AssignedActivity(val label: String, val emoji: String) {
    COMIC("Mistério da bola", "📖"),
    PUZZLE("Quebra-cabeça", "🧩"),
    DRAWING("Quadro criativo", "🎨"),
    SOUND_M("Missão do som M", "🎤")
}

data class LearnerAvatar(val id: String, val label: String, val emoji: String)

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
    val drawingPrompt: DrawingPrompt
) {
    init { require(classroomLabel.isNotBlank() && classroomLabel.length <= 30) }
}
