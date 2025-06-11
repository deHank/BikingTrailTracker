package com.example.uitutorial

import GPSHandler
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.uitutorial.ui.theme.UITutorialTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.views.MapView
import java.io.File


class MainActivity : ComponentActivity() {

    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private lateinit var map : MapView

    private lateinit var locationManager: LocationManager

    private lateinit var locationHandler: GPSHandler

    private lateinit var location: Location
    private lateinit var handler: Handler
    private var permissionCheckCompleted: MutableState<Boolean> = mutableStateOf(false)



    // Using Activity Result API for cleaner permission handling (recommended for modern Android)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineLocationGranted || coarseLocationGranted) {
                // Permissions are granted, proceed with location-dependent operations
                Toast.makeText(this, "Location permissions granted!", Toast.LENGTH_SHORT).show()
                //startFitFileGeneration()
                permissionCheckCompleted.value = true
            } else {
                // Permissions denied, inform the user
                Toast.makeText(this, "Location permissions denied.", Toast.LENGTH_LONG).show()
                permissionCheckCompleted.value = true
            }
        }


    /**
     * Checks if location permissions are granted. If not, requests them.
     */
    private fun checkAndRequestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permissions are already granted
            //startFitFileGeneration()
        } else {
            // Permissions are not granted, request them
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Request both fine and coarse location permissions
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
                permissionCheckCompleted.value = true
            } else {
                // For API < 23, permissions are granted at install time.
                // You might still want to inform the user or handle older device specifics.
                permissionCheckCompleted.value = true            }
        }
    }


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {

        checkAndRequestLocationPermissions()

       // locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000, .01f, locationListener)

        if(permissionCheckCompleted.value) {

            map = MapView(this)
            val trackWriter = TrackWriter(map)
            getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))


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
                UITutorialTheme {
                    // Initialize the NavController
                    val navController = rememberNavController()

                    // Define the navigation graph
                    NavHost(navController, startDestination = "main") {
                        composable("main") {
                            // Your main screen content
                            BottomAppBarExample(navController, map, trackWriter)
                        }
                        composable("pastTracksViewer") {
                            // Content for the Past Tracks Viewer screen
                            PastTracksViewerActivity(navController, map)
                        }
                        composable("CurrentTrackViewerActivity") {
                            CurrentTrackViewerActivity(navController, trackWriter)
                        }
                    }
                }
            }
            map.controller.setZoom(1.0)
            goToCurrentLocation()

        }
    }

    fun goToCurrentLocation(){


    }

    fun getFileList(): List<File> {
        val tracksDir = File(this.filesDir, "tracks")
        if (!tracksDir.exists()) {
            tracksDir.mkdirs()
        }
        Log.d("PastTracksViewer", "files dir is " + tracksDir.canonicalPath)
        return tracksDir.listFiles()?.toList() ?: emptyList()
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

    override fun onBackPressed() {

        // Call super method to handle normal back button behavior
        super.onBackPressed()
    }

}


fun getFileList(context: Context): List<File> {
        val tracksDir = File(context.filesDir, "tracks")
        if (!tracksDir.exists()) {
            tracksDir.mkdirs()
        }
        Log.d("PastTracksViewer", "files dir is " + tracksDir.canonicalPath)
        return tracksDir.listFiles()?.toList() ?: emptyList()
}




fun onMapChanged(mapView: MapView) {
    TODO("Not yet implemented")
}


@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppBarExample(navController: NavHostController, map: MapView, trackWriter: TrackWriter) {

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
                        map.setDestroyMode(false)

                        navController.navigate("pastTracksViewer")
                        map.invalidate()
                        }) {
                        Icon(Icons.Filled.List, contentDescription = "view past tracks")
                    }
                    IconButton(onClick = { map.setDestroyMode(false)

                        navController.navigate("CurrentTrackViewerActivity")
                        map.invalidate() }) {

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
                            Log.d("Floating red Action Button" , "Button was pressed")
                            for(overlay in map.overlays){

                                if(!overlay.toString().contains("MyLocation")){
                                    map.overlays.remove(overlay)
                                }
                            }
                            map.setDestroyMode(false)
                            val constraints = Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()

                            val trackWriterWorker = OneTimeWorkRequestBuilder<TrackWriterWorker>()
                                .setConstraints(constraints)
                                .setInputData(Data.Builder().putString("mapViewKey", map.toString()).build()) // Pass the MapView instance here
                                .build()

                            WorkManager.getInstance(context).enqueue(trackWriterWorker)


                            navController.navigate("CurrentTrackViewerActivity")

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


            MapHomeView(map)

        }

        //R.id.map
    }
}






