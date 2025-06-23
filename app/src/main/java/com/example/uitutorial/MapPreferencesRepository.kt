package com.example.uitutorial

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences.Key
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.osmdroid.util.GeoPoint

// Extension property to get the DataStore instance
val Context.mapDataStore: DataStore<Preferences> by preferencesDataStore("map_preferences")

/**
 * Repository for saving and loading map preferences using DataStore.
 */
class MapPreferencesRepository(private val context: Context) {

    private val TAG = "MapPrefsRepo"

    // Define keys for preferences
    private object PreferencesKeys {
        val LAST_LATITUDE = doublePreferencesKey("last_latitude")
        val LAST_LONGITUDE = doublePreferencesKey("last_longitude")
        val LAST_ZOOM_LEVEL = doublePreferencesKey("last_zoom_level")
    }

    /**
     * Saves the map's current center and zoom level to DataStore.
     * @param geoPoint The current center GeoPoint of the map.
     * @param zoomLevel The current zoom level of the map.
     */
    suspend fun saveMapState(geoPoint: GeoPoint, zoomLevel: Double) {
        try {
            context.mapDataStore.edit { preferences ->
                preferences[PreferencesKeys.LAST_LATITUDE] = geoPoint.latitude
                preferences[PreferencesKeys.LAST_LONGITUDE] = geoPoint.longitude
                preferences[PreferencesKeys.LAST_ZOOM_LEVEL] = zoomLevel
                Log.d(TAG, "Map state saved to DataStore: Lat ${geoPoint.latitude}, Lon ${geoPoint.longitude}, Zoom $zoomLevel")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving map state to DataStore: ${e.message}", e)
        }
    }

    /**
     * Loads the last saved map state from DataStore.
     * @return A Flow emitting Pair<GeoPoint, Double>? representing the last saved
     * latitude, longitude, and zoom level. Emits null if no data is found.
     */
    fun getLastMapState(): Flow<GeoPoint?> {
        return context.mapDataStore.data
            .map { preferences ->
                val latitude = preferences[PreferencesKeys.LAST_LATITUDE]
                val longitude = preferences[PreferencesKeys.LAST_LONGITUDE]
                val zoom = preferences[PreferencesKeys.LAST_ZOOM_LEVEL]

                if (latitude != null && longitude != null && zoom != null) {
                    // Basic validation for valid geographical ranges
                    if (latitude >= -90.0 && latitude <= 90.0 &&
                        longitude >= -180.0 && longitude <= 180.0 &&
                        zoom >= 0.0 && zoom <= 22.0) { // Standard OSMDroid zoom range is approx 0-22
                        GeoPoint(latitude, longitude)
                    } else {
                        Log.w(TAG, "Loaded map state from DataStore is out of valid range: Lat $latitude, Lon $longitude, Zoom $zoom. Returning null.")
                        null // Return null for invalid data
                    }
                } else {
                    null // No data found
                }
            }
    }
}