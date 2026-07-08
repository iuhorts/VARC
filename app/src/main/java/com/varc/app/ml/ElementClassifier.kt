package com.varc.app.ml

import com.varc.app.data.models.DetectedElement
import com.varc.app.data.models.ProgramComponents
import kotlin.math.*

object ElementClassifier {

    data class ClassificationResult(
        val elements: List<DetectedElement>,
        val fallDetected: Boolean = false,
        val fallCount: Int = 0,
        val programDuration: Float = 0f,
        val pcs: Double = 0.0,
        val deductions: Double = 0.0,
        val programComponents: ProgramComponents = ProgramComponents()
    )

    fun classifyFromPoseData(
        poseSequence: List<PoseData>,
        frameTimestamps: List<Float>
    ): ClassificationResult {
        val duration = if (frameTimestamps.isNotEmpty()) frameTimestamps.last() else 0f
        if (poseSequence.size < 5) {
            return ClassificationResult(emptyList(), false, programDuration = duration)
        }

        val landmarks = (0..32).associateWith { type ->
            poseSequence.mapNotNull { frame ->
                frame.landmarks.find { it.type == type }
            }
        }

        val bodyHeight = estimateBodyHeight(landmarks)
        val fallCount = detectFallCount(poseSequence, landmarks)
        val jumps = detectJumps(poseSequence, landmarks, frameTimestamps, bodyHeight, duration)
        val spins = detectSpins(poseSequence, landmarks, frameTimestamps, bodyHeight)
        val allElements = mergeOverlapping(jumps + spins)

        if (allElements.isEmpty()) {
            val midIdx = poseSequence.size / 2
            val start = frameTimestamps.getOrElse(midIdx - 15) { 0f }.coerceAtLeast(0f)
            val end = frameTimestamps.getOrElse(midIdx + 15) { duration }.coerceAtMost(duration)
            allElements.add(DetectedElement(
                type = "STEP", name = "Footwork Sequence (StB)", level = "1",
                baseValue = 1.80, goe = 0,
                goeFactors = listOf("Default sequence"),
                finalValue = 1.80,
                timestampStart = start, timestampEnd = end,
                confidence = 0.5f
            ))
        }

        val deductions = 0.0
        val pcsComponents = estimatePCS(allElements, duration)
        val pcsTotal = kotlin.math.round(
            (pcsComponents.skatingSkills + pcsComponents.transitions +
             pcsComponents.performance + pcsComponents.choreography).toDouble() * 100.0
        ) / 100.0

        return ClassificationResult(
            elements = allElements,
            fallDetected = fallCount > 0,
            fallCount = fallCount,
            programDuration = duration,
            pcs = pcsTotal,
            deductions = deductions,
            programComponents = pcsComponents
        )
    }

    fun estimatePCS(elements: List<DetectedElement>, duration: Float): ProgramComponents {
        val numElements = elements.size.coerceAtLeast(1)
        val hasJumps = elements.any { it.type == "JUMP" }
        val hasSpins = elements.any { it.type == "SPIN" }
        val hasSteps = elements.any { it.type == "STEP" || it.name.contains("Footwork") }
        val variety = listOf(hasJumps, hasSpins, hasSteps).count { it }

        val base = ((numElements * 0.3f).coerceIn(1.5f, 4.0f))
        val ss = (base + variety * 0.3f).coerceAtMost(5.0f)
        val tr = (base * 0.85f + variety * 0.2f).coerceAtMost(4.5f)
        val pe = (base * 0.9f + (if (elements.any { it.confidence > 0.7f }) 0.5f else 0f)).coerceAtMost(4.5f)
        val ch = (base * 0.8f + variety * 0.25f).coerceAtMost(4.5f)

        return ProgramComponents(
            skatingSkills = (kotlin.math.round(ss * 100) / 100f),
            transitions = (kotlin.math.round(tr * 100) / 100f),
            performance = (kotlin.math.round(pe * 100) / 100f),
            choreography = (kotlin.math.round(ch * 100) / 100f)
        )
    }

    private fun estimateBodyHeight(landmarks: Map<Int, List<PoseLandmark>>): Float {
        val nose = landmarks[0]?.firstOrNull() ?: return 240f
        val ankle = landmarks[27]?.firstOrNull() ?: landmarks[28]?.firstOrNull() ?: return 240f
        return abs(nose.y - ankle.y) * 1.2f
    }

    private fun rotationQuality(estimated: Int, expected: Int): String {
        if (expected == 0) return ""
        val ratio = estimated.toFloat() / expected.toFloat()
        return when {
            ratio < 0.3f -> "<<<"
            ratio < 0.7f -> "<<"
            ratio < 0.9f -> "<"
            else -> ""
        }
    }

    private fun mergeOverlapping(elements: List<DetectedElement>): MutableList<DetectedElement> {
        val sorted = elements.sortedBy { it.timestampStart }
        val merged = mutableListOf<DetectedElement>()
        for (elem in sorted) {
            val idx = merged.indexOfLast { overlapRatio(it, elem) > 0.4f }
            if (idx >= 0) {
                if (elem.confidence > merged[idx].confidence) merged[idx] = elem
            } else {
                merged.add(elem)
            }
        }
        return merged
    }

    private fun overlapRatio(a: DetectedElement, b: DetectedElement): Float {
        val start = maxOf(a.timestampStart, b.timestampStart)
        val end = minOf(a.timestampEnd, b.timestampEnd)
        if (start >= end) return 0f
        val overlap = end - start
        return overlap / minOf(a.timestampEnd - a.timestampStart, b.timestampEnd - b.timestampStart)
    }

    private fun angle2D(a: Triple<Float, Float, Float>, b: Triple<Float, Float, Float>, c: Triple<Float, Float, Float>): Float {
        val v1 = Pair(a.first - b.first, a.second - b.second)
        val v2 = Pair(c.first - b.first, c.second - b.second)
        val dot = v1.first * v2.first + v1.second * v2.second
        val m1 = sqrt(v1.first * v1.first + v1.second * v1.second)
        val m2 = sqrt(v2.first * v2.first + v2.second * v2.second)
        if (m1 == 0f || m2 == 0f) return 0f
        return (acos((dot / (m1 * m2)).coerceIn(-1f, 1f)) * 180f / PI).toFloat()
    }

    // ── Fall Detection ──────────────────────────────────────

    private fun detectFallCount(
        poseSequence: List<PoseData>,
        landmarks: Map<Int, List<PoseLandmark>>
    ): Int {
        if (poseSequence.size < 10) return 0
        val windowSize = (poseSequence.size / 6).coerceIn(3, 10)
        var count = 0
        var inFall = false
        for (i in 0 until poseSequence.size - windowSize) {
            val lh = landmarks[23]?.getOrNull(i) ?: continue
            val rh = landmarks[24]?.getOrNull(i) ?: continue
            val nose = landmarks[0]?.getOrNull(i) ?: continue
            val hipY = (lh.y + rh.y) / 2f
            val isDown = nose.y > hipY + 30f
            if (isDown && !inFall) {
                count++
                inFall = true
            } else if (!isDown) {
                inFall = false
            }
        }
        return count
    }

    // ── Jump Detection ──────────────────────────────────────

    private fun detectJumps(
        poseSequence: List<PoseData>,
        landmarks: Map<Int, List<PoseLandmark>>,
        timestamps: List<Float>,
        bodyHeight: Float,
        duration: Float
    ): List<DetectedElement> {
        val n = poseSequence.size
        if (n < 8) return emptyList()

        val hipY = Array<Float?>(n) { i ->
            val lh = landmarks[23]?.getOrNull(i) ?: return@Array null
            val rh = landmarks[24]?.getOrNull(i) ?: return@Array null
            (lh.y + rh.y) / 2f
        }

        val smooth = Array<Float?>(n) { i ->
            var sum = 0f; var cnt = 0
            for (j in (i - 1)..(i + 1)) { val v = hipY.getOrNull(j) ?: continue; sum += v; cnt++ }
            if (cnt > 0) sum / cnt else null
        }

        val shoulderAngle = Array<Float?>(n) { i ->
            val ls = landmarks[11]?.getOrNull(i) ?: return@Array null
            val rs = landmarks[12]?.getOrNull(i) ?: return@Array null
            atan2(rs.y - ls.y, rs.x - ls.x)
        }

        val leftKnee = Array<Float?>(n) { i ->
            val h = landmarks[23]?.getOrNull(i) ?: return@Array null
            val k = landmarks[25]?.getOrNull(i) ?: return@Array null
            val a = landmarks[27]?.getOrNull(i) ?: return@Array null
            angle2D(Triple(h.x, h.y, 0f), Triple(k.x, k.y, 0f), Triple(a.x, a.y, 0f))
        }
        val rightKnee = Array<Float?>(n) { i ->
            val h = landmarks[24]?.getOrNull(i) ?: return@Array null
            val k = landmarks[26]?.getOrNull(i) ?: return@Array null
            val a = landmarks[28]?.getOrNull(i) ?: return@Array null
            angle2D(Triple(h.x, h.y, 0f), Triple(k.x, k.y, 0f), Triple(a.x, a.y, 0f))
        }

        val minJumpPx = (bodyHeight * 0.04f).coerceAtLeast(8f)
        val elements = mutableListOf<DetectedElement>()

        for (i in 1 until n - 1) {
            val prev = smooth[i - 1] ?: continue
            val curr = smooth[i] ?: continue
            val next = smooth[i + 1] ?: continue

            if (curr >= prev || curr >= next) continue

            val displacement = maxOf(prev, next) - curr
            if (displacement < minJumpPx) continue

            val hasKneeBend = (leftKnee.getOrNull(i - 1)?.let { it < 150f } == true) ||
                              (rightKnee.getOrNull(i - 1)?.let { it < 150f } == true)
            if (!hasKneeBend) continue

            val entryAngle = shoulderAngle.getOrNull(maxOf(0, i - 1)) ?: continue
            val exitAngle = shoulderAngle.getOrNull(minOf(n - 1, i + 1))
            val rotDeg = if (exitAngle != null) {
                val raw = abs(exitAngle - entryAngle)
                (raw * 180.0 / PI).coerceAtMost(720.0)
            } else 0.0

            val estimatedRotations = ((rotDeg + 90.0) / 180.0).roundToInt().coerceAtLeast(1)
            val isAxel = rotDeg > 225.0

            val (code, jname, baseVal, level) = jumpClass(estimatedRotations, isAxel)
            val expectedRevs = (code.firstOrNull()?.toString()?.toIntOrNull() ?: 0)
            val rotQual = rotationQuality(estimatedRotations, expectedRevs)
            val startTime = timestamps.getOrElse(maxOf(0, i - 1)) { 0f }
            val endTime = timestamps.getOrElse(minOf(n - 1, i + 1)) { 0f }

            val secondHalf = duration > 0f && startTime >= duration * 0.5f

            elements.add(DetectedElement(
                type = "JUMP", name = jname, level = code,
                baseValue = baseVal, goe = 1,
                goeFactors = listOf(
                    "Rot: ~${estimatedRotations} rev",
                    "Alt: ${"%.0f".format(displacement)}px"
                ),
                finalValue = baseVal * 1.1,
                timestampStart = startTime, timestampEnd = endTime,
                confidence = (displacement / (bodyHeight * 0.15f)).coerceIn(0f, 1f),
                rotationQuality = rotQual,
                isSecondHalf = secondHalf
            ))
        }

        return elements
    }

    private data class JumpSpec(val code: String, val name: String, val baseValue: Double, val level: String)

    private fun jumpClass(rot: Int, axel: Boolean): JumpSpec = when {
        rot >= 3 && axel -> JumpSpec("3A", "Axel Triple (3A)", 5.50, "3")
        rot >= 3 -> JumpSpec("3S", "Salchow Triple (3S)", 3.20, "3")
        rot == 2 && axel -> JumpSpec("2A", "Axel Doble (2A)", 2.50, "2")
        rot == 2 -> JumpSpec("2S", "Salchow Doble (2S)", 1.70, "2")
        rot == 1 && axel -> JumpSpec("1A", "Axel Sencillo (1A)", 1.30, "1")
        rot == 1 -> JumpSpec("1Lo", "Loop Sencillo (1Lo)", 0.90, "1")
        rot < 1 -> JumpSpec("1Th", "Thoren (1Th)", 0.90, "1")
        else -> JumpSpec("1T", "Toe Loop Sencillo (1T)", 0.60, "1")
    }

    // ── Spin Detection ──────────────────────────────────────

    private fun detectSpins(
        poseSequence: List<PoseData>,
        landmarks: Map<Int, List<PoseLandmark>>,
        timestamps: List<Float>,
        bodyHeight: Float
    ): List<DetectedElement> {
        val n = poseSequence.size
        if (n < 10) return emptyList()

        val angle = Array<Double?>(n) { i ->
            val ls = landmarks[11]?.getOrNull(i) ?: return@Array null
            val rs = landmarks[12]?.getOrNull(i) ?: return@Array null
            atan2((rs.y - ls.y).toDouble(), (rs.x - ls.x).toDouble())
        }

        val unwrapped = mutableListOf<Double>()
        for (a in angle) {
            val v = a ?: continue
            if (unwrapped.isEmpty()) { unwrapped.add(v); continue }
            var adj = v
            while (adj - unwrapped.last() > PI) adj -= 2.0 * PI
            while (adj - unwrapped.last() < -PI) adj += 2.0 * PI
            unwrapped.add(adj)
        }
        if (unwrapped.size < 6) return emptyList()

        val window = 3
        val elements = mutableListOf<DetectedElement>()
        var start: Int? = null

        for (i in window until unwrapped.size) {
            val delta = abs(unwrapped[i] - unwrapped[i - window])
            val degPerFrame = (delta * 180.0 / PI) / window
            if (degPerFrame > 8.0) {
                if (start == null) start = i - window
            } else {
                if (start != null && i - start >= 5) {
                    buildSpin(landmarks, timestamps, start, i)?.let { elements.add(it) }
                }
                start = null
            }
        }
        if (start != null && unwrapped.size - start >= 5) {
            buildSpin(landmarks, timestamps, start, unwrapped.size - 1)?.let { elements.add(it) }
        }

        return elements
    }

    private fun buildSpin(
        landmarks: Map<Int, List<PoseLandmark>>,
        timestamps: List<Float>,
        startIdx: Int, endIdx: Int
    ): DetectedElement? {
        val mid = (startIdx + endIdx) / 2
        var upright = 0; var sit = 0; var camel = 0

        for (i in mid - 1..mid + 1) {
            val lh = landmarks[23]?.getOrNull(i) ?: continue
            val rh = landmarks[24]?.getOrNull(i) ?: continue
            val lk = landmarks[25]?.getOrNull(i) ?: continue
            val rk = landmarks[26]?.getOrNull(i) ?: continue
            val la = landmarks[27]?.getOrNull(i) ?: continue
            val ra = landmarks[28]?.getOrNull(i) ?: continue

            val la2 = angle2D(Triple(lh.x, lh.y, 0f), Triple(lk.x, lk.y, 0f), Triple(la.x, la.y, 0f))
            val ra2 = angle2D(Triple(rh.x, rh.y, 0f), Triple(rk.x, rk.y, 0f), Triple(ra.x, ra.y, 0f))
            val skate = maxOf(la2, ra2)
            val free = minOf(la2, ra2)

            when {
                skate > 150f && free > 130f -> upright++
                skate in 60f..130f && free in 50f..120f -> sit++
                skate > 150f && free > 140f -> camel++
            }
        }

        val total = (upright + sit + camel).coerceAtLeast(1)
        val hasU = upright.toFloat() / total >= 0.3f
        val hasS = sit.toFloat() / total >= 0.3f
        val hasC = camel.toFloat() / total >= 0.3f
        val positions = listOf(hasU, hasS, hasC).count { it }

        val startTime = timestamps.getOrElse(startIdx) { 0f }
        val endTime = timestamps.getOrElse(endIdx) { 0f }
        val duration = endTime - startTime

        val (name, baseVal, level) = when {
            positions >= 2 -> Triple("Pirueta Combinada (CoSp)", 2.00, "2")
            hasS -> Triple("Pirueta Sentada (SSp)", 1.30, "1")
            hasC -> Triple("Pirueta de Ángel (CSp)", 1.20, "1")
            else -> Triple("Pirueta Recta (USp)", 1.20, "1")
        }

        return DetectedElement(
            type = "SPIN", name = name, level = level,
            baseValue = baseVal, goe = 1,
            goeFactors = listOf("Duración: ${"%.1f".format(duration)}s"),
            finalValue = baseVal * 1.1,
            timestampStart = startTime, timestampEnd = endTime,
            confidence = minOf(1f, duration / 3f)
        )
    }
}
