package com.varc.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.varc.app.data.models.DetectedElement
import com.varc.app.data.models.ProgramComponents
import com.varc.app.data.models.ScoringResult
import com.varc.app.data.models.toJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.dataStore by preferencesDataStore(name = "varc_sessions")

class SessionRepository(private val context: Context) {

    companion object {
        private val SESSIONS_KEY = stringPreferencesKey("sessions")
    }

    fun getSessions(): Flow<List<ScoringResult>> {
        return context.dataStore.data.map { prefs ->
            val json = prefs[SESSIONS_KEY] ?: return@map emptyList()
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val elementsArr = obj.optJSONArray("elements")
                val elements = if (elementsArr != null) {
                    (0 until elementsArr.length()).map { ei ->
                        val eo = elementsArr.getJSONObject(ei)
                        DetectedElement(
                            type = eo.optString("type", ""),
                            name = eo.optString("name", ""),
                            level = eo.optString("level", ""),
                            baseValue = eo.optDouble("baseValue", 0.0),
                            goe = eo.optInt("goe", 0),
                            goeFactors = if (eo.has("goeFactors")) {
                                val fa = eo.getJSONArray("goeFactors")
                                (0 until fa.length()).map { fa.getString(it) }
                            } else emptyList(),
                            finalValue = eo.optDouble("finalValue", 0.0),
                            timestampStart = eo.optDouble("timestampStart", 0.0).toFloat(),
                            timestampEnd = eo.optDouble("timestampEnd", 0.0).toFloat(),
                            isValid = eo.optBoolean("isValid", true),
                            confidence = eo.optDouble("confidence", 0.0).toFloat(),
                            rotationQuality = eo.optString("rotationQuality", ""),
                            isSecondHalf = eo.optBoolean("isSecondHalf", false),
                            bonusPercent = eo.optInt("bonusPercent", 0)
                        )
                    }
                } else emptyList()

                val fallCount = obj.optInt("fallCount", 0)
                ScoringResult(
                    timestamp = obj.getLong("timestamp"),
                    videoPath = obj.optString("videoPath", ""),
                    elements = elements,
                    tes = obj.optDouble("tes", 0.0),
                    pcs = obj.optDouble("pcs", 0.0),
                    deductions = obj.optDouble("deductions", 0.0),
                    totalScore = obj.optDouble("totalScore", 0.0),
                    programDuration = obj.optDouble("programDuration", 0.0).toFloat(),
                    fallCount = fallCount,
                    programComponents = ProgramComponents(
                        skatingSkills = obj.optDouble("skatingSkills", 0.0).toFloat(),
                        transitions = obj.optDouble("transitions", 0.0).toFloat(),
                        performance = obj.optDouble("performance", 0.0).toFloat(),
                        choreography = obj.optDouble("choreography", 0.0).toFloat()
                    )
                )
            }.sortedByDescending { it.timestamp }
        }
    }

    suspend fun saveSession(result: ScoringResult) {
        context.dataStore.edit { prefs ->
            val existing = prefs[SESSIONS_KEY] ?: "[]"
            val arr = JSONArray(existing)
            arr.put(result.toJson())
            if (arr.length() > 100) {
                val trimmed = JSONArray()
                for (i in arr.length() - 100 until arr.length()) {
                    trimmed.put(arr.getJSONObject(i))
                }
                prefs[SESSIONS_KEY] = trimmed.toString()
            } else {
                prefs[SESSIONS_KEY] = arr.toString()
            }
        }
    }
}
