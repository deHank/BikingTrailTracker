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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GPSHandler(private val context: Context) {



    private var locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var lastKnownLocation: Location? = null





    private lateinit var locationListener: LocationListener

    companion object {
        private const val MIN_DISTANCE_CHANGE_FOR_UPDATES: Float = 10f // meters
    }

    init {
        createLocationListener()



    }

    private fun createLocationListener() {
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                // Update the last known location whenever a new location is received
                lastKnownLocation = location
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this.context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this.context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
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
