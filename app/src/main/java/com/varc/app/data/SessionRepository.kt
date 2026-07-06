package com.varc.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.varc.app.data.models.ScoringResult
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
                ScoringResult(
                    timestamp = obj.getLong("timestamp"),
                    videoPath = obj.optString("videoPath", ""),
                    tes = obj.optDouble("tes", 0.0),
                    deductions = obj.optDouble("deductions", 0.0),
                    totalScore = obj.optDouble("totalScore", 0.0),
                    programDuration = obj.optDouble("programDuration", 0.0).toFloat()
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
