package com.example.uitutorial

import android.R.attr.onClick
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.osmdroid.bonuspack.kml.KmlTrack
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastTracksViewerActivity(
    navController: NavHostController,
    pastTrackGrabber: PastTrackGrabber
) {

    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("Past Activities")
                }
            )
        },

        bottomBar = {
            BottomAppBar(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(20.dp)),
                actions = {
                    IconButton(onClick = { navController.navigate("main") }) {
                        Icon(Icons.Filled.Home, contentDescription = "Localized description")
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

                )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .clip(shape = RoundedCornerShape(20.dp))
        ) {
            // Display a list of clickable items using LazyColumn
            LazyColumn()
            {

                val fileList = pastTrackGrabber.getFileList(context = context).reversed().take(5)
                items(fileList) { file ->

                    //Each item is contained within its own clickable block
                    ClickableItem(navController, file = file, pastTrackGrabber = pastTrackGrabber) {
                        Log.d("ClickableItem", "You clicked on file: ${file.name}")
                        pastTrackGrabber.getPointsFromFile(context, file.name)
                        navController.navigate("pastTrackDetailsScreen")
                    }

                }
            }
        }
    }
}

@Composable
fun ClickableItem(navController: NavHostController, file: File, pastTrackGrabber: PastTrackGrabber, onClick: () -> Unit) {
    val context = LocalContext.current

    var points = pastTrackGrabber.getPointsFromFile(context, file.name)
    var geoPoints = pastTrackGrabber.getGeoPointsFromFile(context, file.name)
    // Each item will now contain its own MapView (WARNING: PERFORMANCE RISK)
    val itemMapView = remember { MapView(context) }
    "ClickableItemMap"

    // Load osmdroid config for this specific map view instance
    DisposableEffect(itemMapView) {
        getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        itemMapView.setTileSource(TileSourceFactory.MAPNIK) // Set tile source
        itemMapView.controller.setZoom(1.0) // Initial zoom

        onDispose {
            // Important for embedded MapViews: destroy them when no longer needed
            itemMapView.onPause()
            itemMapView.onDetach() // Clean up internal resources
        }
    }


    var onFirstLayoutListener = object : MapView.OnFirstLayoutListener {

        override fun onFirstLayout(
            v: View?,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int
        ) {



            val line = Polyline()
            line.setPoints(geoPoints)

            itemMapView.zoomToBoundingBox(line.bounds, false, 50)
        }
    }

    Box(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth() // Take full width
            .clickable(onClick = onClick) // Original click to go to main map
    ) {
        Column {
            var fileName = pastTrackGrabber.convertFilenameToReadableDate(file.name)
            //Text(text = "Activity",  style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            Text(text = "Activity on " + fileName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 6.dp))

            // Embed the MapView
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp) // Fixed height for embedded map
                    .clip(RoundedCornerShape(8.dp)), // Rounded corners for aesthetics
                factory = { context ->
                    // Return the pre-configured MapView instance
                    itemMapView.apply {
                        // Ensure layout params are set for Android View in Compose
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        val pt = SimplePointTheme(points, true)

                        //val textStyle = Paint()

                        //textStyle = 16f
                        val opt = SimpleFastPointOverlayOptions.getDefaultStyle().setAlgorithm(
                            SimpleFastPointOverlayOptions.RenderingAlgorithm.MAXIMUM_OPTIMIZATION)
                        val sfpo = SimpleFastPointOverlay(pt, opt)

                        overlays.add(sfpo)
                        val boundingBox = pastTrackGrabber.computeArea(context, geoPoints)
                        val line = Polyline()
                        line.setPoints(geoPoints)

                        overlays.add(line);



                        addOnFirstLayoutListener(onFirstLayoutListener)
                        //zoomToBoundingBox(boundingBox, true)
                        //z = 16.0f
                        //invalidate()
                    }





                },
                update = { view ->
                    // Optional: Update logic here if needed, but DisposableEffect handles drawing
                    view.onResume() // Resume map lifecycle when view is active
                    view.invalidate() // Ensure it redraws
                }
            )
        }
    }
}