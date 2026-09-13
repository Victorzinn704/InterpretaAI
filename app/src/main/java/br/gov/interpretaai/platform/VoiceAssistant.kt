package br.gov.interpretaai.platform

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.media.MediaPlayer
import java.io.File
import java.util.Locale

class VoiceAssistant(
    context: Context,
    private val onListeningChanged: (Boolean) -> Unit,
    private val onVoiceUnavailable: (String) -> Unit
) : TextToSpeech.OnInitListener, RecognitionListener {
    private val appContext = context.applicationContext
    private val tts = TextToSpeech(appContext, this)
    private var recognizer: SpeechRecognizer? = null
    private var resultCallback: ((String) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null
    private var ready = false
    private var pendingSpeech: String? = null
    private var player: MediaPlayer? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val languageResult = tts.setLanguage(Locale("pt", "BR"))
            if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                languageResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                pendingSpeech = null
                onVoiceUnavailable("A voz em português precisa ser instalada por um adulto neste aparelho.")
                return
            }
            val preferredVoice = tts.voices
                ?.filter { it.locale.toLanguageTag().equals("pt-BR", ignoreCase = true) }
                ?.sortedWith(
                    compareBy<android.speech.tts.Voice> { it.isNetworkConnectionRequired }
                        .thenByDescending { it.quality }
                )
                ?.firstOrNull()
            preferredVoice?.let { tts.setVoice(it) }
            tts.setSpeechRate(0.94f)
            tts.setPitch(1.04f)
            ready = true
            pendingSpeech?.let { speak(it) }
            pendingSpeech = null
        } else {
            pendingSpeech = null
            onVoiceUnavailable("A narração não iniciou neste aparelho. Verifique o mecanismo de texto para fala.")
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) { pendingSpeech = null; tts.stop(); return }
        if (!ready) { pendingSpeech = text; return }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "interpreta-${System.nanoTime()}")
    }

    fun playCloudAudio(audio: ByteArray, onFallback: () -> Unit) {
        runCatching {
            player?.release()
            val file = File.createTempFile("leia-voice-", ".ogg", appContext.cacheDir)
            file.writeBytes(audio)
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { completed -> completed.release(); file.delete(); player = null }
                setOnErrorListener { failed, _, _ -> failed.release(); file.delete(); player = null; onFallback(); true }
                prepare()
                start()
            }
        }.onFailure { onFallback() }
    }

    fun listen(onResult: (String) -> Unit, onError: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onError("Reconhecimento de voz indisponível neste aparelho")
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(appContext).also { it.setRecognitionListener(this) }
        resultCallback = onResult
        errorCallback = onError
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        recognizer?.startListening(intent)
    }

    fun release() {
        recognizer?.destroy()
        player?.release()
        tts.stop()
        tts.shutdown()
    }

    override fun onReadyForSpeech(params: Bundle?) = onListeningChanged(true)
    override fun onResults(results: Bundle?) {
        onListeningChanged(false)
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        if (text.isBlank()) errorCallback?.invoke("Não consegui ouvir. Tente novamente.")
        else resultCallback?.invoke(text)
    }
    override fun onError(error: Int) {
        onListeningChanged(false)
        errorCallback?.invoke("Não entendi ainda. Fale mais perto do aparelho.")
    }
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = onListeningChanged(false)
    override fun onPartialResults(partialResults: Bundle?) = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}
