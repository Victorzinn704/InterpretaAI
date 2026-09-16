package br.gov.interpretaai.domain

enum class DrawingPrompt(val label: String, val emoji: String) {
    BALL("bola", "⚽"), APPLE("maçã", "🍎"), HOUSE("casa", "🏠"), TREE("árvore", "🌳")
}

enum class DrawingTool { BRUSH, ERASER }

data class DrawingPoint(val x: Float, val y: Float)

data class DrawingStroke(
    val points: List<DrawingPoint>,
    val color: Long,
    val width: Float,
    val tool: DrawingTool = DrawingTool.BRUSH
)

/** Pilhas LIFO pequenas e testáveis; a UI apenas desenha o estado resultante. */
class DrawingHistory(private val limit: Int = 80) {
    private val done = ArrayDeque<DrawingStroke>()
    private val undone = ArrayDeque<DrawingStroke>()

    init { require(limit > 0) }

    val strokes: List<DrawingStroke> get() = done.toList()
    val canUndo: Boolean get() = done.isNotEmpty()
    val canRedo: Boolean get() = undone.isNotEmpty()

    fun add(stroke: DrawingStroke) {
        if (stroke.points.isEmpty()) return
        if (done.size == limit) done.removeFirst()
        done.addLast(stroke)
        undone.clear()
    }

    fun undo() {
        if (done.isNotEmpty()) undone.addLast(done.removeLast())
    }

    fun redo() {
        if (undone.isNotEmpty()) done.addLast(undone.removeLast())
    }

    fun clear() {
        done.clear()
        undone.clear()
    }
}
