package com.varc.app.scoring

import com.varc.app.data.models.DetectedElement
import com.varc.app.data.models.ProgramComponents
import com.varc.app.data.models.ScoringResult
import com.varc.app.ml.ElementClassifier

object ScoringEngine {

    data class PCSConfig(
        val factor: Double,
        val componentCount: Int,
        val useInterpretation: Boolean
    )

    private val pcsConfigs = mapOf(
        "benjamin" to PCSConfig(0.8, 4, false),
        "alevin" to PCSConfig(0.8, 4, false),
        "infantil" to PCSConfig(1.0, 4, false),
        "cadete" to PCSConfig(1.2, 5, true),
        "juvenil" to PCSConfig(1.4, 5, true),
        "senior" to PCSConfig(1.6, 5, true),
        "master" to PCSConfig(1.6, 5, true)
    )

    fun calculateScore(
        classification: ElementClassifier.ClassificationResult,
        programCategory: String = "alevin"
    ): ScoringResult {
        val evaluatedElements = classification.elements.map { element ->
            val sovEntry = SOVTable.getEntry(element.code())
            val baseValue = sovEntry?.baseValue ?: element.baseValue
            val goe = element.goe
            val finalValue = GOECalculator.calculateFinalValue(baseValue, goe)
            element.copy(
                baseValue = baseValue,
                finalValue = finalValue
            )
        }

        val tes = kotlin.math.round(evaluatedElements.sumOf { it.finalValue } * 100.0) / 100.0

        val config = pcsConfigs.entries.find { programCategory.startsWith(it.key) }?.value
            ?: PCSConfig(1.0, 4, false)

        val deductions = classification.deductions

        val totalScore = kotlin.math.round((tes + classification.pcs - deductions) * 100.0) / 100.0

        return ScoringResult(
            elements = evaluatedElements,
            tes = tes,
            pcs = classification.pcs,
            deductions = deductions,
            totalScore = totalScore,
            programDuration = classification.programDuration,
            programComponents = classification.programComponents
        )
    }
}

fun DetectedElement.code(): String {
    return when {
        name.contains("Axel") && name.contains("Doble") -> "2A"
        name.contains("Axel") && name.contains("Triple") -> "3A"
        name.contains("Axel") -> "1A"
        name.contains("Waltz") -> "1W"
        name.contains("Thoren") -> "1Th"
        name.contains("Salchow") && name.contains("Triple") -> "3S"
        name.contains("Salchow") && name.contains("Doble") -> "2S"
        name.contains("Salchow") -> "1S"
        name.contains("Toe Loop") && name.contains("Triple") -> "3T"
        name.contains("Toe Loop") && name.contains("Doble") -> "2T"
        name.contains("Toe Loop") -> "1T"
        name.contains("Loop") && name.contains("Triple") -> "3Lo"
        name.contains("Loop") && name.contains("Doble") -> "2Lo"
        name.contains("Loop") -> "1Lo"
        name.contains("Flip") && name.contains("Triple") -> "3F"
        name.contains("Flip") && name.contains("Doble") -> "2F"
        name.contains("Flip") -> "1F"
        name.contains("Lutz") && name.contains("Triple") -> "3Lz"
        name.contains("Lutz") && name.contains("Doble") -> "2Lz"
        name.contains("Lutz") -> "1Lz"
        name.contains("Upright") -> "U"
        name.contains("Sit") -> "S"
        name.contains("Camel Backward") -> "CBD"
        name.contains("Camel Forward") -> "CFD"
        name.contains("Camel") -> "C"
        name.contains("Layback") -> "L"
        name.contains("Footwork") || name.contains("StB") -> "StB"
        name.contains("Circular") -> "CiSt"
        name.contains("Serpentina") -> "SlSt"
        name.contains("Coreográfica") || name.contains("ChSq") -> "ChSq"
        name.contains("No Level") -> name.take(6).uppercase()
        else -> name.take(4).uppercase()
    }
}
