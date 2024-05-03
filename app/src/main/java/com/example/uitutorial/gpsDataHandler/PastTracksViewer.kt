package com.example.uitutorial.gpsDataHandler

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File

class PastTracksViewer: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)


            setContent{
                BottomAppBarExample(this)
                    
                }
            }

}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomAppBarExample(activity: ComponentActivity) {

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
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                val itemList = getItemList(activity)
                items(itemList) { item ->
                    // Each item is contained within its own clickable block
                    ClickableItem(item = item) { clickedItem ->
                        Log.d("ClickableItem", "You clicked on file: $clickedItem")
                    }
                }
            }
        }

        //R.id.map
    }
}

@Composable
fun ClickableItem(item: String, onClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .padding(16.dp)
            .clickable {
                // Handle item click here
                // For example, you can navigate to another screen or perform some action
            },
        content = {
            Text(
                text = item,
                color = Color.Black
            )
        }
    )
}

fun getItemList(activity: ComponentActivity): List<String> {
    val tracksDir = File(activity.filesDir, "tracks")
    if (!tracksDir.exists()) {
        tracksDir.mkdirs()
    }
    Log.d("PastTracksViewer", "files dir is " + tracksDir.canonicalPath)
    val files = tracksDir.listFiles() ?: emptyArray()
    return files.map { it.name }

}
