package com.varc.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class PoseData(
    val landmarks: List<PoseLandmark>,
    val boundingBox: Rect? = null
)

data class PoseLandmark(
    val type: Int,
    val x: Float,
    val y: Float,
    val z: Float,
    val inFrameLikelihood: Float
)

class PoseEstimator {

    private val detector: PoseDetector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
            .build()
    )

    companion object {
        private const val TAG = "VARC-Pose"
        private const val MAX_IMAGE_DIM = 360
    }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val (w, h) = bitmap.width to bitmap.height
        val maxDim = maxOf(w, h)
        if (maxDim <= MAX_IMAGE_DIM) return bitmap
        val scale = MAX_IMAGE_DIM.toFloat() / maxDim
        val newW = (w * scale).toInt().coerceAtLeast(1)
        val newH = (h * scale).toInt().coerceAtLeast(1)
        Log.d(TAG, "Scaling ${w}x$h -> ${newW}x${newH}")
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    suspend fun estimatePose(bitmap: Bitmap): PoseData? = withContext(Dispatchers.Default) {
        var scaled: Bitmap? = null
        try {
            scaled = scaleBitmap(bitmap)
            val inputImage = InputImage.fromBitmap(scaled, 0)
            val pose = suspendCancellableCoroutine<Pose?> { cont ->
                detector.process(inputImage)
                    .addOnSuccessListener { result ->
                        if (!cont.isCancelled) cont.resume(result)
                    }
                    .addOnFailureListener {
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
            Log.d(TAG, "Estimated pose: ${landmarks.size} landmarks")
            PoseData(landmarks)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OOM in pose estimation", e)
            null
        } catch (e: Throwable) {
            Log.e(TAG, "Error estimating pose", e)
            null
        } finally {
            if (scaled != null && scaled !== bitmap) scaled.recycle()
        }
    }

    suspend fun processVideo(
        context: Context,
        videoUri: Uri,
        maxFrames: Int = 30,
        onProgress: ((Float) -> Unit)? = null
    ): List<PoseData> = withContext(Dispatchers.IO) {
        val poses = mutableListOf<PoseData>()
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            Log.d(TAG, "Video duration: ${durationMs}ms, maxFrames=$maxFrames")
            if (durationMs <= 0) {
                Log.e(TAG, "Could not read video duration")
                return@withContext poses
            }
            val intervalMs = (durationMs / maxFrames).coerceAtLeast(100L)
            Log.d(TAG, "Frame interval: ${intervalMs}ms")
            var timeMs = 0L
            var frameCount = 0
            while (timeMs < durationMs && frameCount < maxFrames) {
                Log.d(TAG, "Getting frame at ${timeMs}ms (${timeMs*1000}us)")
                val bitmap = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                if (bitmap != null) {
                    Log.d(TAG, "Frame $frameCount: ${bitmap.width}x${bitmap.height}, size=${bitmap.byteCount}")
                    estimatePose(bitmap)?.let { poses.add(it) }
                    bitmap.recycle()
                    frameCount++
                    if (frameCount % 5 == 0) {
                        Log.d(TAG, "GC hint at frame $frameCount")
                        System.gc()
                    }
                    onProgress?.invoke(frameCount.toFloat() / maxFrames)
                } else {
                    Log.w(TAG, "No bitmap at ${timeMs}ms")
                }
                timeMs += intervalMs
            }
            Log.d(TAG, "Processed $frameCount frames, ${poses.size} had poses")
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OOM processing video at frame ${poses.size}", e)
        } catch (e: Throwable) {
            Log.e(TAG, "Error processing video", e)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
        poses
    }

    fun release() {
        Log.d(TAG, "Releasing detector")
        detector.close()
    }
}
