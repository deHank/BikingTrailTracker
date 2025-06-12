package com.example.uitutorial

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MapHomeView(mapViewModel: MapView?, mapViewModel1: MapViewModel) {

    val context = LocalContext.current

    val mapView = remember {

        MapView(context).apply {
            Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))

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


    //val currentLocation by mapViewModel.currentLocation.collectAsState()
    var selectedItem by remember { mutableStateOf(0) }
    Box(modifier = Modifier.fillMaxSize()) {

        AndroidView(modifier = Modifier.fillMaxSize(), factory = { mapView}, update = { view ->
            // This block is for updates that need to be pushed from Compose state to the View.
            // Most map updates are handled by LaunchedEffects now.
            // Example: If you had a Composable state for map rotation, you'd set it here:
            // view.setMapOrientation(someRotationState)
        })

        // Compose button overlaid on top of the custom view
        LargeFloatingActionButton(
            onClick = {
//                mapViewModel.zoomToCurrentLocationAndFollow()
//                mapView.invalidate()
//                currentLocation?.let { loc ->
//                    val geoPoint = GeoPoint(loc.latitude, loc.longitude)
//                    mapView.controller.setCenter(geoPoint)
//                    // You might also want to set a zoom level here if desired when centering
//                    mapView.controller.setZoom(18)
//                    var locationOverlay = mapView.overlays[2] as MyLocationNewOverlay
//                    locationOverlay.enableFollowLocation()
//                    mapView.invalidate() // Redraw the map
//                    Log.d("MapHomeView", "Map centered via button to: ${geoPoint.latitude}, ${geoPoint.longitude}")
//                } ?: run {
//                    Toast.makeText(mapView.context, "No current location available to center map.", Toast.LENGTH_SHORT).show()
//                }

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

    }


}