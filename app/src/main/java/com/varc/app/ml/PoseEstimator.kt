package com.varc.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.MediaMetadataRetriever
import android.net.Uri
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

    suspend fun estimatePose(bitmap: Bitmap): PoseData? = withContext(Dispatchers.Default) {
        try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
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
            PoseData(landmarks)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun processVideo(context: Context, videoUri: Uri, maxFrames: Int = 60): List<PoseData> = withContext(Dispatchers.IO) {
        val poses = mutableListOf<PoseData>()
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, videoUri)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            if (durationMs <= 0) return@withContext poses
            val intervalMs = (durationMs / maxFrames).coerceAtLeast(100L)
            var timeMs = 0L
            while (timeMs < durationMs && poses.size < maxFrames) {
                val bitmap = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST)
                if (bitmap != null) {
                    estimatePose(bitmap)?.let { poses.add(it) }
                    bitmap.recycle()
                }
                timeMs += intervalMs
            }
        } catch (_: Exception) {
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
        poses
    }

    fun release() {
        detector.close()
    }
}
