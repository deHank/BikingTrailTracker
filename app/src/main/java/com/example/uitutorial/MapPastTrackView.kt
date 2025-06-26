package com.example.uitutorial

import android.content.ContentValues.TAG
import android.preference.PreferenceManager
import android.util.DisplayMetrics
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPastTrackView(mapViewModel1: MapViewModel) {

    val context = LocalContext.current
    val liveTrackLocations by mapViewModel1.recordedLocationsFlow.collectAsState() // Observe live recorded locations for drawing
    val lastMapSavedState by mapViewModel1.lastSavedMapState.collectAsState()
    val mapView = remember {

        MapView(context).apply {

            if(lastMapSavedState != null){
                controller.setCenter(mapViewModel1.lastSavedMapState.value)
                setInitCenter(mapViewModel1.lastSavedMapState.value)
            }
            getInstance().load(context, context.getSharedPreferences("osmdroid", 0))

            setTileSource(TileSourceFactory.MAPNIK)
            overlays.add(RotationGestureOverlay(this))
            overlays.add(CompassOverlay(context, this).apply { enableCompass() })
            setMultiTouchControls(true)

            //setup myLocationOverlay
            val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), this)
            locationOverlay.enableMyLocation()
            locationOverlay.enableFollowLocation()
            locationOverlay.isDrawAccuracyEnabled = true

            overlays.add(2, locationOverlay)
            controller.zoomTo(18)
            invalidate()

        }
    }

    //mapView.init(context, PreferenceManager.getDefaultSharedPreferences(context))
    val currentLocation by mapViewModel1.currentLocation.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        DisposableEffect(mapView, liveTrackLocations) {
            getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
            if(liveTrackLocations.isNotEmpty()){
                val polyLine = Polyline(mapView)
                polyLine.setPoints(liveTrackLocations)
                mapView.overlays.add(polyLine)
                mapView.invalidate()
                Log.d(TAG, "Drawing live track polyline with ${liveTrackLocations.size} points.")
                //mapViewModel1.saveLastMapState(liveTrackLocations.getLast())
            }
            else {
                mapView.invalidate()
            }
            onDispose {
                mapView?.overlays?.removeAll(mapView.overlays.filterIsInstance<Polyline>())
                mapView?.invalidate()
            }
        }

        AndroidView(modifier = Modifier.fillMaxSize(), factory = { mapView}, update = { view ->
            // This block is for updates that need to be pushed from Compose state to the View.
            // Most map updates are handled by LaunchedEffects now.
            // Example: If you had a Composable state for map rotation, you'd set it here:
            // view.setMapOrientation(someRotationState)
        })

        // Compose button overlaid on top of the custom view
        LargeFloatingActionButton(
            onClick = {
                mapViewModel1.zoomToCurrentLocationAndFollow()
                mapView.invalidate()
                currentLocation?.let { loc ->
                    val geoPoint = GeoPoint(loc.latitude, loc.longitude)
                    mapView.controller.setCenter(geoPoint)
                    // You might also want to set a zoom level here if desired when centering
                    mapView.controller.setZoom(18)
                    var locationOverlay = mapView.overlays[2] as MyLocationNewOverlay
                    locationOverlay.enableFollowLocation()

                    mapView.invalidate() // Redraw the map
                    Log.d("MapHomeView", "Map centered via button to: ${geoPoint.latitude}, ${geoPoint.longitude}")
                } ?: run {
                    Toast.makeText(mapView.context, "No current location available to center map.", Toast.LENGTH_SHORT).show()
                }

            },
            containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
            elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(48.dp)
        ) {
            var recIcon = Icons.Filled.AddCircle

            Icon(recIcon, "Localized description", Modifier.size(32.dp), tint = Color.Gray)


        }
        val sheetState = rememberModalBottomSheetState()
        val scope = rememberCoroutineScope()
        var showBottomSheet by remember { mutableStateOf(true) }
        val halfScreenHeight = DisplayMetrics()

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    //showBottomSheet = false
                },
                sheetState = sheetState,
            ) {
                // Sheet content - apply fillMaxHeight(0.5f) to the content's root modifier
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.5f) // <--- THIS IS THE KEY CHANGE
                        .padding(16.dp)
                ) {
                    Text("Content taking up half the screen", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                //showBottomSheet = false
                            }
                        }
                    }) {
                        Text("Hide bottom sheet")
                    }
                }
            }
        }

    }


}