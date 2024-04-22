package com.example.uitutorial

import GPSHandler
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.example.uitutorial.gpsDataHandler.PastTracksViewer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.gridlines.LatLonGridlineOverlay2
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


class MainActivity : ComponentActivity() {

    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private lateinit var map : MapView

    private lateinit var locationManager: LocationManager

    private lateinit var locationHandler: GPSHandler

    private lateinit var location: Location



    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Handle location updates
            Log.d("LocationListener", "Location changed: $location")
            // You can update the UI or perform any action based on the new location
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            // Handle status changes of the location provider
            Log.d("LocationListener", "Status changed: $provider, $status")
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




    //final lateinit var pLocationManager : LocationManager


    init {

    }


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000, 1f, locationListener)



        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))



        map = MapView(this)

        //locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        //handle permissions first, before map is created. not depicted here

        //load/initialize the osmdroid configuration, this can be done
        // This won't work unless you have imported this: org.osmdroid.config.Configuration.*
        getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, if you abuse osm's
        //tile servers will get you banned based on this string.

        //inflate and create the map
        //setContentView(R.layout.main)
        super.onCreate(savedInstanceState)
        // Set content with Jetpack Compose
        setContent {
            CheckLocationPermission {
                BottomAppBarExample(map = this.map)
            }
        }


        map.controller.setZoom(1.0)
        goToCurrentLocation()


    }

    fun goToCurrentLocation(){


    }


    @Composable
    fun CheckLocationPermission(onPermissionGranted: @Composable () -> Unit) {
        val permissionState = remember { ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) }

        // If permission is granted, execute the provided function
        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted()
        } else {
            // Request permission from the user
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }
    // Handle the result of the permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, continue with your UI setup
                setContent {
                    BottomAppBarExample(map = MapView(this))
                }
            } else {
                // Permission denied, handle accordingly (e.g., show an explanation or disable location features)
                // For simplicity, we'll just finish the activity
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume() //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onPause() {
        super.onPause()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause()  //needed for compass, my location overlays, v6.0.0 and up
    }


}





@Composable
fun CustomView(map: MapView) {
    var selectedItem by remember { mutableStateOf(0) }
    Box(modifier = Modifier.fillMaxSize()) {

        // Adds view to Compose
        AndroidView(

            modifier = Modifier.fillMaxSize(), // Occupy the max size in the Compose UI tree
            factory = {
                // Creates view
                map.apply {


                    val overlay = LatLonGridlineOverlay2();
                    //this.overlays.add(overlay);
                    val rotationGestureOverlay = RotationGestureOverlay(this)
                    rotationGestureOverlay.isEnabled

                    this.setMultiTouchControls(true)
                    this.overlays.add(rotationGestureOverlay)

                    val compassOverlay = CompassOverlay(this.context, this)
                    compassOverlay.enableCompass()
                    compassOverlay.isPointerMode = true
                    this.overlays.add(compassOverlay)

                    this.controller.zoomTo(18)



                    setTileSource(TileSourceFactory.MAPNIK)

                    var locationHandler = GPSHandler(this.context)


                    CoroutineScope(Dispatchers.Main).launch {

                        val location = locationHandler.getCurrentLocation()!!
                        // Do something with the location
                        location?.let {
                            Log.d(
                                "Location",
                                "Latitude: ${it.latitude}, Longitude: ${it.longitude}"
                            )
                            // Create a GeoPoint with the location's latitude and longitude
                            val center = GeoPoint(it.latitude, it.longitude)
                            // Set the center of the map view to the new location
                            this@apply.controller.animateTo(center)
                        }
                    }


                    val locationOverlay =
                        MyLocationNewOverlay(GpsMyLocationProvider(this.context), this)
                    locationOverlay.enableMyLocation()
                    locationOverlay.enableFollowLocation()
                    locationOverlay.isDrawAccuracyEnabled = true


                    val x = R.drawable.currentlocation
                    // Convert the drawable resource into a Bitmap
                    val bitmap: Bitmap = BitmapFactory.decodeResource(resources, x)


                    this.getLocalVisibleRect(Rect())
                    this.overlays.add(locationOverlay)
                    this.invalidate()


                    setOnClickListener {
                        // Call the callback to notify the parent about the map change
                        onMapChanged(this)
                    }


                    //maxZoomLevel = 2.0
                }


            },
            update = { view ->



                //view.get(selectedItem)
                //map.zoomController.activate()

                // View's been inflated or state read in this block has been updated
                // Add logic here if necessary

                // As selectedItem is read here, AndroidView will recompose
                // whenever the state changes
                // Example of Compose -> View communication


            }
        )

        // Compose button overlaid on top of the custom view
        LargeFloatingActionButton(
            onClick = { map.controller.zoomTo(18)
                var locationOverlay: MyLocationNewOverlay
                locationOverlay = MyLocationNewOverlay(map)
                for(overlay in map.overlays){
                    Log.d(
                        "Location",
                        "Latitude: ${overlay.toString()}, Longitude: "
                    )
                    if(overlay.toString().contains("MyLocation")){
                        locationOverlay = overlay as MyLocationNewOverlay
                    }
                }
                locationOverlay.enableFollowLocation()
                locationOverlay.isDrawAccuracyEnabled = true

                map.invalidate()
            },
            containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(48.dp)
        ) {
            var recIcon = Icons.Filled.AddCircle


            Icon(recIcon, "Localized description",Modifier.size(32.dp), tint = androidx.compose.ui.graphics.Color.Gray)


        }

    }


}

fun onMapChanged(mapView: MapView) {
    TODO("Not yet implemented")
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppBarExample(map: MapView) {

    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("Top app bar")
                }
            )
        },

        bottomBar = {
            BottomAppBar(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(20.dp)),
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(context, PastTracksViewer::class.java)
                        TrackWriter(context)
                        context.startActivity(intent) }) {
                        Icon(Icons.Filled.List, contentDescription = "Localized description")
                    }
                    IconButton(onClick = { /* do something */ }) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Localized description",
                        )
                    }
                    IconButton(onClick = { /* do something */ }) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = "Localized description",
                        )
                    }
                    IconButton(onClick = { /* do something */ }) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "Localized description",
                        )
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                        },
                        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    ) {
                        var recIcon = Icons.Filled.AddCircle


                        Icon(recIcon, "Localized description",Modifier.size(48.dp), tint = androidx.compose.ui.graphics.Color.Red)


                    }
                }
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .clip(shape = RoundedCornerShape(20.dp)),
            verticalArrangement = Arrangement.spacedBy(16.dp),

            ) {


            CustomView(map)

        }

        //R.id.map
    }
}






