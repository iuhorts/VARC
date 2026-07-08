package com.varc.app.ml

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

data class PoseData(
    val landmarks: List<PoseLandmark>,
    val boundingBox: Rect? = null,
    val timestampMs: Long = 0L
)

data class PoseLandmark(
    val type: Int,
    val x: Float,
    val y: Float,
    val z: Float,
    val inFrameLikelihood: Float
)

object FileLog {
    private var writer: java.io.FileWriter? = null
    private var logDir: File? = null
    private const val FILE_NAME = "varc_log.txt"

    fun init(context: Context) {
        try {
            logDir = context.cacheDir
            val file = File(logDir, FILE_NAME)
            file.createNewFile()
            writer = java.io.FileWriter(file, false)
            writeLine("=== VARC Debug Log ===")
            writeLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            writeLine("Android: ${Build.VERSION.SDK_INT}")
            writeLine("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
        } catch (e: Exception) {
            Log.e("VARC-FileLog", "init failed", e)
        }
    }

    fun writeLine(msg: String) {
        try {
            writer?.write("$msg\n")
            writer?.flush()
        } catch (_: Exception) {}
    }

    fun exportToDownloads(context: Context): Uri? {
        close()
        val file = File(logDir, FILE_NAME)
        if (!file.exists()) return null
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { os ->
                    file.inputStream().use { `is` -> `is`.copyTo(os) }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
            }
            uri
        } catch (e: Exception) {
            Log.e("VARC-FileLog", "export failed", e)
            null
        }
    }

    private fun close() {
        try { writer?.close() } catch (_: Exception) {}
    }
}

class PoseEstimator(private val context: Context) {

    private val detector: PoseDetector = PoseDetection.getClient()

    companion object {
        private const val TAG = "VARC-Pose"
        private const val MAX_IMAGE_DIM = 240
    }

    private fun log(msg: String) {
        Log.d(TAG, msg)
        FileLog.writeLine("[PoseEstimator] $msg")
    }

    private fun logWarn(msg: String) {
        Log.w(TAG, msg)
        FileLog.writeLine("[WARN] [PoseEstimator] $msg")
    }

    private fun logError(msg: String, e: Throwable? = null) {
        Log.e(TAG, msg, e)
        FileLog.writeLine("[ERROR] [PoseEstimator] $msg${if (e != null) ": ${e::class.simpleName}: ${e.message}" else ""}")
        e?.let {
            FileLog.writeLine("[ERROR] stacktrace: ${it.stackTraceToString().take(500)}")
        }
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val (w, h) = bitmap.width to bitmap.height
        val maxDim = maxOf(w, h)
        if (maxDim <= MAX_IMAGE_DIM) return bitmap
        val scale = MAX_IMAGE_DIM.toFloat() / maxDim
        val newW = (w * scale).toInt().coerceAtLeast(1)
        val newH = (h * scale).toInt().coerceAtLeast(1)
        log("Scaling ${w}x$h -> ${newW}x${newH}")
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    suspend fun estimatePose(bitmap: Bitmap, rotation: Int = 0): PoseData? = withContext(Dispatchers.Default) {
        var scaled: Bitmap? = null
        try {
            scaled = scaleBitmap(bitmap)
            log("rotation=$rotation")
            val inputImage = InputImage.fromBitmap(scaled, rotation)
            val pose = suspendCancellableCoroutine<Pose?> { cont ->
                detector.process(inputImage)
                    .addOnSuccessListener { result ->
                        if (!cont.isCancelled) cont.resume(result)
                    }
                    .addOnFailureListener { e ->
                        logWarn("ML Kit failure: ${e.message}")
                        if (!cont.isCancelled) cont.resume(null)
                    }
            } ?: return@withContext null
            val rawLandmarks = pose.allPoseLandmarks
            if (rawLandmarks.isEmpty()) return@withContext null
            val landmarks = rawLandmarks.map { lm ->
                PoseLandmark(
                    type = lm.landmarkType,
                    x = lm.position3D.x,
                    y = lm.position3D.y,
                    z = lm.position3D.z,
                    inFrameLikelihood = lm.inFrameLikelihood
                )
            }
            log("Estimated pose: ${landmarks.size} landmarks")
            PoseData(landmarks)
        } catch (e: OutOfMemoryError) {
            logError("OOM in pose estimation", e)
            null
        } catch (e: Throwable) {
            logError("Error estimating pose", e)
            null
        } finally {
            if (scaled != null && scaled !== bitmap) scaled.recycle()
        }
    }

    suspend fun processVideo(
        videoUri: Uri,
        fps: Int = 5,
        onProgress: ((Float) -> Unit)? = null
    ): List<PoseData> = withContext(Dispatchers.IO) {
        val poses = mutableListOf<PoseData>()
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val videoRotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val totalFrames = (durationMs * fps / 1000).toInt()
            log("Video duration: ${durationMs}ms, rotation=$videoRotation, fps=$fps, totalFrames=$totalFrames")
            if (durationMs <= 0) {
                logError("Could not read video duration")
                return@withContext poses
            }
            val intervalMs = (1000 / fps).toLong().coerceAtLeast(50L)
            log("Frame interval: ${intervalMs}ms")
            var timeMs = 0L
            var frameCount = 0
            while (timeMs < durationMs && frameCount < totalFrames) {
                log("Getting frame at ${timeMs}ms")
                val bitmap = try {
                    retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                } catch (e: Throwable) {
                    logError("getFrameAtTime failed", e)
                    null
                }
                if (bitmap != null) {
                    log("Frame $frameCount: ${bitmap.width}x${bitmap.height}, size=${bitmap.byteCount}")
                    estimatePose(bitmap, videoRotation)?.let { pose ->
                        poses.add(pose.copy(timestampMs = timeMs))
                    }
                    bitmap.recycle()
                    frameCount++
                    if (frameCount % 20 == 0) {
                        log("GC hint at frame $frameCount")
                        System.gc()
                    }
                    onProgress?.invoke(frameCount.toFloat() / totalFrames)
                } else {
                    logWarn("No bitmap at ${timeMs}ms")
                    frameCount++
                }
                timeMs += intervalMs
            }
            log("Processed $frameCount frames, ${poses.size} had poses")
        } catch (e: OutOfMemoryError) {
            logError("OOM processing video at frame ${poses.size}", e)
        } catch (e: Throwable) {
            logError("Error processing video", e)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
        poses
    }

    fun release() {
        log("Releasing detector")
        detector.close()
    }
}
