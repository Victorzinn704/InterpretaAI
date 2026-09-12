package br.gov.interpretaai

import android.Manifest
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.gov.interpretaai.platform.KioskController
import br.gov.interpretaai.platform.VoiceAssistant
import br.gov.interpretaai.ui.InterpretaApp
import br.gov.interpretaai.ui.theme.InterpretaTheme

class MainActivity : ComponentActivity() {
    private lateinit var kiosk: KioskController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        kiosk = KioskController(this)
        kiosk.provisionPoliciesIfDeviceOwner()

        setContent {
            InterpretaTheme {
                val appViewModel: AppViewModel = viewModel()
                val state by appViewModel.state.collectAsStateWithLifecycle()
                val latestState by rememberUpdatedState(state)
                val voice = remember {
                    VoiceAssistant(
                        context = this,
                        onListeningChanged = appViewModel::setListening,
                        onVoiceUnavailable = appViewModel::speechError
                    )
                }
                val microphonePermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        voice.listen(appViewModel::voiceAnswer, appViewModel::speechError)
                    } else {
                        appViewModel.speechError("O microfone precisa ser autorizado por um adulto.")
                    }
                }
                val listen: () -> Unit = {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        voice.listen(appViewModel::voiceAnswer, appViewModel::speechError)
                    } else {
                        microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                DisposableEffect(Unit) {
                    onDispose { voice.release() }
                }

                InterpretaApp(
                    state = latestState,
                    viewModel = appViewModel,
                    speak = voice::speak,
                    listen = listen,
                    kiosk = kiosk
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        kiosk.enterImmersiveMode()
        if (kiosk.isDeviceOwner) kiosk.startFocusMode()
    }
}
