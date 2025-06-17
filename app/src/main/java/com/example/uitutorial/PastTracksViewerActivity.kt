package com.example.uitutorial

import android.preference.PreferenceManager
import android.util.Log
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
import org.osmdroid.config.Configuration.getInstance
import org.osmdroid.views.MapView
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastTracksViewerActivity(
    navController: NavHostController,
    map: MapView?,
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
                    Text("Top app bar")
                }
            )
        },

        bottomBar = {
            BottomAppBar(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(20.dp)),
                actions = {
                    IconButton(onClick = { /* do something */ }) {
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
                val fileList = pastTrackGrabber.getFileList(context = context)
                items(fileList) { file ->
                    // Each item is contained within its own clickable block
                    ClickableItem(file = file) {
                        Log.d("ClickableItem", "You clicked on file: ${file.name}")
                        pastTrackGrabber.getPointsFromFile(context, file.name)
                    }
                }
            }
        }
    }
}

@Composable
fun ClickableItem(file: File, onClick: () -> Unit) {
    val context = LocalContext.current
    // Each item will now contain its own MapView (WARNING: PERFORMANCE RISK)
    val itemMapView = remember { MapView(context) }
    "ClickableItemMap"

    // Load osmdroid config for this specific map view instance
    DisposableEffect(itemMapView) {
        getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        itemMapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK) // Set tile source
        itemMapView.controller.setZoom(1.0) // Initial zoom
        onDispose {
            // Important for embedded MapViews: destroy them when no longer needed
            itemMapView.onPause()
            itemMapView.onDetach() // Clean up internal resources
        }
    }

    Box(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth() // Take full width
            .clickable(onClick = onClick) // Original click to go to main map
    ) {
        Column {
            Text(text = file.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
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