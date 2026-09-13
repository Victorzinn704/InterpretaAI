package br.gov.interpretaai.platform

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.Closeable
import java.text.Normalizer
import java.util.Locale

data class LocalVisionResult(val words: List<String>, val objects: List<String>) {
    fun firstStartingWith(letter: Char): String? {
        val expected = normalize(letter.toString())
        return (words + objects).firstOrNull { normalize(it).startsWith(expected) }
    }

    fun bestDescription(): String? = (objects + words).firstOrNull()

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
        .lowercase(Locale.ROOT)
}

class LocalVisionRecognizer : Closeable {
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val imageLabeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder().setConfidenceThreshold(0.55f).build()
    )

    fun analyze(context: Context, uri: Uri, onResult: (Result<LocalVisionResult>) -> Unit) {
        val image = runCatching { InputImage.fromFilePath(context, uri) }
            .getOrElse { onResult(Result.failure(it)); return }

        textRecognizer.process(image).addOnCompleteListener { textTask ->
            imageLabeler.process(image).addOnCompleteListener { labelTask ->
                if (!textTask.isSuccessful && !labelTask.isSuccessful) {
                    onResult(Result.failure(labelTask.exception ?: textTask.exception ?: IllegalStateException("vision_failed")))
                    return@addOnCompleteListener
                }
                val words = textTask.result?.text.orEmpty()
                    .split(Regex("[^\\p{L}]+"))
                    .filter { it.length > 1 }
                    .distinct()
                val objects = labelTask.result.orEmpty()
                    .sortedByDescending { it.confidence }
                    .mapNotNull { LABELS_PT_BR[it.text.lowercase(Locale.ROOT)] }
                    .distinct()
                onResult(Result.success(LocalVisionResult(words, objects)))
            }
        }
    }

    override fun close() {
        textRecognizer.close()
        imageLabeler.close()
    }

    private companion object {
        val LABELS_PT_BR = mapOf(
            "apple" to "maçã",
            "banana" to "banana",
            "ball" to "bola",
            "soccer ball" to "bola",
            "car" to "carro",
            "motorcycle" to "moto",
            "milk" to "leite",
            "food" to "comida",
            "fruit" to "fruta",
            "orange" to "laranja",
            "grape" to "uva",
            "book" to "livro",
            "cat" to "gato",
            "dog" to "cachorro",
            "flower" to "flor",
            "table" to "mesa"
        )
    }
}
