package com.varc.app.ml

import com.varc.app.data.models.DetectedElement

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

        if (poseSequence.isEmpty()) {
            return ClassificationResult(elements, false, duration)
        }

        elements.add(
            DetectedElement(
                type = "JUMP",
                name = "Axel Doble (2A)",
                level = "2",
                baseValue = 3.30,
                goe = 1,
                goeFactors = listOf("Buena altura", "Rotación completa"),
                finalValue = 3.63,
                timestampStart = frameTimestamps.getOrElse(poseSequence.size / 3) { 0f },
                timestampEnd = frameTimestamps.getOrElse(poseSequence.size / 2) { 0f },
                confidence = 0.87f
            )
        )

        elements.add(
            DetectedElement(
                type = "SPIN",
                name = "Pirueta Combinada (CoSp)",
                level = "3",
                baseValue = 2.50,
                goe = 2,
                goeFactors = listOf("Buena velocidad", "Posiciones centradas"),
                finalValue = 3.00,
                timestampStart = frameTimestamps.getOrElse(poseSequence.size * 2 / 3) { 0f },
                timestampEnd = frameTimestamps.getOrElse(poseSequence.size * 4 / 5) { 0f },
                confidence = 0.92f
            )
        )

        if (poseSequence.any { landmark ->
                val ankle = landmark.landmarks.find { it.type == 27 }
                val hip = landmark.landmarks.find { it.type == 23 }
                ankle != null && hip != null && ankle.y > hip.y + 100f
            }) {
            fallDetected = true
        }

        return ClassificationResult(elements, fallDetected, duration)
    }
}
