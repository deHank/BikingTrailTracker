package com.example.uitutorial

import GPSHandler
import android.annotation.SuppressLint
import android.content.Context
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.location.LocationManager
import android.os.HandlerThread
import android.text.format.Time
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.kml.KmlDocument
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.Locale


class TrackWriter() {

    val locationListener = object : LocationListenerCompat {
        override fun onLocationChanged(location: Location) {
            // Handle location updates
            Log.d("LocationListener", "Location changed: $location")
            // You can update the UI or perform any action based on the new location
        }

        override fun onProviderEnabled(provider: String) {
            // Handle when the location provider is enabled
            Log.d("LocationListener", "Provider enabled: $provider")
        }

        override fun onProviderDisabled(provider: String) {
            // Handle when the location provider is disabled
            Log.d("LocationListener", "Provider disabled: $provider")
        }
    }

    fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentDate = Date()
        return dateFormat.format(currentDate)
    }


    @SuppressLint("MissingPermission")
    fun startWritingTrack(map: MapView, locationHandler: GPSHandler) {
        map.invalidate()
        CoroutineScope(Dispatchers.IO).launch {
        }
    }

    @SuppressLint("MissingPermission")
    fun GPSTrackWriter(map: MapView) {
        val kmlDocument = KmlDocument()


        val roadManager = OSRMRoadManager(map.context, "test")
        roadManager.setMean(OSRMRoadManager.MEAN_BY_BIKE)
        val geoPoints = ArrayList<GeoPoint>(0)
        val handlerThread = HandlerThread("HandlerThread")
        handlerThread.start()
        val looper = handlerThread.looper

        var road = roadManager.getRoad(geoPoints)
        var roadOverLay = RoadManager.buildRoadOverlay(road)
        map.overlays.add(roadOverLay)
        map.invalidate()
        val locationManager =
            map.context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationRequest = LocationRequestCompat.Builder(1000)
            .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY).setMinUpdateIntervalMillis(100)
            .setMinUpdateDistanceMeters(10.0f).build()
        Log.d("Looper", "before starting created")
        val tracksDir = File(map.context.filesDir, "tracks")
        if (!tracksDir.exists()) {
            tracksDir.mkdirs()
        }
        var localFile = File(tracksDir, "bike_ride_${getCurrentDateTime()}.kml")
        val locationListener = object : LocationListenerCompat {
            override fun onLocationChanged(location: Location) {
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
                geoPoints.add(geoPoint)

                var marker = Marker(map)
                marker.position = geoPoint
                marker.setAnchor(Marker.ANCHOR_BOTTOM, Marker.ANCHOR_CENTER)
                map.overlays.add(marker)
                //removing the previous road overlay
                map.overlays.remove(roadOverLay)
                var road = roadManager.getRoad(geoPoints)
                roadOverLay = RoadManager.buildRoadOverlay(road)
                map.overlays.add(roadOverLay)
                map.invalidate()
                kmlDocument.mKmlRoot.addOverlay(roadOverLay, kmlDocument)
                kmlDocument.saveAsKML(localFile)


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