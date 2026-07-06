package com.varc.app.scoring

object GOECalculator {

    private val goePercentageMap = mapOf(
        -5 to -0.50, -4 to -0.40, -3 to -0.30, -2 to -0.20, -1 to -0.10,
        0 to 0.00,
        1 to 0.10, 2 to 0.20, 3 to 0.30, 4 to 0.40, 5 to 0.50
    )

    fun calculateFinalValue(baseValue: Double, goe: Int): Double {
        val clamped = goe.coerceIn(-5, 5)
        val multiplier = goePercentageMap[clamped] ?: 0.0
        return kotlin.math.round((baseValue + baseValue * multiplier) * 100.0) / 100.0
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
