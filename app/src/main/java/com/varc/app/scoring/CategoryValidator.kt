package com.varc.app.scoring

import com.varc.app.data.models.DetectedElement

data class CategoryRule(
    val maxOneRotationJumps: Int,
    val maxCombos: Int,
    val maxJumpsPerCombo: Int,
    val axelRequired: Boolean,
    val maxSpinLevel: Int,
    val maxStepLevel: Int,
    val forbiddenSpinPositions: Set<String>,
    val secondHalfBonusEligible: Boolean,
    val spinOneComboWithSit: Boolean,
    val fallFirst: Double,
    val fallSecond: Double,
    val fallThirdMinis: Double,
    val fallThirdSenior: Double,
    val fallFourth: Double,
    val fallFifth: Double,
    val fallSixth: Double
)

object CategoryValidator {

    val MINIS = CategoryRule(
        maxOneRotationJumps = 12,
        maxCombos = 2,
        maxJumpsPerCombo = 5,
        axelRequired = true,
        maxSpinLevel = 2,
        maxStepLevel = 2,
        forbiddenSpinPositions = setOf("Br", "H", "In"),
        secondHalfBonusEligible = false,
        spinOneComboWithSit = true,
        fallFirst = 1.0,
        fallSecond = 2.0,
        fallThirdMinis = 3.3,
        fallThirdSenior = 0.0,
        fallFourth = 4.9,
        fallFifth = 6.8,
        fallSixth = 9.0
    )

    val CADETE = MINIS.copy(
        maxOneRotationJumps = 10,
        maxCombos = 3,
        maxJumpsPerCombo = 4,
        axelRequired = true,
        maxSpinLevel = 4,
        maxStepLevel = 4,
        forbiddenSpinPositions = emptySet(),
        secondHalfBonusEligible = true,
        spinOneComboWithSit = false,
        fallFirst = 1.0,
        fallSecond = 2.0,
        fallThirdMinis = 0.0,
        fallThirdSenior = 3.5,
        fallFourth = 5.0,
        fallFifth = 7.0,
        fallSixth = 9.0
    )

    val SENIOR = CADETE.copy(
        maxOneRotationJumps = 8,
        maxJumpsPerCombo = 3,
        maxSpinLevel = 4,
        maxStepLevel = 4,
        fallFirst = 1.0,
        fallSecond = 2.0,
        fallThirdSenior = 3.5,
        fallThirdMinis = 0.0,
        fallFourth = 5.0,
        fallFifth = 7.0,
        fallSixth = 9.0
    )

    private val categoryAlias = mapOf(
        "benjamín" to "minis", "benjamin" to "minis",
        "alevín" to "minis", "alevin" to "minis",
        "prealevín" to "minis", "prealevin" to "minis",
        "infantil" to "cadete",
        "cadete" to "cadete",
        "juvenil" to "senior",
        "senior" to "senior",
        "master" to "senior"
    )

    fun getRule(category: String): CategoryRule {
        val key = categoryAlias[category.lowercase().trim()] ?: "senior"
        return when (key) {
            "minis" -> MINIS
            "cadete" -> CADETE
            else -> SENIOR
        }
    }

    data class Violation(
        val message: String,
        val severity: String
    )

    fun validate(elements: List<DetectedElement>, category: String): List<Violation> {
        val rule = getRule(category)
        val violations = mutableListOf<Violation>()

        val jumps = elements.filter { SOVTable.isCodeJump(it.level) }
        val spins = elements.filter { SOVTable.isCodeSpin(it.level) }
        val steps = elements.filter { SOVTable.isCodeStep(it.level) }

        if (jumps.size > rule.maxOneRotationJumps) {
            violations.add(Violation(
                "Máximo $rule.maxOneRotationJumps saltos (tienes ${jumps.size})", "error"
            ))
        }

        if (rule.axelRequired && jumps.none { it.level in listOf("1A", "2A", "3A") }) {
            violations.add(Violation("Axel obligatorio", "error"))
        }

        val combos = jumps.filter { it.name.contains("+") }
        if (combos.size > rule.maxCombos) {
            violations.add(Violation(
                "Máximo $rule.maxCombos combinaciones (tienes ${combos.size})", "warning"
            ))
        }

        val forbiddenPositionsUsed = spins.filter { it.level in rule.forbiddenSpinPositions }
        if (forbiddenPositionsUsed.isNotEmpty()) {
            val codes = forbiddenPositionsUsed.joinToString(", ") { it.level }
            violations.add(Violation(
                "Posiciones prohibidas en esta categoría: $codes", "error"
            ))
        }

        if (rule.spinOneComboWithSit) {
            val hasSitCombo = spins.any { it.level == "S" || it.name.contains("Sit", true) }
            if (!hasSitCombo) {
                violations.add(Violation(
                    "Requiere un giro combinado con posición sentada", "warning"
                ))
            }
        }

        val stepLevels = steps.map { parseLevel(it.level) }
        if (stepLevels.any { it > rule.maxStepLevel }) {
            violations.add(Violation(
                "Footwork máximo nivel $rule.maxStepLevel en esta categoría", "warning"
            ))
        }

        return violations
    }

    fun fallPenalty(fallNumber: Int, category: String): Double {
        val rule = getRule(category)
        return when (fallNumber) {
            1 -> rule.fallFirst
            2 -> rule.fallSecond
            3 -> if (category.lowercase().trim() in listOf("benjamín", "benjamin", "alevín", "alevin", "prealevín", "prealevin")) rule.fallThirdMinis else rule.fallThirdSenior
            4 -> rule.fallFourth
            5 -> rule.fallFifth
            else -> rule.fallSixth
        }
    }

    private fun parseLevel(level: String): Int {
        val n = level.filter { it.isDigit() }
        return n.toIntOrNull() ?: 0
    }
}
