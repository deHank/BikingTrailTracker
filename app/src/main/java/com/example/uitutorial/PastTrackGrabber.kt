package com.example.uitutorial

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.garmin.fit.Decode
import com.garmin.fit.Decoder
import com.garmin.fit.Mesg
import com.garmin.fit.MesgBroadcaster
import com.garmin.fit.MesgListener
import com.garmin.fit.RecordMesg
import com.garmin.fit.RecordMesgListener
import com.garmin.fit.util.SemicirclesConverter
import java.io.File

/* This class is used to grab a list of tracks to send to the PastTracksViewerActivity */
class PastTrackGrabber {

    private val TAG = "PastTrackGrabber"
    // --- Helper function (can be in a separate file or companion object) ---
    /**
     * Helper function to retrieve a list of files from app's internal storage.
     */
    fun getFileList(context: Context): List<File> {
        val tracksDir = File(context.filesDir, "tracks")
        if (!tracksDir.exists()) {
            tracksDir.mkdirs()
        }
        Log.d("PastTracksViewer", "files dir is " + tracksDir.canonicalPath)
        return tracksDir.listFiles()?.toList() ?: emptyList()
    }

    fun getPointsFromFile(context: Context, fileName: String): List<Pair<Double, Double>> {
        val points = mutableListOf<Pair<Double, Double>>()
        val tracksDir = File(context.filesDir, "tracks")
        val fitFile = File(tracksDir, fileName)
        Log.d(TAG, "FIT file path: ${fitFile.absolutePath}")
        if (!fitFile.exists()) {
            Log.e(TAG, "FIT file not found: ${fitFile.absolutePath}")
            return emptyList()
        }
        val messageListner = MesgListener { mesg ->

            if(mesg.hasField(RecordMesg.PositionLatFieldNum) && mesg.hasField(RecordMesg.PositionLongFieldNum)) {
                val latitudeSemicircles: Number = mesg?.getFieldValue(RecordMesg.PositionLatFieldNum) as Number
                val longitudeSemicircles: Number = mesg?.getFieldValue(RecordMesg.PositionLongFieldNum) as Number
                //points.add(Pair(latitudeSemicircles!!, longitudeSemicircles!!))
                //Toast.makeText(context, "Latitude: $latitudeSemicircles, Longitude: $longitudeSemicircles", Toast.LENGTH_SHORT).show()

                Log.d(TAG, "Latitude: $latitudeSemicircles, Longitude: $longitudeSemicircles")
                val latitudeDegrees = SemicirclesConverter.semicirclesToDegrees(latitudeSemicircles.toInt())
                val longitudeDegrees = SemicirclesConverter.semicirclesToDegrees(
                    longitudeSemicircles.toInt()
                )
                if ((latitudeDegrees != 0.0 || longitudeDegrees != 0.0) && // Exclude (0,0)
                    latitudeDegrees >= -90.0f && latitudeDegrees <= 90.0f &&
                    longitudeDegrees >= -180.0f && longitudeDegrees <= 180.0f) {

                    points.add(Pair(latitudeDegrees.toDouble(), longitudeDegrees.toDouble()))
                     Log.d(TAG, "Extracted valid point: Lat $latitudeDegrees, Lon $longitudeDegrees")
                } else {
                    Log.w(TAG, "Filtered out invalid/out-of-range coordinate: Lat $latitudeDegrees, Lon $longitudeDegrees (semicircles: $latitudeSemicircles, $longitudeSemicircles)")
                }
            }
        }
        val decode = Decode()
        var fileInputStream = fitFile.inputStream()
        decode.addListener(messageListner)
        var messageBroadcaster = MesgBroadcaster()
        messageBroadcaster.addListener(messageListner)
        messageBroadcaster.run(fileInputStream)

        return points
    }

}