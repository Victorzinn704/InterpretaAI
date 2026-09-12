package br.gov.interpretaai.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionEvaluatorTest {
    @Test fun acceptsPortugueseWordsStartingWithM() {
        assertTrue(MissionEvaluator.startsWithLetterM("Eu encontrei uma maçã"))
        assertTrue(MissionEvaluator.startsWithLetterM("MOCHILA"))
    }

    @Test fun rejectsAnswerWithoutMWord() {
        assertFalse(MissionEvaluator.startsWithLetterM("achei um abacaxi"))
        assertFalse(MissionEvaluator.startsWithLetterM(""))
    }
}
