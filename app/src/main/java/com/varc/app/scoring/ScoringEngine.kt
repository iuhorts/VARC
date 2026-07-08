package com.varc.app.scoring

import com.varc.app.data.models.DetectedElement
import com.varc.app.data.models.ProgramComponents
import com.varc.app.data.models.ScoringResult
import com.varc.app.ml.ElementClassifier

object ScoringEngine {

    data class PCSConfig(
        val factor: Double,
        val componentCount: Int
    )

    private val pcsConfigs = mapOf(
        "benjamin" to PCSConfig(0.8, 4),
        "alevin" to PCSConfig(0.8, 4),
        "infantil" to PCSConfig(1.0, 4),
        "cadete" to PCSConfig(1.2, 4),
        "juvenil" to PCSConfig(1.4, 4),
        "senior" to PCSConfig(1.6, 4),
        "master" to PCSConfig(1.6, 4)
    )

    fun calculateScore(
        classification: ElementClassifier.ClassificationResult,
        programCategory: String = "alevin"
    ): ScoringResult {
        val categoryKey = mapCategory(programCategory)

        val evaluatedElements = classification.elements.mapIndexed { _, element ->
            val code = element.code()
            val sovEntry = SOVTable.getEntry(code)
            val baseValue = sovEntry?.baseValue ?: element.baseValue

            val secondHalfBonus = SOVTable.baseValueWithSecondHalf(code, element.isSecondHalf, categoryKey)
            val adjustedBase = if (element.isSecondHalf && categoryKey in listOf("cadete", "juvenil", "senior"))
                secondHalfBonus else baseValue

            val isTripleOrQuad = code.startsWith("3") || code.startsWith("4")
            val rotationAdjusted = when (element.rotationQuality) {
                "<" -> GOECalculator.jumpValueUnder(adjustedBase, isTripleOrQuad)
                "<<" -> GOECalculator.jumpValueHalf(adjustedBase, isTripleOrQuad)
                "<<<" -> GOECalculator.jumpValueDowngraded(code)
                else -> adjustedBase
            }

            val goeFinal = element.goe
            val withGOE = GOECalculator.calculateFinalValue(rotationAdjusted, goeFinal)

            val finalValue = kotlin.math.round(withGOE * 100.0) / 100.0

            element.copy(
                baseValue = adjustedBase,
                finalValue = finalValue
            )
        }

        val tes = kotlin.math.round(evaluatedElements.sumOf { it.finalValue } * 100.0) / 100.0

        val config = pcsConfigs.entries.find { categoryKey.startsWith(it.key) }?.value
            ?: PCSConfig(1.0, 4)

        val fallDeduction = (1..classification.fallCount).sumOf {
            CategoryValidator.fallPenalty(it, programCategory)
        }
        val deductions = classification.deductions + fallDeduction

        val pcsSum = classification.programComponents.run {
            skatingSkills + transitions + performance + choreography
        }
        val weightedPcs = kotlin.math.round(pcsSum.toDouble() * config.factor * 100.0) / 100.0

        val totalScore = kotlin.math.round((tes + weightedPcs - deductions) * 100.0) / 100.0

        return ScoringResult(
            elements = evaluatedElements,
            tes = tes,
            pcs = weightedPcs,
            deductions = deductions,
            totalScore = totalScore,
            programDuration = classification.programDuration,
            programComponents = classification.programComponents,
            fallCount = classification.fallCount
        )
    }

    private fun mapCategory(raw: String): String {
        val lower = raw.lowercase().trim()
        return when {
            lower.contains("benjam") || lower.contains("alev") || lower.contains("prealev") -> "benjamin"
            lower.contains("infant") -> "infantil"
            lower.contains("cadet") -> "cadete"
            lower.contains("juven") -> "juvenil"
            lower.contains("senior") || lower.contains("master") -> "senior"
            else -> "alevin"
        }
    }

    fun validateElements(
        elements: List<DetectedElement>,
        category: String
    ): List<CategoryValidator.Violation> {
        return CategoryValidator.validate(elements, category)
    }
}

fun DetectedElement.code(): String {
    return when {
        name.contains("Axel") && name.contains("Triple") -> "3A"
        name.contains("Axel") && name.contains("Doble") -> "2A"
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
        name.contains("Broken") -> "Br"
        name.contains("Heel") -> "H"
        name.contains("Inverted") -> "In"
        name.contains("No Level") -> name.take(6).uppercase()
        name.contains("Footwork") || name.contains("StB") -> "StB"
        name.contains("Circular") -> "CiSt"
        name.contains("Serpentina") -> "SlSt"
        name.contains("Coreográfica") || name.contains("ChSq") -> "ChSq"
        name.contains("Recta") || name.contains("USp") -> "U"
        name.contains("Sentada") || name.contains("SSp") -> "S"
        name.contains("Ángel") || name.contains("CSp") -> "C"
        name.contains("Combinada") || name.contains("CoSp") -> "U"
        else -> name.take(4).uppercase()
    }
}
