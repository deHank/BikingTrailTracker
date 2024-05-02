import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationProvider
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import com.example.uitutorial.gpsDataHandler.aLocationListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GPSHandler(private val context: Context) {



    private var locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var lastKnownLocation: Location? = null
    private lateinit var looperThread: Thread
    private lateinit var backgroundHandler: Handler







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
                //Log.d("LocationListener", "Location changed: $location")
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
