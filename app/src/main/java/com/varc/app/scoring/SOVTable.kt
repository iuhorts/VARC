package com.varc.app.scoring

data class SOVEntry(
    val code: String,
    val name: String,
    val baseValue: Double
)

object SOVTable {

    private val entries = mutableMapOf<String, SOVEntry>()
    private var currentVersion = "World Skate RollArt 2025-2026"

    init {
        loadDefaultSOV()
    }

    private fun loadDefaultSOV() {
        val raw = listOf(
            SOVEntry("1W",  "Waltz Jump", 0.40),
            SOVEntry("1Th", "Thoren", 0.90),
            SOVEntry("1A",  "Axel Sencillo", 1.30),
            SOVEntry("1Lo", "Loop Sencillo", 0.90),
            SOVEntry("1T",  "Toe Loop Sencillo", 0.60),
            SOVEntry("1F",  "Flip Sencillo", 0.86),
            SOVEntry("1S",  "Salchow Sencillo", 0.60),
            SOVEntry("1Lz", "Lutz Sencillo", 0.70),
            SOVEntry("2A",  "Axel Doble", 2.50),
            SOVEntry("2Lo", "Loop Doble", 1.90),
            SOVEntry("2T",  "Toe Loop Doble", 1.80),
            SOVEntry("2F",  "Flip Doble", 2.20),
            SOVEntry("2S",  "Salchow Doble", 1.70),
            SOVEntry("2Lz", "Lutz Doble", 2.40),
            SOVEntry("3A",  "Axel Triple", 5.50),
            SOVEntry("3Lo", "Loop Triple", 3.50),
            SOVEntry("3T",  "Toe Loop Triple", 3.30),
            SOVEntry("3F",  "Flip Triple", 3.80),
            SOVEntry("3S",  "Salchow Triple", 3.20),
            SOVEntry("3Lz", "Lutz Triple", 4.20),
            SOVEntry("U",   "Upright Spin", 0.50),
            SOVEntry("S",   "Sit Spin", 0.80),
            SOVEntry("C",   "Camel Spin", 0.70),
            SOVEntry("CBD", "Camel Backward Spin", 1.00),
            SOVEntry("CFD", "Camel Forward Spin", 1.20),
            SOVEntry("L",   "Layback Spin", 0.60),
            SOVEntry("H",   "Heel Spin", 1.20),
            SOVEntry("Br",  "Broken Spin", 1.50),
            SOVEntry("In",  "Inverted Spin", 1.80),
            SOVEntry("NLUpr", "Upright Spin No Level", 0.00),
            SOVEntry("NLSit", "Sit Spin No Level", 0.00),
            SOVEntry("StB", "Footwork Sequence StB", 1.80),
            SOVEntry("CiSt","Circular Step Sequence", 1.80),
            SOVEntry("SlSt","Serpentina Step Sequence", 1.80),
            SOVEntry("ChSq","Coreographic Sequence", 2.00)
        )
        raw.forEach { entries[it.code] = it }
    }

    fun getEntry(code: String): SOVEntry? = entries[code]
    fun getBaseValue(code: String): Double = entries[code]?.baseValue ?: 0.0

    fun getAllCodes(): List<String> = entries.keys.toList()
    fun getVersion(): String = currentVersion

    const val BONUS_SECOND_HALF = 0.10
    const val BONUS_AXEL_DOUBLE_TOE = 0.10
    const val BONUS_DOUBLE_DOUBLE = 0.10
    const val BONUS_DOUBLE_TRIPLE = 0.20
    const val BONUS_TRIPLE_TRIPLE = 0.30

    fun comboBonus(jumpCodes: List<String>): Double {
        if (jumpCodes.size < 2) return 0.0
        val nJumps = jumpCodes.count { it.length >= 2 && it[0].isDigit() }
        val hasTriple = jumpCodes.any { it.startsWith("3") }
        val hasDouble = jumpCodes.any { it.startsWith("2") }
        val hasAxelDoubleToe = jumpCodes.any { it == "2A" || it == "1A" } &&
                               jumpCodes.any { it == "2T" || it == "1T" }
        val hasDoubleDouble = nJumps >= 2 && hasDouble && !hasTriple

        return when {
            hasTriple && jumpCodes.count { it.startsWith("3") || it == "2A" } >= 2 -> BONUS_TRIPLE_TRIPLE
            hasTriple && hasDouble -> BONUS_DOUBLE_TRIPLE
            hasAxelDoubleToe -> BONUS_AXEL_DOUBLE_TOE
            hasDoubleDouble -> BONUS_DOUBLE_DOUBLE
            else -> 0.0
        }
    }

    fun baseValueWithSecondHalf(code: String, isSecondHalf: Boolean, categoryKey: String): Double {
        val bv = getBaseValue(code)
        if (!isSecondHalf) return bv
        val eligible = categoryKey in listOf("cadete", "juvenil", "senior")
        if (!eligible) return bv
        return kotlin.math.round(bv * (1.0 + BONUS_SECOND_HALF) * 100.0) / 100.0
    }

    fun isCodeJump(code: String): Boolean {
        return code.length >= 2 && code[0].isDigit() && code.any { it.isLetter() }
    }

    fun isCodeSpin(code: String): Boolean {
        return code in listOf("U", "S", "C", "CBD", "CFD", "L", "H", "Br", "In", "NLUpr", "NLSit")
    }

    fun isCodeStep(code: String): Boolean {
        return code in listOf("StB", "CiSt", "SlSt", "ChSq")
    }
}
