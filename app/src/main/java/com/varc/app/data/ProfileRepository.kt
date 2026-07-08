package com.varc.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.varc.app.data.models.SkaterProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "varc_profile")

class ProfileRepository(private val context: Context) {

    companion object {
        private val NAME_KEY = stringPreferencesKey("name")
        private val CATEGORY_KEY = stringPreferencesKey("category")
        private val CLUB_KEY = stringPreferencesKey("club")
        private val LEVEL_KEY = stringPreferencesKey("level")
        private val STYLE_KEY = stringPreferencesKey("style")
    }

    fun getProfile(): Flow<SkaterProfile> {
        return context.profileDataStore.data.map { prefs ->
            SkaterProfile(
                name = prefs[NAME_KEY] ?: "Patinadora",
                category = prefs[CATEGORY_KEY] ?: "Alevín Femenino",
                club = prefs[CLUB_KEY] ?: "",
                level = prefs[LEVEL_KEY] ?: "Nacional",
                style = prefs[STYLE_KEY] ?: "Libre"
            )
        }
    }

    suspend fun updateProfile(profile: SkaterProfile) {
        context.profileDataStore.edit { prefs ->
            prefs[NAME_KEY] = profile.name
            prefs[CATEGORY_KEY] = profile.category
            prefs[CLUB_KEY] = profile.club
            prefs[LEVEL_KEY] = profile.level
            prefs[STYLE_KEY] = profile.style
        }
    }
}
