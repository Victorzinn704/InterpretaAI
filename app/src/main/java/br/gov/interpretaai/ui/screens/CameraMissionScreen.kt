package br.gov.interpretaai.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.gov.interpretaai.ui.ComicButton
import br.gov.interpretaai.ui.GuidedComicButton
import br.gov.interpretaai.ui.StageHeader
import br.gov.interpretaai.ui.theme.ComicInk
import br.gov.interpretaai.ui.theme.ComicRed
import br.gov.interpretaai.ui.theme.ComicYellow
import java.io.File

@Composable
fun CameraMissionScreen(onBack: () -> Unit, onCaptured: () -> Unit, onSpeak: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var error by remember { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        if (!it) error = "A câmera precisa ser autorizada por um adulto."
    }
    LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(LifecycleCameraController.IMAGE_CAPTURE)
        }
    }
    LaunchedEffect(granted) { if (granted) controller.bindToLifecycle(lifecycleOwner) }

    Column(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StageHeader("Câmera do Gibi", "Etapa 4 • letra M", onBack, onSpeak)
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(4.dp, ComicInk),
            color = Color.Black
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (granted) {
                    AndroidView(
                        factory = { PreviewView(it).apply { this.controller = controller } },
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(3.dp, ComicInk),
                        modifier = Modifier.align(Alignment.Center).padding(20.dp)
                    ) {
                        Text("Ache algo com M!", Modifier.padding(15.dp), fontSize = 20.sp)
                    }
                } else {
                    Text(error ?: "Preparando a câmera...", color = Color.White, modifier = Modifier.padding(24.dp))
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            GuidedComicButton(
                text = "FOTOGRAFAR OBJETO",
                onClick = {
                    val temporaryPhoto = File(context.cacheDir, "mission-${System.currentTimeMillis()}.jpg")
                    val output = ImageCapture.OutputFileOptions.Builder(
                        temporaryPhoto
                    ).build()
                    controller.takePicture(
                        output,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                temporaryPhoto.delete()
                                onCaptured()
                            }
                            override fun onError(exception: ImageCaptureException) {
                                error = "Não foi possível fotografar. Tente outra vez."
                            }
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                color = ComicYellow,
                enabled = granted,
                leading = "📸",
                trailing = "",
                cue = "APONTE E TOQUE"
            )
        }
        Text("🔒 Fotografe objetos, não pessoas. A imagem fica temporariamente no aparelho.")
    }
}
