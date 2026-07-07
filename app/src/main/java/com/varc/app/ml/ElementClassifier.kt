package com.varc.app.ml

import com.varc.app.data.models.DetectedElement
import kotlin.math.*

object ElementClassifier {

    data class ClassificationResult(
        val elements: List<DetectedElement>,
        val fallDetected: Boolean = false,
        val programDuration: Float = 0f
    )

    fun classifyFromPoseData(
        poseSequence: List<PoseData>,
        frameTimestamps: List<Float>
    ): ClassificationResult {
        val elements = mutableListOf<DetectedElement>()
        var fallDetected = false
        val duration = if (frameTimestamps.isNotEmpty()) frameTimestamps.last() else 0f

        if (poseSequence.size < 3) {
            return ClassificationResult(elements, false, duration)
        }

        val landmarks = (0..32).associateWith { type ->
            poseSequence.mapNotNull { frame ->
                frame.landmarks.find { it.type == type }
            }
        }

        fallDetected = detectFall(poseSequence, landmarks)

        detectJumps(poseSequence, landmarks, frameTimestamps)?.let { elements.addAll(it) }
        detectSpins(poseSequence, landmarks, frameTimestamps)?.let { elements.addAll(it) }

        if (elements.isEmpty()) {
            val midIdx = poseSequence.size / 2
            val start = frameTimestamps.getOrElse(midIdx - 10) { 0f }.coerceAtLeast(0f)
            val end = frameTimestamps.getOrElse(midIdx + 10) { duration }.coerceAtMost(duration)
            elements.add(DetectedElement(
                type = "STEP", name = "Secuencia Coreográfica (ChSq)", level = "1",
                baseValue = 2.00, goe = 1, goeFactors = listOf("Movimiento continuo"),
                finalValue = 2.20, timestampStart = start, timestampEnd = end,
                confidence = 0.5f
            ))
        }

        return ClassificationResult(elements, fallDetected, duration)
    }

    private fun landmarkPos(landmarks: Map<Int, List<PoseLandmark>>, type: Int, frameIdx: Int): Triple<Float, Float, Float>? {
        val lm = landmarks[type]?.getOrNull(frameIdx) ?: return null
        return Triple(lm.x, lm.y, lm.z)
    }

    private fun avgLandmarkPos(landmarks: Map<Int, List<PoseLandmark>>, type: Int): Triple<Float, Float, Float>? {
        val list = landmarks[type] ?: return null
        if (list.isEmpty()) return null
        val avgX = list.map { it.x }.average().toFloat()
        val avgY = list.map { it.y }.average().toFloat()
        val avgZ = list.map { it.z }.average().toFloat()
        return Triple(avgX, avgY, avgZ)
    }

    private fun angleBetween(p1: Triple<Float, Float, Float>, p2: Triple<Float, Float, Float>, p3: Triple<Float, Float, Float>): Float {
        val v1 = Triple(p1.first - p2.first, p1.second - p2.second, p1.third - p2.third)
        val v2 = Triple(p3.first - p2.first, p3.second - p2.second, p3.third - p2.third)
        val dot = v1.first * v2.first + v1.second * v2.second + v1.third * v2.third
        val mag1 = sqrt(v1.first * v1.first + v1.second * v1.second + v1.third * v1.third)
        val mag2 = sqrt(v2.first * v2.first + v2.second * v2.second + v2.third * v2.third)
        if (mag1 == 0f || mag2 == 0f) return 0f
        return (acos((dot / (mag1 * mag2)).coerceIn(-1f, 1f)) * 180f / PI).toFloat()
    }

    private fun distance2D(p1: Triple<Float, Float, Float>, p2: Triple<Float, Float, Float>): Float {
        return sqrt((p1.first - p2.first).pow(2) + (p1.second - p2.second).pow(2))
    }

    private fun detectFall(poseSequence: List<PoseData>, landmarks: Map<Int, List<PoseLandmark>>): Boolean {
        if (poseSequence.size < 5) return false
        val framesToCheck = (poseSequence.size / 2).coerceAtLeast(5)
        for (i in (poseSequence.size - framesToCheck) until poseSequence.size) {
            val leftHip = landmarks[23]?.getOrNull(i)
            val rightHip = landmarks[24]?.getOrNull(i)
            val nose = landmarks[0]?.getOrNull(i)
            if (leftHip != null && rightHip != null && nose != null) {
                val hipY = (leftHip.y + rightHip.y) / 2f
                if (nose.y > hipY + 50f) return true
            }
        }
        return false
    }

    private fun detectJumps(
        poseSequence: List<PoseData>,
        landmarks: Map<Int, List<PoseLandmark>>,
        timestamps: List<Float>
    ): List<DetectedElement>? {
        if (poseSequence.size < 8) return null
        val elements = mutableListOf<DetectedElement>()

        val hipCenterY = (0 until poseSequence.size).map { i ->
            val lh = landmarks[23]?.getOrNull(i)
            val rh = landmarks[24]?.getOrNull(i)
            if (lh != null && rh != null) (lh.y + rh.y) / 2f else null
        }

        val hipVelocities = (1 until hipCenterY.size).mapNotNull { i ->
            val prev = hipCenterY[i - 1]
            val curr = hipCenterY[i]
            if (prev != null && curr != null) curr - prev else null
        }

        var inJump = false
        var jumpStart = 0
        var minHipY = Float.MAX_VALUE
        var maxHipY = Float.MIN_VALUE

        for (i in hipVelocities.indices) {
            val vel = hipVelocities[i]
            if (!inJump && vel < -3f) {
                inJump = true
                jumpStart = i
                minHipY = hipCenterY[i] ?: Float.MAX_VALUE
                maxHipY = hipCenterY[i] ?: Float.MIN_VALUE
            }
            if (inJump) {
                hipCenterY[i]?.let {
                    if (it < minHipY) minHipY = it
                    if (it > maxHipY) maxHipY = it
                }
                if (vel > 3f && (i - jumpStart) > 3) {
                    val displacement = maxHipY - minHipY
                    val baseVal = when {
                        displacement > 30 -> 3.30
                        displacement > 20 -> 2.10
                        else -> 1.10
                    }
                    val name = when {
                        displacement > 30 -> "Axel Doble (2A)"
                        displacement > 20 -> "Lutz Doble (2Lz)"
                        else -> "Axel Sencillo (1A)"
                    }
                    val level = when {
                        displacement > 30 -> "2"
                        displacement > 20 -> "2"
                        else -> "1"
                    }
                    val startTime = timestamps.getOrElse(jumpStart) { 0f }
                    val endTime = timestamps.getOrElse(i) { 0f }
                    elements.add(DetectedElement(
                        type = "JUMP", name = name, level = level,
                        baseValue = baseVal, goe = 1,
                        goeFactors = listOf("Altura: ${"%.0f".format(displacement)}px"),
                        finalValue = baseVal * 1.1,
                        timestampStart = startTime, timestampEnd = endTime,
                        confidence = (displacement / 50f).coerceIn(0f, 1f)
                    ))
                    inJump = false
                    minHipY = Float.MAX_VALUE
                    maxHipY = Float.MIN_VALUE
                }
            }
        }

        return if (elements.isEmpty()) null else elements
    }

    private fun detectSpins(
        poseSequence: List<PoseData>,
        landmarks: Map<Int, List<PoseLandmark>>,
        timestamps: List<Float>
    ): List<DetectedElement>? {
        if (poseSequence.size < 10) return null
        val elements = mutableListOf<DetectedElement>()

        val shoulderAngles = (0 until poseSequence.size).mapNotNull { i ->
            val ls = landmarks[11]?.getOrNull(i)
            val rs = landmarks[12]?.getOrNull(i)
            val lh = landmarks[23]?.getOrNull(i)
            val rh = landmarks[24]?.getOrNull(i)
            if (ls != null && rs != null && lh != null && rh != null) {
                val sCenter = Triple((ls.x + rs.x) / 2f, (ls.y + rs.y) / 2f, 0f)
                val hCenter = Triple((lh.x + rh.x) / 2f, (lh.y + rh.y) / 2f, 0f)
                angleBetween(Triple(ls.x, ls.y, 0f), sCenter, Triple(rs.x, rs.y, 0f))
            } else null
        }

        var inSpin = false
        var spinStart = 0
        var consecutiveRotation = 0

        for (i in 1 until shoulderAngles.size) {
            val change = abs((shoulderAngles[i] ?: 0f) - (shoulderAngles[i - 1] ?: 0f))
            if (!inSpin && change > 10f) {
                inSpin = true
                spinStart = i
                consecutiveRotation = 1
            } else if (inSpin) {
                if (change > 5f) {
                    consecutiveRotation++
                    if (consecutiveRotation > 8) {
                        val startTime = timestamps.getOrElse(spinStart) { 0f }
                        val endTime = timestamps.getOrElse(i) { 0f }
                        elements.add(DetectedElement(
                            type = "SPIN", name = "Pirueta Combinada (CoSp)", level = "2",
                            baseValue = 2.00, goe = 1,
                            goeFactors = listOf("Rotación: ${consecutiveRotation} frames"),
                            finalValue = 2.20,
                            timestampStart = startTime, timestampEnd = endTime,
                            confidence = (consecutiveRotation / 15f).coerceIn(0f, 1f)
                        ))
                        inSpin = false
                        consecutiveRotation = 0
                    }
                }
            }
        }

        return if (elements.isEmpty()) null else elements
    }
}
