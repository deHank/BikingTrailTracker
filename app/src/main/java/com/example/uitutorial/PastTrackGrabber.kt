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
                val latitudeSemicircles = mesg?.getFieldValue(RecordMesg.PositionLatFieldNum) as Int
                val longitudeSemicircles = mesg?.getFieldValue(RecordMesg.PositionLongFieldNum) as Int
                //points.add(Pair(latitudeSemicircles!!, longitudeSemicircles!!))
                //Toast.makeText(context, "Latitude: $latitudeSemicircles, Longitude: $longitudeSemicircles", Toast.LENGTH_SHORT).show()

                Log.d(TAG, "Latitude: $latitudeSemicircles, Longitude: $longitudeSemicircles")
                val convertedLatitude = SemicirclesConverter.semicirclesToDegrees(latitudeSemicircles!!)
                val convertedLongitude = SemicirclesConverter.semicirclesToDegrees(longitudeSemicircles!!)
                points.add(Pair(convertedLatitude, convertedLongitude))
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