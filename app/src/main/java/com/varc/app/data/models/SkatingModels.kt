package com.varc.app.data.models

import org.json.JSONObject

data class SkaterProfile(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "Patinadora",
    val category: String = "Alevín Femenino",
    val club: String = "",
    val level: String = "Nacional",
    val style: String = "Libre"
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
    val pcs: Double = 0.0,
    val deductions: Double = 0.0,
    val totalScore: Double = 0.0,
    val programDuration: Float = 0f,
    val programComponents: ProgramComponents = ProgramComponents()
)

data class ProgramComponents(
    val skatingSkills: Float = 0f,
    val transitions: Float = 0f,
    val performance: Float = 0f,
    val choreography: Float = 0f
)

fun DetectedElement.toJson(): JSONObject {
    return JSONObject().apply {
        put("type", type)
        put("name", name)
        put("level", level)
        put("baseValue", baseValue)
        put("goe", goe)
        put("finalValue", finalValue)
        put("timestampStart", timestampStart.toDouble())
        put("timestampEnd", timestampEnd.toDouble())
        put("isValid", isValid)
        put("confidence", confidence.toDouble())
    }
}

fun ScoringResult.toJson(): JSONObject {
    return JSONObject().apply {
        put("timestamp", timestamp)
        put("videoPath", videoPath)
        put("tes", tes)
        put("pcs", pcs)
        put("deductions", deductions)
        put("totalScore", totalScore)
        put("programDuration", programDuration.toDouble())
        put("skatingSkills", programComponents.skatingSkills.toDouble())
        put("transitions", programComponents.transitions.toDouble())
        put("performance", programComponents.performance.toDouble())
        put("choreography", programComponents.choreography.toDouble())
        val elementsArray = org.json.JSONArray()
        elements.forEach { elementsArray.put(it.toJson()) }
        put("elements", elementsArray)
    }
}
