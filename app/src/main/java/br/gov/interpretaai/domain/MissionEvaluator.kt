package br.gov.interpretaai.domain

import java.text.Normalizer

object MissionEvaluator {
    fun startsWithLetterM(spokenText: String): Boolean {
        val normalized = Normalizer.normalize(spokenText.trim(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .lowercase()
        return normalized.split(" ", "-", ",", ".")
            .filter { it.isNotBlank() }
            .any { it.startsWith("m") }
    }
}
