package com.icywatermelonstudios.atmoraweather.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.icywatermelonstudios.atmoraweather.data.model.Location
import com.icywatermelonstudios.atmoraweather.data.model.UnitSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException

// one datastore file per app
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// persists everything the user changes
class SettingsStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        // json array of location objects
        val LOCATIONS = stringPreferencesKey("locations")
        val HOME_ID = longPreferencesKey("home_location_id")
        val SELECTED_ID = longPreferencesKey("selected_location_id")
        // json object of units
        val UNITS = stringPreferencesKey("units")
    }

    // if the file can't be read i behave as if nothing was saved
    private val data: Flow<Preferences> = context.settingsDataStore.data.catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    // reading

    val locations: Flow<List<Location>> = data.map { it.readLocations() }

    val homeLocationId: Flow<Long?> = data.map { it[Keys.HOME_ID] }

    val selectedLocationId: Flow<Long?> = data.map { it[Keys.SELECTED_ID] }

    val units: Flow<UnitSettings> = data.map { prefs ->
        prefs[Keys.UNITS]
            ?.let { runCatching { json.decodeFromString<UnitSettings>(it) }.getOrNull() }
            ?: UnitSettings()
    }

    // writing

    suspend fun addLocation(location: Location) {
        context.settingsDataStore.edit { prefs ->
            val list = prefs.readLocations()
            if (list.none { it.id == location.id }) {
                prefs[Keys.LOCATIONS] = json.encodeToString(list + location)
                if (prefs[Keys.HOME_ID] == null) prefs[Keys.HOME_ID] = location.id
            }
            prefs[Keys.SELECTED_ID] = location.id
        }
    }

    suspend fun removeLocation(id: Long) {
        context.settingsDataStore.edit { prefs ->
            val remaining = prefs.readLocations().filterNot { it.id == id }
            prefs[Keys.LOCATIONS] = json.encodeToString(remaining)

            if (prefs[Keys.HOME_ID] == id) {
                val newHome = remaining.firstOrNull()?.id
                if (newHome != null) prefs[Keys.HOME_ID] = newHome else prefs.remove(Keys.HOME_ID)
            }
            if (prefs[Keys.SELECTED_ID] == id) {
                val home = prefs[Keys.HOME_ID]
                if (home != null) prefs[Keys.SELECTED_ID] = home else prefs.remove(Keys.SELECTED_ID)
            }
        }
    }

    suspend fun setHomeLocation(id: Long) {
        context.settingsDataStore.edit { it[Keys.HOME_ID] = id }
    }

    suspend fun selectLocation(id: Long) {
        context.settingsDataStore.edit { it[Keys.SELECTED_ID] = id }
    }

    suspend fun setUnits(units: UnitSettings) {
        context.settingsDataStore.edit { it[Keys.UNITS] = json.encodeToString(units) }
    }

    // helpers

    private fun Preferences.readLocations(): List<Location> =
        this[Keys.LOCATIONS]
            ?.let { runCatching { json.decodeFromString<List<Location>>(it) }.getOrNull() }
            .orEmpty()
}