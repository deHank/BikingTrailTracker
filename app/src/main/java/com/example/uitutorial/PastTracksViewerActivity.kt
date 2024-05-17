package com.example.uitutorial

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.osmdroid.bonuspack.kml.KmlDocument
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.FolderOverlay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastTracksViewerActivity(navController: NavHostController, map: MapView) {

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
                val fileList = getFileList(context = context)
                items(fileList) { file ->
                    // Each item is contained within its own clickable block
                    ClickableItem(file = file) {
                        Log.d("ClickableItem", "You clicked on file: ${file.name}")
                        var kmlDocument = KmlDocument()
                        kmlDocument.parseKMLFile(file)
                        val kmlOverlay = kmlDocument.mKmlRoot.buildOverlay(map, null, null, kmlDocument) as FolderOverlay
                        map.overlays.add(kmlOverlay)
                        map.invalidate()
                        val bb = kmlDocument.mKmlRoot.getBoundingBox()
                        map.zoomToBoundingBox(bb, true)
                    }
                }
            }
        }
    }
}

@Composable
fun ClickableItem(file: File, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .clickable(onClick = onClick)
    ) {
        Text(text = file.name)
    }
}