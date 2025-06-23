package com.example.uitutorial

import android.app.Application
import android.content.ContentValues.TAG
import android.location.Location
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class MapViewModel(application: Application, private val trackWriter: TrackWriter, private val mapPreferencesRepository: MapPreferencesRepository) : AndroidViewModel(application) {
    //private val gpsHandler = GPSHandler(application)

    // LiveData for the current location to be displayed on the map and for zooming
    val _currentLocation = MutableStateFlow<Location??>(null)
    val currentLocation = MutableStateFlow<Location??>(null)

    // State for the map's desired center (can be current location or manually set)
    private val _mapCenter = MutableLiveData<GeoPoint>()
    val mapCenter: LiveData<GeoPoint> = _mapCenter

    // State for the map's desired zoom level
    private val _mapZoom = MutableLiveData(18.0) // Initial zoom
    val mapZoom: LiveData<Double> = _mapZoom

    // State to control if the map should follow the user's location
    private val _isFollowLocationEnabled = MutableStateFlow(true) // Default to true
    val isFollowLocationEnabled: StateFlow<Boolean> = _isFollowLocationEnabled.asStateFlow()

    val recordedLocationsFlow: kotlinx.coroutines.flow.StateFlow<List<GeoPoint>> = trackWriter.recordedLocationsFlow

    // StateFlow to hold the last saved map state (center and zoom)
    private val _lastSavedMapState = kotlinx.coroutines.flow.MutableStateFlow<GeoPoint?>(null)
    val lastSavedMapState: kotlinx.coroutines.flow.StateFlow<GeoPoint?> = _lastSavedMapState
    //private val mapPreferencesRepository = MapPreferencesRepository(context = application.applicationContext)

    init {
        // Observing Location Updates from GPSHandler
        viewModelScope.launch {
            //gpsHandler.getCurrentLocation()
            startLocationUpdates()
        }
    }

    /**
     * Loads the last saved map center and zoom level from preferences.
     */
    private fun loadLastMapState() {
        viewModelScope.launch {
            mapPreferencesRepository.getLastMapState().collect { state ->
                _lastSavedMapState.value = state
                if (state != null) {
                    Log.d(TAG, "Loaded last map state: Lat ${state.latitude}, Lon ${state.longitude}")
                } else {
                    Log.d(TAG, "No last map state found in preferences.")
                }
            }
        }
    }

    /**
     * Saves the current map center and zoom level to preferences.
     * @param geoPoint The current center GeoPoint of the map.
     * @param zoomLevel The current zoom level of the map.
     */
    fun saveLastMapState(geoPoint: GeoPoint) {
        viewModelScope.launch {
            mapPreferencesRepository.saveMapState(geoPoint)
            Log.d(TAG, "Map state saved: ${geoPoint.latitude}, ${geoPoint.longitude}")
        }
    }

    /**
     * Clears the internally held last saved map state.
     * Useful after applying it to the map to prevent re-applying on recomposition.
     */
    fun clearLastSavedMapState() {
        _lastSavedMapState.value = null
        Log.d(TAG, "Cleared internal last saved map state.")
    }

    //function to start listening for GPS location updates
    //updates the _currentLocation StateFlow when a new location is received
    //will need to be updated for TrackWriter
    fun startLocationUpdates() {
        viewModelScope.launch {
            val location = trackWriter.getCurrentLocation()
            val geoPoint = GeoPoint(location!!.latitude, location.longitude)
            mapPreferencesRepository.saveMapState(geoPoint)
            if (location != null) {
                _currentLocation.value = location
                currentLocation.value = location
                _mapCenter.postValue(GeoPoint(location.latitude, location.longitude))
            }

        }

    }

    fun startRecordingAndSaveFile(){
        trackWriter.startRecording()
    }

    fun finishRecordingAndSaveFile(){
        trackWriter.endRecording()
    }
    /**
     * Call this when the user wants to re-center the map to their current location.
     */
    fun zoomToCurrentLocationAndFollow() {
        Log.w("MapViewModel", "zoomToCurrentLocationAndFollow called.")
        _isFollowLocationEnabled.value = true // Ensure follow mode is enabled
        _mapZoom.postValue(18.0) // Set preferred zoom level

        _currentLocation.value?.let { loc ->
            _mapCenter.postValue(GeoPoint(loc.latitude, loc.longitude)) // Force center to current location
        } ?: run {
            Log.w("MapViewModel", "Current location not available for recenter.")
            // You might want to show a toast or error message here via a separate LiveData
        }
    }

    /**
     * Call this when the user manually moves the map, to stop following their location.
     */
    fun onMapManuallyMoved() {
        _isFollowLocationEnabled.value = false
        Log.d("MapViewModel", "Map manually moved, follow location disabled.")
    }


}