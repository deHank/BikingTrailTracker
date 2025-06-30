package com.example.uitutorial

import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPastTrackView(
    mapViewModel1: MapViewModel,
    fileName: String,
    pastTrackGrabber: PastTrackGrabber
) {

    val context = LocalContext.current
    val liveTrackLocations by mapViewModel1.recordedLocationsFlow.collectAsState() // Observe live recorded locations for drawing
    val lastMapSavedState by mapViewModel1.lastSavedMapState.collectAsState()
    var mapLocations = pastTrackGrabber.getGeoPointsFromFile(context, fileName)


    val mapView = remember {MapView(context) }

        var onFirstLayoutListener = object : MapView.OnFirstLayoutListener {

            override fun onFirstLayout(
                v: View?,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int
            ) {



                val line = Polyline()
                line.setPoints(mapLocations)

                mapView.zoomToBoundingBox(line.bounds, false, 50)
            }
        }

        mapView.apply {

            if(lastMapSavedState != null){
                controller.setCenter(mapViewModel1.lastSavedMapState.value)
                setInitCenter(mapViewModel1.lastSavedMapState.value)
            }
            getInstance().load(context, context.getSharedPreferences("osmdroid", 0))

            setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
            overlays.add(RotationGestureOverlay(this))
            overlays.add(CompassOverlay(context, this).apply { enableCompass() })
            setMultiTouchControls(true)

            val pt = SimplePointTheme(mapLocations, true)

            val opt = SimpleFastPointOverlayOptions.getDefaultStyle().setAlgorithm(
                SimpleFastPointOverlayOptions.RenderingAlgorithm.MAXIMUM_OPTIMIZATION)
            val sfpo = SimpleFastPointOverlay(pt, opt)

            //overlays.add(sfpo)
            val line = Polyline()
            line.setPoints(mapLocations)

            overlays.add(line);
            //overlays.add(2, locationOverlay)
            addOnFirstLayoutListener(onFirstLayoutListener)
            invalidate()

        }



    val scope = rememberCoroutineScope() // Coroutine scope for launching sheet operations
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState() // State for controlling the bottom sheet
    val sessionStats = pastTrackGrabber.getTotalDistanceFromFile(context, fileName)
    //mapView.init(context, PreferenceManager.getDefaultSharedPreferences(context))
    val currentLocation by mapViewModel1.currentLocation.collectAsState()
    BottomSheetScaffold(
        scaffoldState = bottomSheetScaffoldState,
        sheetPeekHeight = 100.dp, // Initial height when collapsed
        sheetContent = {
            // Content of your pull-up sheet
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp) // Max height when expanded (adjust as needed)
                    .padding(16.dp)
                    .animateContentSize() // Animate height changes
            ) {
                Text("Activity Stats", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Total Distance: " + sessionStats.totalDistance, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Avg Pace: " + sessionStats.avgPace, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Moving Time: " + sessionStats.movingTime, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Elevation Gain ", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                // Add a button to expand/collapse the sheet
                Button(
                    onClick = {
                        scope.launch {

                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {

                }
            }
        },
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        content = {
            // Main content area, where your map view sits
            Column(
                modifier = Modifier
                    .padding(it) // Apply Scaffold's padding
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (mapView != null) {
                    // Embed the MapView
                    AndroidView(
                        modifier = Modifier.fillMaxSize(), // Fill all available space
                        factory = { context ->
                            mapView.apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { view ->
                            view.onResume()
                            view.invalidate()
                        }
                    )



                } else {
                    Text("Map View not available without permissions.", modifier = Modifier.padding(16.dp))
                }
            }
        }
    )


}