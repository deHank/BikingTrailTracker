package com.example.uitutorial

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GPSHandler(private val context: Context) {



    private var locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var lastKnownLocation: Location? = null
    private lateinit var looperThread: Thread
    private lateinit var backgroundHandler: Handler

    // Using a shared flow or a replay cache to provide the *latest* location
    private val _locationUpdates = MutableSharedFlow<Location>(
        replay = 1, // Keep the last emitted item
        onBufferOverflow = BufferOverflow.DROP_OLDEST // Drop if buffer is full
    )
    val locationUpdates: Flow<Location> = _locationUpdates.asSharedFlow() // Expose as SharedFlow


    private var locationUpdateListener: ((Location) -> Unit)? = null

    // LocationListener implementation to receive updates from LocationManager
    private val androidLocationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // When a new location is received, pass it to our registered listener
            locationUpdateListener?.invoke(location)
            Log.d(TAG, "Real Location Updated: Lat ${location.latitude}, Lon ${location.longitude}, Speed ${location.speed}")
        }

        @Deprecated("Deprecated in API 29")
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            // This method is deprecated in API 29+. For newer APIs, consider `onProviderEnabled/Disabled`
            // and `onLocationChanged` for status information if needed from location object.
            Log.d(TAG, "Location provider status changed: $provider, Status: $status")
        }

        override fun onProviderEnabled(provider: String) {
            Log.d(TAG, "Location provider enabled: $provider")
        }

        override fun onProviderDisabled(provider: String) {
            Log.d(TAG, "Location provider disabled: $provider")
        }
    }


    companion object {
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Float = 10f // meters
    }

    init {
        looperThread = Thread {
            Looper.prepare()
            backgroundHandler = Handler(Looper.myLooper()!!)
            Looper.loop()
        }
        looperThread.start()
        createLocationListener()


    }

    @SuppressLint("MissingPermission")
    private fun createLocationListener() {
        Log.d("GPS Handler", "GPS Handler was created")
        val locationListener = object : LocationListenerCompat {
            override fun onLocationChanged(location: Location) {
                // Handle location updates
                Log.d("LocationListener", "Location changed: $location")
                Log.d("LocationListener", "Speed was: ${location.speed}")
                // You can update the UI or perform any action based on the new location
            }

            override fun onProviderEnabled(provider: String) {
                // Handle when the location provider is enabled
                //Log.d("LocationListener", "Provider enabled: $provider")
            }

            override fun onProviderDisabled(provider: String) {
                // Handle when the location provider is disabled
                //Log.d("LocationListener", "Provider disabled: $provider")
            }
        }

        val looper = Looper.myLooper() ?: Looper.getMainLooper()
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationRequest = LocationRequestCompat.Builder(200).setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY).setMinUpdateIntervalMillis(100).build()
        //Log.d("Looper", "before starting created")
        LocationManagerCompat.requestLocationUpdates(
            locationManager,
            LocationManager.FUSED_PROVIDER,
            locationRequest,
            locationListener,
            looper
        )
        //Log.d("Location Listener", "was created")
    }

    /**
     * Starts listening for GPS location updates.
     * Requires ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION permissions to be granted.
     *
     * @param listener A lambda function to be called with the new Location object on update.
     */
    @SuppressLint("MissingPermission") // Permissions are handled by MainActivity, suppressing lint warning
    fun startLocationUpdates(listener: (Location) -> Unit) {
        this.locationUpdateListener = listener

        // Check for permissions at this point as well, though MainActivity should ensure it.
        val fineLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted && !coarseLocationGranted) {
            Log.e(TAG, "Location permissions not granted. Cannot start location updates.")
            // In a real app, you might want to show a user-friendly message or throw an exception.
            return
        }

        try {
            // Request updates from the GPS provider.
            // You can adjust minTimeMs (milliseconds) and minDistanceM (meters) for update frequency.
            // For a Strava-like app, frequent updates are important.
            // Using 1000ms (1 second) and 1 meter for example.
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L, // minTimeMs: minimum time interval between location updates, in milliseconds.
                1.0F,  // minDistanceM: minimum distance between location updates, in meters.
                androidLocationListener
            )
            Log.d(TAG, "Real GPS location updates requested.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to request location updates: ${e.message}", e)
        }
    }


    suspend fun getCurrentLocation(): Location? {
        return withContext(Dispatchers.IO) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission not granted, handle it as needed
                return@withContext null
            }

            val location = suspendCoroutine<Location?> { continuation ->
                // Retrieve the last known location
                val lastKnownLocation =
                    locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (lastKnownLocation != null) {
                    // Return the last known location if available
                    continuation.resume(lastKnownLocation)
                }
            }

            location
        }
    }




}