package com.fittrack.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class UnitSystem(val distanceLabel: String, val speedLabel: String, val distanceFactor: Double) {
    KM("km", "km/h", 1.0),
    MI("mi", "mph", 0.621371)
}

data class AppPreferences(val unitSystem: UnitSystem = UnitSystem.KM)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val UNITS_KEY = stringPreferencesKey("units")

    val userPreferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { preferences ->
        val unitsName = preferences[UNITS_KEY] ?: UnitSystem.KM.name
        val units = try { UnitSystem.valueOf(unitsName) } catch (e: Exception) { UnitSystem.KM }
        AppPreferences(unitSystem = units)
    }

    suspend fun setUnits(unit: UnitSystem) {
        context.dataStore.edit { preferences ->
            preferences[UNITS_KEY] = unit.name
        }
    }

    // Alias used by SettingsViewModel
    suspend fun updateUnitSystem(unit: UnitSystem) = setUnits(unit)
}
