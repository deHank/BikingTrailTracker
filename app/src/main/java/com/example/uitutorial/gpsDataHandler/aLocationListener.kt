package com.example.uitutorial.gpsDataHandler

import android.location.Location
import android.location.LocationListener
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi

class aLocationListener : LocationListener {
    override fun onLocationChanged(location: Location) {
        Log.d("LocationListener", "location was $location")
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onLocationChanged(locations: List<Location>) {
        super.onLocationChanged(locations)
        Log.d("LocationListener", "location was $locations")
    }

    override fun onFlushComplete(requestCode: Int) {
        super.onFlushComplete(requestCode)
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        Log.d("Location Listener", "status changed")
        super.onStatusChanged(provider, status, extras)
    }

    override fun onProviderEnabled(provider: String) {
        super.onProviderEnabled(provider)
        Log.d("Location Listener", "provider enabled")
    }

    override fun onProviderDisabled(provider: String) {
        super.onProviderDisabled(provider)
    }
}
