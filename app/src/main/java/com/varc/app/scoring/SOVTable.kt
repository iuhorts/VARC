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
            // Saltos simples
            SOVEntry("1W",  "Waltz Jump", 0.40),
            SOVEntry("1Th", "Thoren", 0.90),
            SOVEntry("1A",  "Axel Sencillo", 1.30),
            SOVEntry("1Lo", "Loop Sencillo", 0.90),
            SOVEntry("1T",  "Toe Loop Sencillo", 0.60),
            SOVEntry("1F",  "Flip Sencillo", 0.86),
            SOVEntry("1S",  "Salchow Sencillo", 0.60),
            SOVEntry("1Lz", "Lutz Sencillo", 0.70),
            // Saltos dobles
            SOVEntry("2A",  "Axel Doble", 2.50),
            SOVEntry("2Lo", "Loop Doble", 1.90),
            SOVEntry("2T",  "Toe Loop Doble", 1.80),
            SOVEntry("2F",  "Flip Doble", 2.20),
            SOVEntry("2S",  "Salchow Doble", 1.70),
            SOVEntry("2Lz", "Lutz Doble", 2.40),
            // Saltos triples
            SOVEntry("3A",  "Axel Triple", 5.50),
            SOVEntry("3Lo", "Loop Triple", 3.50),
            SOVEntry("3T",  "Toe Loop Triple", 3.30),
            SOVEntry("3F",  "Flip Triple", 3.80),
            SOVEntry("3S",  "Salchow Triple", 3.20),
            SOVEntry("3Lz", "Lutz Triple", 4.20),
            // Giros individuales (posiciones dentro de ComboSpin)
            SOVEntry("U",   "Upright Spin", 0.50),
            SOVEntry("S",   "Sit Spin", 0.80),
            SOVEntry("C",   "Camel Spin", 0.70),
            SOVEntry("CBD", "Camel Backward Spin", 1.00),
            SOVEntry("CFD", "Camel Forward Spin", 1.20),
            SOVEntry("L",   "Layback Spin", 0.60),
            SOVEntry("NLUpr", "Upright Spin No Level", 0.00),
            SOVEntry("NLSit", "Sit Spin No Level", 0.00),
            // Secuencias de pasos
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

    fun baseValueWithBonus(code: String, secondHalf: Boolean = false): Double {
        val bv = getBaseValue(code)
        if (!secondHalf) return bv
        return kotlin.math.round(bv * (1.0 + BONUS_SECOND_HALF) * 100.0) / 100.0
    }
}
