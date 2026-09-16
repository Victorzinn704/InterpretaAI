package br.gov.interpretaai.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawingHistoryTest {
    private fun stroke(id: Float) = DrawingStroke(listOf(DrawingPoint(id, id)), id.toLong(), 8f)

    @Test fun undoAndRedoFollowLifoOrder() {
        val history = DrawingHistory()
        history.add(stroke(1f)); history.add(stroke(2f)); history.undo()
        assertEquals(listOf(stroke(1f)), history.strokes)
        assertTrue(history.canRedo)
        history.redo()
        assertEquals(listOf(stroke(1f), stroke(2f)), history.strokes)
    }

    @Test fun newStrokeInvalidatesRedoAndLimitDropsOldest() {
        val history = DrawingHistory(limit = 2)
        history.add(stroke(1f)); history.add(stroke(2f)); history.undo(); history.add(stroke(3f)); history.add(stroke(4f))
        assertEquals(listOf(stroke(3f), stroke(4f)), history.strokes)
        assertFalse(history.canRedo)
    }

    @Test fun eraserGestureParticipatesInUndoAndRedo() {
        val history = DrawingHistory()
        val eraser = DrawingStroke(
            listOf(DrawingPoint(4f, 4f), DrawingPoint(8f, 8f)),
            0L,
            40f,
            DrawingTool.ERASER
        )

        history.add(stroke(1f))
        history.add(eraser)
        history.undo()
        assertEquals(DrawingTool.BRUSH, history.strokes.last().tool)
        history.redo()
        assertEquals(DrawingTool.ERASER, history.strokes.last().tool)
    }
}
