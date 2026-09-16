package br.gov.interpretaai.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniGameRulesTest {
    @Test fun numberedActivitiesAdvanceOnlyInSequence() {
        assertTrue(MiniGameRules.acceptsNumber(0, 1, false))
        assertFalse(MiniGameRules.acceptsNumber(0, 3, false))
        assertTrue(MiniGameRules.acceptsNumber(3, 4, true))
        assertFalse(MiniGameRules.acceptsNumber(5, 5, true))
    }

    @Test fun pictureWordRequiresLettersInOrder() {
        assertTrue(MiniGameRules.acceptsLetter(0, 'B'))
        assertFalse(MiniGameRules.acceptsLetter(1, 'L'))
        assertTrue(MiniGameRules.acceptsLetter(3, 'A'))
        assertFalse(MiniGameRules.acceptsLetter(4, 'A'))
    }
}
