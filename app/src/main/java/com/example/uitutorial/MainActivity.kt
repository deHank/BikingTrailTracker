package com.example.uitutorial

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
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
import com.example.uitutorial.ui.theme.UITutorialTheme
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.views.MapView
import java.io.File

//region Dummy Implementations for classes not provided in the original snippet
// These are added to make the MainActivity code compile and run for demonstration.
// In a real project, these would be in their own respective files.


/**
 * Dummy implementation of TrackWriter.
 * In a real app, this would handle recording GPS data and potentially
 * interacting with the MapView to draw the current track.
 */



/**
 * Dummy Composable for the Current Track Viewer screen.
 */
@Composable
fun CurrentTrackViewerActivity(navController: NavHostController, trackWriter: TrackWriter?) { // TrackWriter is now nullable here too
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Current Track Recording", style = MaterialTheme.typography.headlineMedium)
        if (trackWriter != null) {
            Button(onClick = {  }) {
                Text("Start Recording")
            }
            Button(onClick = {  }) {
                Text("Stop Recording")
            }
        } else {
            Text("Tracking not available without permissions.", modifier = Modifier.padding(16.dp))
        }
        Button(onClick = { navController.popBackStack() }) {
            Text("Go Back")
        }
    }
}




class MainActivity : ComponentActivity() {

    // Using Activity Result API is generally preferred for runtime permissions.
    // The old onRequestPermissionsResult callback is also kept for completeness but might not be strictly needed.
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1

    // Map and TrackWriter are now nullable and initialized only after permissions are granted.
    private var map: MapView? = null
    private var trackWriter: TrackWriter? = null

    // State variables to control UI rendering based on permission status.
    private var isLocationPermissionGranted: MutableState<Boolean> = mutableStateOf(false)
    private var permissionCheckCompleted: MutableState<Boolean> = mutableStateOf(false)


    // Activity Result Launcher for requesting multiple permissions.
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            // Update the state based on the user's decision.
            if (fineLocationGranted || coarseLocationGranted) {
                Toast.makeText(this, "Location permissions granted!", Toast.LENGTH_SHORT).show()
                isLocationPermissionGranted.value = true
                setupCoreAppComponents() // Initialize core components now that permissions are granted.
            } else {
                Toast.makeText(this, "Location permissions denied. App functionality is limited.", Toast.LENGTH_LONG).show()
                isLocationPermissionGranted.value = false
            }
            permissionCheckCompleted.value = true // Permission check process is now complete.
        }

    // Using `by viewModels()` with a custom ViewModelFactory because MapViewModel now
    // has a constructor parameter (TrackWriter).
    private val mapViewModel: MapViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
                    // Ensure trackWriter is not null here. setupCoreAppComponents guarantees it.
                    // This factory will only be called after setupCoreAppComponents.
                    @Suppress("UNCHECKED_CAST")
                    return MapViewModel(application, trackWriter!!) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }

    /**
     * Checks if location permissions are already granted. If not, requests them.
     * This method is called once at the start of the activity to initiate the permission flow.
     */
    private fun checkAndRequestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permissions are already granted.
            isLocationPermissionGranted.value = true
            permissionCheckCompleted.value = true
            setupCoreAppComponents() // Initialize core components immediately.
        } else {
            // Permissions are not granted. Request them using the Activity Result API.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else {
                // For API < 23, dangerous permissions are granted at install time.
                // In a real app, you might consider your min SDK version.
                // For this example, we assume permissions are effectively granted on older APIs.
                isLocationPermissionGranted.value = true
                permissionCheckCompleted.value = true
                setupCoreAppComponents()
            }
        }
    }


    @SuppressLint("MissingPermission") // Permissions are checked via checkAndRequestLocationPermissions and Composables
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the content with Jetpack Compose.
        // The UI will react to changes in `permissionCheckCompleted` and `isLocationPermissionGranted`.
        setContent {
            UITutorialTheme {
                // Use a Box to center the permission screens or fill the layout.
                Box(modifier = Modifier.fillMaxSize()) {
                    if (permissionCheckCompleted.value) {
                        if (isLocationPermissionGranted.value) {
                            // Permissions granted: Show the main app content (navigation graph).
                            // Ensure osmdroid configuration is loaded if not already.
                            // This might be redundant if setupCoreAppComponents() already did it.
                            getInstance().load(LocalContext.current, PreferenceManager.getDefaultSharedPreferences(LocalContext.current))

                            val navController = rememberNavController()
                            NavHost(navController, startDestination = "main") {
                                composable("main") {
                                    // Use 'let' to safely unwrap nullable map and trackWriter.
                                    // If for some reason they are null here (shouldn't happen with `setupCoreAppComponents`),
                                    // a fallback Text is displayed.
                                    map?.let { mapInstance ->
                                        trackWriter?.let { trackWriterInstance ->
                                            BottomAppBarExample(navController, mapInstance, mapViewModel = mapViewModel)
                                        } ?: Text("Error: TrackWriter not initialized.", modifier = Modifier.align(Alignment.Center))
                                    } ?: Text("Error: MapView not initialized.", modifier = Modifier.align(Alignment.Center))
                                }
                                composable("pastTracksViewer") {
                                    map?.let { mapInstance ->
                                        PastTracksViewerActivity(navController, mapInstance)
                                    } ?: Text("Error: MapView not initialized.", modifier = Modifier.padding(16.dp).align(Alignment.Center))
                                }
                                composable("CurrentTrackViewerActivity"){
                                    trackWriter?.let { trackWriterInstance ->
                                        CurrentTrackViewerActivity(navController , trackWriterInstance)
                                    } ?: Text("Error: TrackWriter not initialized.", modifier = Modifier.padding(16.dp).align(Alignment.Center))
                                }
                            }
                        } else {
                            // Permissions denied: Show a screen indicating denial with a retry option.
                            PermissionDeniedScreen {
                                // When the retry button is clicked, re-initiate the permission check.
                                checkAndRequestLocationPermissions()
                            }
                        }
                    } else {
                        // Permissions check in progress: Show a loading indicator.
                        PermissionCheckingScreen()
                    }
                }
            }
        }

        // Initiate the permission check when the Activity is created.
        // This will trigger the UI to show either loading, denied, or main content.
        checkAndRequestLocationPermissions()
    }

    /**
     * Initializes core application components (MapView, TrackWriter) that
     * require location permissions. This method is called ONLY after
     * location permissions have been successfully granted.
     */
    private fun setupCoreAppComponents() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        // Ensure components are only initialized once.
        if (map == null) {
            // Initialize MapView
            map = MapView(this)
            // Initialize TrackWriter, passing the non-null MapView instance.
            //trackWriter = TrackWriter(map) // !! is safe here because we just assigned `map`
            // Load osmdroid configuration.
            getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

            // Set initial map properties after initialization.
            map!!.controller.setZoom(1.0)
            // Call the actual logic for going to the current location.
            // This will likely involve a LocationManager or FusedLocationProviderClient.
            goToCurrentLocation()
        }
        trackWriter = TrackWriter(context = applicationContext, locationManager)
    }

    /**
     * Placeholder for actual logic to go to the device's current location on the map.
     * In a real app, this would involve using Android's location services.
     */
    fun goToCurrentLocation(){
        // Example: To get the actual current location, you would need
        // a LocationManager instance and request updates or last known location.
        // For demonstration, this remains a placeholder.
        Log.d("MainActivity", "goToCurrentLocation called. Implement actual location logic here.")
    }

    /**
     * Helper function to get a list of track files from app's internal storage.
     */
    fun getFileList(): List<File> {
        val tracksDir = File(this.filesDir, "tracks")
        if (!tracksDir.exists()) {
            tracksDir.mkdirs()
        }
        Log.d("PastTracksViewer", "files dir is " + tracksDir.canonicalPath)
        return tracksDir.listFiles()?.toList() ?: emptyList()
    }

    // This @Composable function `CheckLocationPermission` was for a different pattern
    // (traditional `ActivityCompat.requestPermissions` with a Composable wrapper).
    // Given the current `ActivityResultContracts.RequestMultiplePermissions` setup in MainActivity,
    // this specific composable might not be directly used anymore for the main flow.
    @Composable
    fun CheckLocationPermission(onPermissionGranted: @Composable () -> Unit) {
        // This function's logic is largely superseded by the `requestPermissionLauncher` pattern.
        // If it's not explicitly called, it can be removed to reduce confusion.
        // For now, it's kept as-is, but note its limited role in this updated flow.
        val permissionState = remember { ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) }
        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            onPermissionGranted()
        } else {
            // Note: Calling requestPermissions directly from a Composable can be problematic
            // in some scenarios. ActivityResultContracts is preferred.
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    // The traditional `onRequestPermissionsResult` callback is part of `ComponentActivity`.
    // It will still be called by the system, even if you use `ActivityResultContracts`.
    // It's good practice to keep it if you have other permission requests not handled by `ActivityResultContracts`.
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // If you rely solely on `requestPermissionLauncher` for location, this block
        // for `REQUEST_PERMISSIONS_REQUEST_CODE` might not be strictly necessary for location.
        // However, if you have other permission requests using this old API, keep it.
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Permission granted via old API callback!")
                // You might need to update state variables here if this path is the primary one
                // for some permissions, to ensure UI reflects the new state.
            } else {
                Log.d("MainActivity", "Permission denied via old API callback!")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure map is not null before calling its lifecycle methods.
        map?.onResume()
    }

    override fun onPause() {
        super.onPause()
        // Ensure map is not null before calling its lifecycle methods.
        map?.onPause()
    }

    override fun onBackPressed() {
        // Call super method to handle normal back button behavior.
        // For Jetpack Compose navigation, you might want to handle it with `navController.popBackStack()`.
        super.onBackPressed()
    }
}


// --- Composable functions for permission status screens ---
/**
 * Composable screen displayed while location permissions are being checked.
 */
@Composable
fun PermissionCheckingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text("Checking location permissions...", modifier = Modifier.padding(top = 16.dp))
        }
    }
}

/**
 * Composable screen displayed when location permissions are denied.
 */
@Composable
fun PermissionDeniedScreen(onRetryClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Location permissions are required for this app's core features. Please grant permissions to continue.",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Button(onClick = onRetryClick) {
            Text("Grant Permissions")
        }
    }
}

// --- Helper function (can be in a separate file or companion object) ---
/**
 * Helper function to retrieve a list of files from app's internal storage.
 */
fun getFileList(context: Context): List<File> {
    val tracksDir = File(context.filesDir, "tracks")
    if (!tracksDir.exists()) {
        tracksDir.mkdirs()
    }
    Log.d("PastTracksViewer", "files dir is " + tracksDir.canonicalPath)
    return tracksDir.listFiles()?.toList() ?: emptyList()
}

/**
 * Placeholder for map change listener.
 */
fun onMapChanged(mapView: MapView) {
    // TODO: Not yet implemented
}

/**
 * Main application UI with a BottomAppBar and content area.
 * This Composable now safely receives non-null MapView and TrackWriter instances.
 */
@SuppressLint("MissingPermission") // Suppress lint warning because permissions are checked at a higher level
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppBarExample(navController: NavHostController, map: MapView?, mapViewModel: MapViewModel) { // Marked map and trackWriter as nullable
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
                modifier = Modifier.clip(shape = RoundedCornerShape(20.dp)),
                actions = {
                    IconButton(onClick = {
                        map?.setDestroyMode(false) // Safe call
                        navController.navigate("pastTracksViewer")
                        map?.invalidate() // Safe call
                    }) {
                        Icon(Icons.Filled.List, contentDescription = "view past tracks")
                    }
                    IconButton(onClick = {
                        map?.setDestroyMode(false) // Safe call
                        navController.navigate("CurrentTrackViewerActivity")
                        map?.invalidate() // Safe call
                    }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Localized description")
                    }
                    IconButton(onClick = { /* do something */ }) {
                        Icon(Icons.Filled.Check, contentDescription = "Localized description")
                    }
                    IconButton(onClick = { /* do something */ }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Localized description")
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            Log.d("Floating red Action Button", "Button was pressed")
                            mapViewModel.startRecordingAndSaveFile()
                            // Safely remove overlays. `map?.overlays` returns null if map is null.
                            // If `map.overlays` is null, `filter` and `removeAll` won't be called.
                            map?.let {
                                val overlaysToRemove = it.overlays.filter { overlay -> !overlay.toString().contains("MyLocation") }
                                it.overlays.removeAll(overlaysToRemove)
                                it.setDestroyMode(false)
                            }

                            val constraints = Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()

                            // It's generally not recommended to pass UI components (like MapView) to Workers directly.
                            // Instead, pass necessary data (e.g., activity ID, parameters for tracking).
                            val trackWriterWorker = OneTimeWorkRequestBuilder<TrackWriterWorker>()
                                .setConstraints(constraints)
                                .setInputData(Data.Builder().putString("mapViewKey", "mapViewInstance").build())
                                .build()
//
//                            WorkManager.getInstance(context).enqueue(trackWriterWorker)
                            //navController.navigate("CurrentTrackViewerActivity")
                        },
                        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    ) {
                        val recIcon = Icons.Filled.AddCircle
                        Icon(recIcon, "Localized description", Modifier.size(48.dp), tint = Color.Red)
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
            MapHomeView(map, mapViewModel)
        }
    }
}