package com.example.uitutorial

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.example.uitutorial.ui.theme.UITutorialTheme
import org.osmdroid.views.MapView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastTrackDetailsScreen (
    navController: NavHostController,
    mapViewModel: MapViewModel,
    fileName: String,
    pastTrackGrabber: PastTrackGrabber
){

    UITutorialTheme {

        Box(modifier = Modifier.fillMaxSize()) {
            MapPastTrackView(mapViewModel,fileName,
                pastTrackGrabber )
        }
    }
}

@Composable
fun item(){
    val context = LocalContext.current
    //Log.d("ClickableItem", "You clicked on file: ${file.name}")
    val itemMapView = remember { MapView(context) }

}