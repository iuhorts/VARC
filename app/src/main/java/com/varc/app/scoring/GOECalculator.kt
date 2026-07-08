package com.varc.app.scoring

object GOECalculator {

    private val goeFlatMap = mapOf(
        -5 to -0.50, -4 to -0.40, -3 to -0.30, -2 to -0.20, -1 to -0.10,
        0 to 0.00,
        1 to 0.10, 2 to 0.20, 3 to 0.30, 4 to 0.40, 5 to 0.50
    )

    fun calculateFinalValue(baseValue: Double, goe: Int): Double {
        val clamped = goe.coerceIn(-5, 5)
        val flatAdjustment = goeFlatMap[clamped] ?: 0.0
        val result = baseValue + flatAdjustment
        return kotlin.math.round(result * 100.0) / 100.0
    }

    fun jumpValueUnder(baseValue: Double, isTripleOrQuad: Boolean = false): Double {
        val reduction = if (isTripleOrQuad) 0.20 else 0.30
        return kotlin.math.round(baseValue * (1.0 - reduction) * 100.0) / 100.0
    }

    fun jumpValueHalf(baseValue: Double, isTripleOrQuad: Boolean = false): Double {
        val reduction = when {
            isTripleOrQuad -> 0.40
            else -> 0.50
        }
        return kotlin.math.round(baseValue * (1.0 - reduction) * 100.0) / 100.0
    }

    fun jumpValueDowngraded(code: String): Double {
        val lowerCode = downgradeCode(code)
        return SOVTable.getBaseValue(lowerCode)
    }

    private fun downgradeCode(code: String): String {
        if (code.length < 2) return code
        val prefix = code[0]
        val suffix = code.substring(1)
        return when (prefix) {
            '4' -> "3$suffix"
            '3' -> "2$suffix"
            '2' -> "1$suffix"
            '1' -> when (suffix) {
                "A" -> "1W"
                "Th", "W" -> "1W"
                else -> "1T"
            }
            else -> code
        }
    }

    fun fallPenalty(fallNumber: Int, isMiniOrTot: Boolean = false): Double {
        return when {
            fallNumber <= 0 -> 0.0
            fallNumber == 1 -> 1.0
            fallNumber == 2 -> 2.0
            fallNumber >= 6 -> if (isMiniOrTot) 9.0 else 11.0
            else -> {
                val base = if (isMiniOrTot) listOf(3.3, 4.9, 6.8) else listOf(3.5, 5.5, 8.0)
                base.getOrElse(fallNumber - 3) { base.last() }
            }
        }
    }

    fun evaluateFall(detected: Boolean): Int {
        return if (detected) -3 else 0
    }

    fun evaluateHeight(verticalDisplacementCm: Float): Int {
        return when {
            verticalDisplacementCm > 40 -> 2
            verticalDisplacementCm > 30 -> 1
            verticalDisplacementCm > 20 -> 0
            else -> -1
        }
    }

    fun evaluateRotation(degreesShort: Float): Int {
        return when {
            degreesShort < 30 -> 1
            degreesShort < 90 -> 0
            degreesShort < 180 -> -1
            else -> -2
        }
    }

    fun evaluateLanding(ankleAngle: Float): Int {
        return if (ankleAngle in 70.0f..110.0f) 1 else -1
    }

    fun evaluateSpinPosition(holdDurationFrames: Int, minRequired: Int = 6): Int {
        return if (holdDurationFrames >= minRequired) 1 else -1
    }
}
