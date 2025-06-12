package com.example.uitutorial

import com.example.uitutorial.GPSHandler
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.location.LocationManager
import android.os.HandlerThread
import android.util.Log
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
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


class TrackWriter(private var context: Context, private val locationManager: LocationManager) {
    private val refreshIntervalMs: Long = 3000
    private lateinit var currLocation: Location
    private val gpsHandler: GPSHandler = GPSHandler(context, locationManager) // Uses injected LocationManager

    private var fitFilter = FitMessages()
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
        var FileOutputStream = FileOutputStream(file)
        var fileEncoder = FileEncoder(file, Fit.ProtocolVersion.V2_0)
        var fileName = "test.fit"
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
        var cumulativeDistance = 0.0F
        var previousLocation: Location? = null
        var totalTimerTime = 0.0F
        var totalElapsedTime = 0.0F

        // Metrics for Lap/Session summary
        var minHeartRate: Short = 255
        var maxHeartRate: Short = 0
        var minSpeed: Float = Float.MAX_VALUE
        var maxSpeed: Float = 0.0F
        var totalCalories: Int = 0 // Needs proper calculation in a real app





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

    @SuppressLint("MissingPermission")
    fun GPSTrackWriter(map: MapView?) {
        map?.invalidate()
        val kmlDocument = KmlDocument()
        val kmlTrack = KmlTrack()
        val kmlPlaceMark = KmlPlacemark()
        val polyLine = Polyline()
        polyLine.width = 1.0f
        polyLine.color = Color.GREEN





        val roadManager = OSRMRoadManager(map?.context, "test")
        roadManager.setMean(OSRMRoadManager.MEAN_BY_BIKE)
        val geoPoints = ArrayList<GeoPoint>(0)
        val handlerThread = HandlerThread("HandlerThread")
        handlerThread.start()
        val looper = handlerThread.looper


        var road = roadManager.getRoad(geoPoints)
        var roadOverLay = RoadManager.buildRoadOverlay(road)
        map?.overlays?.add(roadOverLay)
        map?.invalidate()
        val locationManager =
            (map?.context?.getSystemService(Context.LOCATION_SERVICE) ) as LocationManager
        val locationRequest = LocationRequestCompat.Builder(1000)
            .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY).setMinUpdateIntervalMillis(100)
            .setMinUpdateDistanceMeters(10.0f).build()
        Log.d("Looper", "before starting created")
        val tracksDir = File(map?.context?.filesDir, "tracks")
        if (!tracksDir.exists()) {
            tracksDir.mkdirs()
        }
        var localFile = File(tracksDir, "bike_ride_${getCurrentDateTime()}.kml")


        val locationListener = object : LocationListenerCompat {
            override fun onLocationChanged(location: Location) {
                speed = location.speed
                val writer = BufferedWriter(FileWriter(localFile))
                // Handle location updates
                Log.d("Track Writer", "Location changed: $location")
                // You can update the UI or perform any action based on the new location
                Log.d(
                    "Location",
                    "Latitude: ${location.latitude}, Longitude: ${location.longitude}"
                )
                // Create a GeoPoint with the location's latitude and longitude
                val center = GeoPoint(location.latitude, location.longitude)
                // Set the center of the map view to the new location
                //adding a new geoPoint to the road overlay
                var geoPoint = GeoPoint(location.latitude, location.longitude, location.altitude)
                currLocation = location
                geoPoints.add(geoPoint)
                kmlTrack.add(geoPoint, Date())

                var marker = Marker(map)
                marker.position = geoPoint

                marker.setAnchor(Marker.ANCHOR_BOTTOM, Marker.ANCHOR_CENTER)
                map?.overlays?.add(marker)
                //removing the previous road overlay
                map?.overlays?.remove(roadOverLay)
                var road = roadManager.getRoad(geoPoints)

                roadOverLay = RoadManager.buildRoadOverlay(road)

                map?.overlays?.add(roadOverLay)
                map?.invalidate()
                kmlDocument.mKmlRoot.addOverlay(roadOverLay, kmlDocument)
                writer.write("<?xml version='1.0' encoding='UTF-8'?>\n");
                writer.write("<kml xmlns='http://www.opengis.net/kml/2.2' xmlns:gx='http://www.google.com/kml/ext/2.2'>\n");
                writer.write("<Document>\n")
                writer.write("<Placemark>\n")

                //kmlDocument.saveAsKML(localFile)

                kmlTrack.saveAsKML(writer)
                writer.write("</Placemark>\n")
                writer.write("</Document>\n")
                writer.write("</kml>\n")
                writer.flush()
                writer.close()
            }

            override fun onProviderEnabled(provider: String) {
                // Handle when the location provider is enabled
                Log.d("Track Writer", "Provider enabled: $provider")
            }

            override fun onProviderDisabled(provider: String) {
                // Handle when the location provider is disabled
                Log.d("Track Writer", "Provider disabled: $provider")
            }
        }

        LocationManagerCompat.requestLocationUpdates(
            locationManager,
            LocationManager.FUSED_PROVIDER,
            locationRequest,
            locationListener,
            looper
        )


    }


}