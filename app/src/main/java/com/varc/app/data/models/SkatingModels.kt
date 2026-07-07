package com.varc.app.data.models

import org.json.JSONObject

data class SkaterProfile(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "Patinadora",
    val category: String = "Senior Femenino",
    val club: String = "",
    val level: String = "Nacional"
)

data class DetectedElement(
    val type: String,
    val name: String,
    val level: String,
    val baseValue: Double,
    val goe: Int,
    val goeFactors: List<String> = emptyList(),
    val finalValue: Double,
    val timestampStart: Float,
    val timestampEnd: Float,
    val isValid: Boolean = true,
    val confidence: Float = 0f
)

data class ScoringResult(
    val timestamp: Long = System.currentTimeMillis(),
    val videoPath: String = "",
    val elements: List<DetectedElement> = emptyList(),
    val tes: Double = 0.0,
    val deductions: Double = 0.0,
    val totalScore: Double = 0.0,
    val programDuration: Float = 0f
)

data class ProgramComponents(
    val skills: Float = 0f,
    val transitions: Float = 0f,
    val performance: Float = 0f,
    val choreography: Float = 0f,
    val interpretation: Float = 0f
)

fun DetectedElement.toJson(): JSONObject {
    return JSONObject().apply {
        put("type", type)
        put("name", name)
        put("level", level)
        put("baseValue", baseValue)
        put("goe", goe)
        put("finalValue", finalValue)
        put("timestampStart", timestampStart)
        put("timestampEnd", timestampEnd)
        put("isValid", isValid)
        put("confidence", confidence)
    }
}

fun ScoringResult.toJson(): JSONObject {
    return JSONObject().apply {
        put("timestamp", timestamp)
        put("videoPath", videoPath)
        put("tes", tes)
        put("deductions", deductions)
        put("totalScore", totalScore)
        put("programDuration", programDuration)
        val elementsArray = org.json.JSONArray()
        elements.forEach { elementsArray.put(it.toJson()) }
        put("elements", elementsArray)
    }
}
