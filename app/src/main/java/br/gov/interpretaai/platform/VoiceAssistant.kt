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
import android.speech.tts.UtteranceProgressListener

class VoiceAssistant(
    context: Context,
    private val onListeningChanged: (Boolean) -> Unit,
    private val onSpeakingChanged: (Boolean) -> Unit,
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
    private var playerFile: File? = null

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
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = onSpeakingChanged(true)
                override fun onDone(utteranceId: String?) = onSpeakingChanged(false)
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = onSpeakingChanged(false)
            })
            ready = true
            pendingSpeech?.let { speak(it) }
            pendingSpeech = null
        } else {
            pendingSpeech = null
            onVoiceUnavailable("A narração não iniciou neste aparelho. Verifique o mecanismo de texto para fala.")
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) { stopPlayback(); return }
        if (!ready) { pendingSpeech = text; return }
        releaseCloudAudio()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "interpreta-${System.nanoTime()}")
    }

    fun playCloudAudio(audio: ByteArray, mimeType: String, onFallback: () -> Unit) {
        var createdFile: File? = null
        runCatching {
            stopPlayback()
            val extension = if (mimeType.contains("wav", ignoreCase = true)) ".wav" else ".ogg"
            val file = File.createTempFile("leia-voice-", extension, appContext.cacheDir)
            createdFile = file
            playerFile = file
            file.writeBytes(audio)
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { completed -> completed.release(); player = null; deletePlayerFile(); onSpeakingChanged(false) }
                setOnErrorListener { failed, _, _ -> failed.release(); player = null; deletePlayerFile(); onSpeakingChanged(false); onFallback(); true }
                prepare()
                onSpeakingChanged(true)
                start()
            }
        }.onFailure {
            createdFile?.delete()
            releaseCloudAudio()
            onSpeakingChanged(false)
            onFallback()
        }
    }

    fun listen(onResult: (String) -> Unit, onError: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onError("Reconhecimento de voz indisponível neste aparelho")
            return
        }
        stopPlayback()
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
        stopPlayback()
        tts.shutdown()
    }

    private fun stopPlayback() {
        pendingSpeech = null
        tts.stop()
        releaseCloudAudio()
        onSpeakingChanged(false)
    }

    private fun releaseCloudAudio() {
        player?.release()
        player = null
        deletePlayerFile()
    }

    private fun deletePlayerFile() {
        playerFile?.delete()
        playerFile = null
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
