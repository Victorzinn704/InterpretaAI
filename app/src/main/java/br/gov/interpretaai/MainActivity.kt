package br.gov.interpretaai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.gov.interpretaai.platform.KioskController
import br.gov.interpretaai.platform.VoiceAssistant
import br.gov.interpretaai.platform.InteractionSounds
import br.gov.interpretaai.ui.InterpretaApp
import br.gov.interpretaai.ui.theme.InterpretaTheme

class MainActivity : ComponentActivity() {
    private lateinit var kiosk: KioskController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kiosk = KioskController(this)
        kiosk.provisionPoliciesIfDeviceOwner()
        val openDeviceSetup = intent?.data?.scheme == "interpretaai"
            && intent?.data?.host == "device-setup"

        setContent {
            InterpretaTheme {
                val appViewModel: AppViewModel = viewModel()
                LaunchedEffect(openDeviceSetup) {
                    if (openDeviceSetup) appViewModel.navigate(AppScreen.EDUCATOR)
                }
                val state by appViewModel.state.collectAsStateWithLifecycle()
                val latestState by rememberUpdatedState(state)
                val voice = remember {
                    VoiceAssistant(
                        context = this,
                        onListeningChanged = appViewModel::setListening,
                        onSpeakingChanged = appViewModel::setSpeaking,
                        onVoiceUnavailable = appViewModel::speechError
                    )
                }
                val sounds = remember { InteractionSounds(this) }
                var pendingVoiceResult by remember { mutableStateOf<(String) -> Unit>(appViewModel::voiceAnswer) }
                val microphonePermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        voice.listen(pendingVoiceResult, appViewModel::speechError)
                    } else {
                        appViewModel.speechError("O microfone precisa ser autorizado por um adulto.")
                    }
                }
                val listen: ((String) -> Unit) -> Unit = { onResult ->
                    pendingVoiceResult = onResult
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        voice.listen(onResult, appViewModel::speechError)
                    } else {
                        microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                DisposableEffect(Unit) {
                    onDispose { voice.release(); sounds.release() }
                }

                InterpretaApp(
                    state = latestState,
                    viewModel = appViewModel,
                    speak = voice::speak,
                    playAudio = voice::playCloudAudio,
                    playSound = { cue -> sounds.play(cue, latestState.reducedStimuli) },
                    listen = listen,
                    kiosk = kiosk
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        kiosk.enterImmersiveMode()
        if (!isInstrumentationInstalled()) kiosk.startFocusMode()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.data?.scheme == "interpretaai" && intent.data?.host == "device-setup") {
            recreate()
        }
    }

    private fun isInstrumentationInstalled(): Boolean = runCatching {
        packageManager.getPackageInfo("$packageName.test", 0)
        true
    }.getOrDefault(false)
}
