package com.varc.app.scoring

import com.varc.app.data.models.DetectedElement
import com.varc.app.data.models.ProgramComponents
import com.varc.app.data.models.ScoringResult
import com.varc.app.ml.ElementClassifier

object ScoringEngine {

    private val pcsFactors = mapOf(
        "benjamin" to 0.8, "alevin" to 1.0, "infantil" to 1.2,
        "cadete" to 1.4, "juvenil" to 1.6, "senior" to 2.0,
        "master" to 1.6
    )

    fun calculateScore(
        classification: ElementClassifier.ClassificationResult,
        programCategory: String = "senior"
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

        val totalBaseValue = evaluatedElements.sumOf { it.finalValue }
        val fallDeduction = if (classification.fallDetected) 1.0 else 0.0
        val deductions = fallDeduction
        val tes = kotlin.math.round((totalBaseValue - deductions) * 100.0) / 100.0

        val factor = pcsFactors.entries.find { programCategory.startsWith(it.key) }?.value ?: 1.5
        val numElements = maxOf(evaluatedElements.size, 1)
        val basePcs = (numElements * 1.5 * factor).coerceAtMost(50.0)
        val programComponents = ProgramComponents(
            skills = ((numElements * 0.4 * factor).coerceAtMost(10.0)).toFloat(),
            transitions = ((numElements * 0.3 * factor).coerceAtMost(10.0)).toFloat(),
            performance = ((numElements * 0.3 * factor).coerceAtMost(10.0)).toFloat(),
            choreography = ((numElements * 0.3 * factor).coerceAtMost(10.0)).toFloat(),
            interpretation = ((numElements * 0.3 * factor).coerceAtMost(10.0)).toFloat()
        )
        val pcs = basePcs
        val totalScore = kotlin.math.round((tes + pcs) * 100.0) / 100.0

        return ScoringResult(
            elements = evaluatedElements,
            tes = tes,
            pcs = pcs,
            deductions = deductions,
            totalScore = totalScore,
            programDuration = classification.programDuration,
            programComponents = programComponents
        )
    }
}

private fun DetectedElement.code(): String {
    return when {
        name.contains("Axel") && name.contains("Doble") -> "2A"
        name.contains("Axel") && name.contains("Triple") -> "3A"
        name.contains("Axel") -> "1A"
        name.contains("Pirueta Combinada Cambio") -> "CCoSp"
        name.contains("Pirueta Combinada") -> "CoSp"
        name.contains("Pirueta Recta") -> "USp"
        name.contains("Pirueta Sentada") -> "SSp"
        name.contains("Pirueta de Ángel") -> "CSp"
        name.contains("Pirueta del Revés") -> "LSp"
        name.contains("Secuencia de Pasos Recta") -> "StSq"
        name.contains("Secuencia de Pasos Circular") -> "CiSt"
        name.contains("Secuencia Coreográfica") -> "ChSq"
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
        name.contains("Euler") -> "1Eu"
        else -> name.take(4).uppercase()
    }
}
