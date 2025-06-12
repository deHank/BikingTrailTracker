package com.example.uitutorial

import android.app.Application
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

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val gpsHandler = GPSHandler(application)

    // LiveData for the current location to be displayed on the map and for zooming
    private val _currentLocation = MutableLiveData<Location>()
    val currentLocation: LiveData<Location> = _currentLocation

    // State for the map's desired center (can be current location or manually set)
    private val _mapCenter = MutableLiveData<GeoPoint>()
    val mapCenter: LiveData<GeoPoint> = _mapCenter

    // State for the map's desired zoom level
    private val _mapZoom = MutableLiveData(18.0) // Initial zoom
    val mapZoom: LiveData<Double> = _mapZoom

    // State to control if the map should follow the user's location
    private val _isFollowLocationEnabled = MutableStateFlow(true) // Default to true
    val isFollowLocationEnabled: StateFlow<Boolean> = _isFollowLocationEnabled.asStateFlow()

    init {
        // Observing Location Updates from GPSHandler
        viewModelScope.launch {
            gpsHandler.getCurrentLocation()
        }
    }

    /**
     * Call this when the user wants to re-center the map to their current location.
     */
    fun zoomToCurrentLocationAndFollow() {
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