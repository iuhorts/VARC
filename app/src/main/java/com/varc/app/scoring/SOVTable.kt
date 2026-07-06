package com.varc.app.scoring

import org.json.JSONObject

data class SOVEntry(
    val code: String,
    val name: String,
    val baseValue: Double,
    val category: String = "singles"
)

object SOVTable {

    private val entries = mutableMapOf<String, SOVEntry>()
    private var currentVersion = "World Skate 2025-2026"

    init {
        loadDefaultSOV()
    }

    private fun loadDefaultSOV() {
        val rawSOV = """
            {
                "1A":  {"name": "Axel Sencillo", "baseValue": 1.10},
                "2A":  {"name": "Axel Doble", "baseValue": 3.30},
                "3A":  {"name": "Axel Triple", "baseValue": 8.00},
                "1S":  {"name": "Salchow Sencillo", "baseValue": 0.50},
                "2S":  {"name": "Salchow Doble", "baseValue": 1.30},
                "3S":  {"name": "Salchow Triple", "baseValue": 4.30},
                "1T":  {"name": "Toe Loop Sencillo", "baseValue": 0.40},
                "2T":  {"name": "Toe Loop Doble", "baseValue": 1.30},
                "3T":  {"name": "Toe Loop Triple", "baseValue": 4.20},
                "1Lo": {"name": "Loop Sencillo", "baseValue": 0.50},
                "2Lo": {"name": "Loop Doble", "baseValue": 1.80},
                "3Lo": {"name": "Loop Triple", "baseValue": 4.90},
                "1F":  {"name": "Flip Sencillo", "baseValue": 0.50},
                "2F":  {"name": "Flip Doble", "baseValue": 1.80},
                "3F":  {"name": "Flip Triple", "baseValue": 5.30},
                "1Lz": {"name": "Lutz Sencillo", "baseValue": 0.60},
                "2Lz": {"name": "Lutz Doble", "baseValue": 2.10},
                "3Lz": {"name": "Lutz Triple", "baseValue": 5.90},
                "1Eu": {"name": "Euler", "baseValue": 0.50},
                "USp": {"name": "Pirueta Recta", "baseValue": 1.20},
                "SSp": {"name": "Pirueta Sentada", "baseValue": 1.30},
                "CSp": {"name": "Pirueta de Ángel", "baseValue": 1.20},
                "LSp": {"name": "Pirueta del Revés", "baseValue": 1.20},
                "CoSp": {"name": "Pirueta Combinada", "baseValue": 2.00},
                "CCoSp": {"name": "Pirueta Combinada Cambio Pie", "baseValue": 2.50},
                "StSq": {"name": "Secuencia de Pasos Recta", "baseValue": 1.80},
                "CiSt": {"name": "Secuencia de Pasos Circular", "baseValue": 1.80},
                "SlSt": {"name": "Secuencia de Pasos Serpentina", "baseValue": 1.80},
                "ChSq": {"name": "Secuencia Coreográfica", "baseValue": 2.00}
            }
        """
        val json = JSONObject(rawSOV)
        json.keys().forEach { key ->
            val obj = json.getJSONObject(key)
            entries[key] = SOVEntry(
                code = key,
                name = obj.getString("name"),
                baseValue = obj.getDouble("baseValue")
            )
        }
    }

    fun getEntry(code: String): SOVEntry? = entries[code]

    fun getBaseValue(code: String): Double = entries[code]?.baseValue ?: 0.0

    fun updateFromJson(json: JSONObject) {
        json.keys().forEach { key ->
            val obj = json.getJSONObject(key)
            entries[key] = SOVEntry(
                code = key,
                name = obj.getString("name"),
                baseValue = obj.getDouble("baseValue")
            )
        }
    }

    fun setVersion(version: String) {
        currentVersion = version
    }

    fun getVersion(): String = currentVersion

    fun getAllCodes(): List<String> = entries.keys.toList()
}
