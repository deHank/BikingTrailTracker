package com.example.uitutorial

import GPSHandler
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Log
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.gridlines.LatLonGridlineOverlay2
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MapHomeView(map: MapView) {
    var selectedItem by remember { mutableStateOf(0) }
    Box(modifier = Modifier.fillMaxSize()) {

        // Adds view to Compose
        AndroidView(
            modifier = Modifier.fillMaxSize(), // Occupy the max size in the Compose UI tree
            factory = {
                // Creates view

                map.apply {

                    if(map.overlays.isEmpty()) {
                        val overlay = LatLonGridlineOverlay2();
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
                    }

                    //maxZoomLevel = 2.0
                }


            },
            update = { view ->
            }
        )

        // Compose button overlaid on top of the custom view
        LargeFloatingActionButton(
            onClick = { map.controller.zoomTo(18)
                var locationOverlay: MyLocationNewOverlay
                locationOverlay = MyLocationNewOverlay(map)
                for(overlay in map.overlays){

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

            Icon(recIcon, "Localized description", Modifier.size(32.dp), tint = Color.Gray)


        }

    }


}