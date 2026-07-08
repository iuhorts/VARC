package com.varc.app.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.varc.app.data.SessionRepository
import com.varc.app.data.models.DetectedElement
import com.varc.app.data.models.ScoringResult
import com.varc.app.ml.ElementClassifier
import com.varc.app.ml.FileLog
import com.varc.app.ml.PoseEstimator
import com.varc.app.scoring.ScoringEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onAnalysisComplete: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var progressValue by remember { mutableFloatStateOf(0f) }
    val repository = remember { SessionRepository(context) }

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
            if (isProcessing) {
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
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (videoUri != null) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Vídeo seleccionado",
                                style = MaterialTheme.typography.titleLarge)
                        } else {
                            Icon(Icons.Outlined.Videocam, contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Selecciona un vídeo de la galería",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { galleryLauncher.launch("video/*") },
                    modifier = Modifier.size(72.dp), shape = CircleShape
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = "Galería",
                        modifier = Modifier.size(28.dp))
                }
            }

            Text(
                "Seleccionar vídeo de galería",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun processVideo(
    context: Context,
    uri: Uri,
    repository: SessionRepository,
    onComplete: () -> Unit,
    onProgress: (Float) -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
    setProcessing: (Boolean) -> Unit
) {
    setProcessing(true)
    onProgress(0f)
    FileLog.init(context)
    FileLog.writeLine("[CameraScreen] Starting processing")
    scope.launch {
        val estimator = PoseEstimator(context)
        try {
            onProgress(0.05f)
            FileLog.writeLine("[CameraScreen] Step 1: estimating poses")
            val estimateProgress = { f: Float -> onProgress(0.05f + f * 0.45f) }
            val poses = withContext(Dispatchers.IO) { estimator.processVideo(uri, onProgress = estimateProgress) }
            onProgress(0.5f)
            FileLog.writeLine("[CameraScreen] Step 2: ${poses.size} poses, classifying")
            val result = if (poses.isNotEmpty()) {
                val timestamps = poses.indices.map { it * 0.2f }
                val classification = ElementClassifier.classifyFromPoseData(poses, timestamps)
                FileLog.writeLine("[CameraScreen] Step 3: classification done, ${classification.size} elements")
                onProgress(0.7f)
                ScoringEngine.calculateScore(classification)
            } else {
                FileLog.writeLine("[CameraScreen] No poses, using fallback")
                ScoringResult(videoPath = uri.toString(), tes = 0.0, totalScore = 0.0,
                    elements = listOf(DetectedElement("STEP", "Secuencia Coreográfica (ChSq)", "1",
                        2.00, 1, listOf("No se pudieron detectar poses"), 2.20, 0f, 0f, confidence = 0.3f)))
            }
            onProgress(0.9f)
            FileLog.writeLine("[CameraScreen] Step 4: saving session")
            repository.saveSession(result)
            FileLog.writeLine("[CameraScreen] Step 5: navigating")
            onProgress(1f)
            onComplete()
        } catch (e: Throwable) {
            FileLog.writeLine("[CameraScreen] ERROR in try block: ${e::class.simpleName}: ${e.message}")
            FileLog.writeLine("[CameraScreen] stacktrace: ${e.stackTraceToString().take(500)}")
            repository.saveSession(ScoringResult(videoPath = uri.toString(), tes = 0.0, totalScore = 0.0))
            onComplete()
        } finally {
            estimator.release()
            FileLog.writeLine("[CameraScreen] Finally: exporting log")
            FileLog.exportToDownloads(context)
            setProcessing(false)
        }
    }
}
