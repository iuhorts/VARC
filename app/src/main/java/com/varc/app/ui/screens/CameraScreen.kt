package com.varc.app.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.varc.app.data.SessionRepository
import com.varc.app.data.models.DetectedElement
import com.varc.app.data.models.ScoringResult
import com.varc.app.ml.ElementClassifier
import com.varc.app.ml.PoseEstimator
import com.varc.app.scoring.ScoringEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onAnalysisComplete: (String) -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var progressValue by remember { mutableFloatStateOf(0f) }
    val repository = remember { SessionRepository(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var currentRecording by remember { mutableStateOf<Recording?>(null) }

    val preview = remember { Preview.Builder().build() }
    val recorder = remember { Recorder.Builder().build() }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }

    LaunchedEffect(lifecycleOwner) {
        try {
            val provider = awaitCameraProvider(context)
            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA,
                preview, videoCapture
            )
        } catch (_: Exception) {}
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            videoUri = uri
            processVideo(context, uri, repository, onAnalysisComplete, { progressValue = it }, scope) {
                isProcessing = it
            }
        }
    }

    fun startRecording(file: File) {
        currentRecording = recorder.prepareRecording(context, file)
            .start(ContextCompat.getMainExecutor(context)) { event: VideoRecordEvent ->
                when (event) {
                    is VideoRecordEvent.Start -> {}
                    is VideoRecordEvent.Finalize -> {
                        if (event.error == VideoRecordEvent.Finalize.ERROR_NONE) {
                            val uri = event.outputResults.outputUri
                            videoUri = uri
                            processVideo(context, uri, repository, onAnalysisComplete, { progressValue = it }, scope) {
                                isProcessing = it
                            }
                        }
                    }
                }
            }
    }

    fun toggleRecording() {
        if (isRecording) {
            currentRecording?.stop()
            currentRecording = null
            isRecording = false
        } else {
            isRecording = true
            val dir = File(context.cacheDir, "varc_recordings").also { it.mkdirs() }
            val file = File(dir, "recording_${System.currentTimeMillis()}.mp4")
            startRecording(file)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            currentRecording?.stop()
            cameraExecutor.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VARC", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Outlined.Person, contentDescription = "Perfil")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!hasPermission) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.VideocamOff, contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Se necesita permiso de cámara",
                            style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onRequestPermission) {
                            Text("Conceder permiso")
                        }
                    }
                }
            } else if (isProcessing) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            progress = { progressValue },
                            modifier = Modifier.size(80.dp),
                            strokeWidth = 6.dp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Analizando rutina…",
                            style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${(progressValue * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (videoUri != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Vídeo seleccionado",
                                style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).also { view ->
                                    preview.setSurfaceProvider(view.surfaceProvider)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            if (hasPermission && !isProcessing) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { galleryLauncher.launch("video/*") },
                        modifier = Modifier.size(64.dp), shape = CircleShape
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = "Galería",
                            modifier = Modifier.size(28.dp))
                    }

                    Button(
                        onClick = { toggleRecording() },
                        modifier = Modifier.size(72.dp), shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            if (isRecording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                            contentDescription = if (isRecording) "Detener" else "Grabar",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Text(
                    if (isRecording) "Grabando…" else "Grabar o seleccionar vídeo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private suspend fun awaitCameraProvider(context: Context): ProcessCameraProvider = suspendCancellableCoroutine { cont ->
    val future = ProcessCameraProvider.getInstance(context)
    future.addListener({
        if (cont.isActive) {
            try { cont.resume(future.get(), onCancellation = null) } catch (e: Exception) { cont.resumeWithException(e) }
        }
    }, ContextCompat.getMainExecutor(context))
}

private fun processVideo(
    context: Context,
    uri: Uri,
    repository: SessionRepository,
    onComplete: (String) -> Unit,
    onProgress: (Float) -> Unit,
    scope: CoroutineScope,
    setProcessing: (Boolean) -> Unit
) {
    setProcessing(true)
    onProgress(0f)
    scope.launch {
        val estimator = PoseEstimator()
        try {
            onProgress(0.1f)
            val poses = withContext(Dispatchers.IO) { estimator.processVideo(context, uri) }
            onProgress(0.5f)
            val result = if (poses.isNotEmpty()) {
                val timestamps = poses.indices.map { it * 0.2f }
                val classification = ElementClassifier.classifyFromPoseData(poses, timestamps)
                onProgress(0.7f)
                ScoringEngine.calculateScore(classification)
            } else {
                ScoringResult(videoPath = uri.toString(), tes = 0.0, totalScore = 0.0,
                    elements = listOf(DetectedElement("STEP", "Secuencia Coreográfica (ChSq)", "1",
                        2.00, 1, listOf("No se pudieron detectar poses"), 2.20, 0f, 0f, confidence = 0.3f)))
            }
            onProgress(0.9f)
            repository.saveSession(result)
            onProgress(1f)
            onComplete(uri.toString())
        } catch (e: Exception) {
            repository.saveSession(ScoringResult(videoPath = uri.toString(), tes = 0.0, totalScore = 0.0))
            onComplete(uri.toString())
        } finally {
            estimator.release()
            setProcessing(false)
        }
    }
}
