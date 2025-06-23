package com.example.uitutorial

import FitFileWriter
import android.Manifest
import android.R.attr.tag
import com.example.uitutorial.GPSHandler
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.location.LocationManager
import android.os.HandlerThread
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import com.garmin.fit.Activity
import com.garmin.fit.ActivityType
import com.garmin.fit.DateTime
import com.garmin.fit.Event
import com.garmin.fit.EventMesg
import com.garmin.fit.EventType
import com.garmin.fit.FileEncoder
import com.garmin.fit.FileIdMesg
import com.garmin.fit.Fit
import com.garmin.fit.FitMessages
import com.garmin.fit.GarminProduct
import com.garmin.fit.Manufacturer
import com.garmin.fit.util.DateTimeConverter
import com.garmin.fit.util.SemicirclesConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.bonuspack.kml.KmlDocument
import org.osmdroid.bonuspack.kml.KmlPlacemark
import org.osmdroid.bonuspack.kml.KmlTrack
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.util.Date
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class TrackWriter(private var context: Context, private val locationManager: LocationManager) {
    private val refreshIntervalMs: Long = 3000
    private lateinit var currLocation: Location
    private val gpsHandler: GPSHandler = GPSHandler(context, locationManager) // Uses injected LocationManager

    private val TAG = "TrackWriter"
    private val fitFileWriter: FitFileWriter = FitFileWriter(context)

    //list of points will be used to show where we have already ran
    private val _recordedLocationsForDrawing = mutableListOf<GeoPoint>()
    private val _recordedLocationsFlow = kotlinx.coroutines.flow.MutableStateFlow<List<GeoPoint>>(emptyList())
    val recordedLocationsFlow: kotlinx.coroutines.flow.StateFlow<List<GeoPoint>> = _recordedLocationsFlow

    private var fitFilter = FitMessages()
    // StateFlow to indicate if the app is currently recording an activity
    private val _isRecording = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isRecording: kotlinx.coroutines.flow.StateFlow<Boolean> = _isRecording

    // Flow to emit the latest recorded location for UI updates (e.g., map centering)
    private val _currentRecordedLocation = kotlinx.coroutines.flow.MutableStateFlow<Location?>(null)
    val currentRecordedLocation: kotlinx.coroutines.flow.StateFlow<Location?> = _currentRecordedLocation
    val latestNews: Flow<Location> = flow {
        while(true) {
            //val latestNews = GPSTrackWriter(map)
            emit(currLocation) // Emits the result of the request to the flow
            delay(refreshIntervalMs) // Suspends the coroutine for some time
        }
    }

    var speed = 0.0f


    fun fitExampleWriter(){
        val tag = "FitFileWriter"
        val manufacturer = Manufacturer.DEVELOPMENT
        val productID = 12345

        val file = File("testLocation")
        FileOutputStream(file)
        var fileEncoder = FileEncoder(file, Fit.ProtocolVersion.V2_0)
        "test.fit"
        //date in format for .fit file
        var date = DateTime(System.currentTimeMillis())

        val fileIDMessage = FileIdMesg()

        fileIDMessage.type = com.garmin.fit.File.ACTIVITY
        fileIDMessage.manufacturer = manufacturer
        fileIDMessage.product = productID
        fileIDMessage.serialNumber = System.currentTimeMillis()
        fileIDMessage.timeCreated = date
        fileEncoder.write(fileIDMessage)
        Log.d(tag, "Step 1. FileIdMessage Writer")

        //start even message
        val eventMessageStart = EventMesg()
        eventMessageStart.timestamp = date
        eventMessageStart.event = Event.TIMER
        eventMessageStart.eventType = EventType.START

        // --- Step 3: Record Messages (The core time-series data for the track) ---
        // Iterate through your collected Location data and create a RecordMesg for each.

        // Metrics for Lap/Session summary
        Float.MAX_VALUE

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

    fun startRecording(){
        Log.d("TrackWriter", "Attempting to start recording")

        // Start new FIT file
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "activity_$timestamp.fit"
        val file = File(context.filesDir,"testPath")
        FileEncoder(file, Fit.ProtocolVersion.V2_0)

        fitFileWriter.startNewFitFile(fileName, com.garmin.fit.Sport.RUNNING)
        // Example: assume running sport
        //fitFileEncoder.write()
        //if we are not recording
        if( !_isRecording.value){
            _isRecording.value = true
            gpsHandler.startLocationUpdates {
                //updating the flow for the UI
                newLocation -> _currentRecordedLocation.value = newLocation
                //doubleChecking to make sure we are recording
                if(_isRecording.value){
                    fitFileWriter.addRecord(newLocation)
                    var positionLat = newLocation.latitude
                    var positionLong = newLocation.longitude
                    var geoPoint = GeoPoint(positionLat, positionLong)
                    _recordedLocationsForDrawing.add(geoPoint)
                    _recordedLocationsFlow.value = _recordedLocationsForDrawing.toList() // Update drawing flow with new list

                    Log.d(TAG, "Recorded location and added to FIT file: ${newLocation.latitude}, ${newLocation.longitude}\"")
                }
            }
            Log.d(TAG, "Recording started. _recordedLocationsForDrawing cleared.")
            Toast.makeText(context, "Recording started!", Toast.LENGTH_SHORT).show()
        }
    }

    fun endRecording(){
        _isRecording.value = false
        fitFileWriter.endFitFile()
        _recordedLocationsForDrawing.clear()
        _recordedLocationsFlow.value = emptyList<GeoPoint>()
        _currentRecordedLocation.value = null
    }

    fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentDate = Date()
        return dateFormat.format(currentDate)
    }

    fun getCurrentSpeed(): Float {
        return speed
    }

    @SuppressLint("MissingPermission")
    fun startWritingTrack(map: MapView, locationHandler: GPSHandler) {
        map.invalidate()
        CoroutineScope(Dispatchers.IO).launch {

        }
    }



}