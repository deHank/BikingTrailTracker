package com.example.uitutorial

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.uitutorial.ui.theme.UITutorialTheme
import org.osmdroid.views.MapView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastTrackDetailScreen (navController: NavHostController, mapViewModel: MapViewModel){

    UITutorialTheme {

        Box(modifier = Modifier.fillMaxSize()) {
            MapPastTrackView(mapViewModel)
        }
    }
}

@Composable
fun item(){
    val context = LocalContext.current
    //Log.d("ClickableItem", "You clicked on file: ${file.name}")
    val itemMapView = remember { MapView(context) }

}