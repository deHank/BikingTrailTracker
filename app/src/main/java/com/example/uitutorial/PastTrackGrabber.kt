package com.example.uitutorial

import android.content.Context
import android.util.Log
import com.garmin.fit.Decode
import com.garmin.fit.MesgBroadcaster
import com.garmin.fit.MesgListener
import com.garmin.fit.RecordMesg
import com.garmin.fit.util.SemicirclesConverter
import org.osmdroid.api.IGeoPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.simplefastpoint.LabelledGeoPoint
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale


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

    /**
     * Converts a filename in "activity_YYYYMMDD_HHMMSS.fit" format to a readable date string.
     *
     * @param fileName The input filename.
     * @param outputPattern The desired output date format (e.g., "MMM dd, yyyy HH:mm:ss").
     * @return A formatted date string, or the original filename if parsing fails.
     */
    fun convertFilenameToReadableDate(fileName: String, outputPattern: String = "MMM dd, yyyy HH:mm"): String {
        // Regex to extract the timestamp part "YYYYMMDD_HHMMSS"
        val regex = "activity_(\\d{8}_\\d{6})\\.fit".toRegex()
        val matchResult = regex.find(fileName)

        val timestampString = matchResult?.groups?.get(1)?.value

        if (timestampString == null) {
            Log.w(TAG, "Filename does not match expected pattern: $fileName")
            return fileName // Return original if format doesn't match
        }

        val inputFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val outputFormat = SimpleDateFormat(outputPattern, Locale.getDefault())

        return try {
            val date = inputFormat.parse(timestampString)
            if (date != null) {
                outputFormat.format(date)
            } else {
                Log.e(TAG, "Failed to parse timestamp string: $timestampString")
                fileName
            }
        } catch (e: ParseException) {
            Log.e(TAG, "Error parsing date from filename: $fileName - ${e.message}")
            fileName // Return original on parse error
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in date conversion: $fileName - ${e.message}")
            fileName
        }
    }

    fun getGeoPointsFromFile(context: Context, fileName: String): List<GeoPoint> {
        val points = mutableListOf<GeoPoint>()
        val tracksDir = File(context.filesDir, "tracks")
        val fitFile = File(tracksDir, fileName)
        Log.d(TAG, "FIT file path: ${fitFile.absolutePath}")
        if (!fitFile.exists()) {
            Log.e(TAG, "FIT file not found: ${fitFile.absolutePath}")
            return emptyList()
        }
        val messageListner = MesgListener { mesg ->

            if(mesg.name == "record") {
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

                    points.add(
                        LabelledGeoPoint(
                            latitudeDegrees.toDouble(),
                            longitudeDegrees.toDouble()
                        )
                    )
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
        try {
            messageBroadcaster.run(fileInputStream)
        }
        catch (err: Exception) {
            Log.e(TAG, "Error reading FIT file: ${err.message}")
        }
        return points
    }
    fun getPointsFromFile(context: Context, fileName: String): List<IGeoPoint> {
        val points = mutableListOf<IGeoPoint>()
        val tracksDir = File(context.filesDir, "tracks")
        val fitFile = File(tracksDir, fileName)
        Log.d(TAG, "FIT file path: ${fitFile.absolutePath}")
        if (!fitFile.exists()) {
            Log.e(TAG, "FIT file not found: ${fitFile.absolutePath}")
            return emptyList()
        }
        val messageListner = MesgListener { mesg ->

            if(mesg.name == "record") {
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

                    points.add(
                        LabelledGeoPoint(
                            latitudeDegrees.toDouble(),
                            longitudeDegrees.toDouble()
                        )
                    )
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
        try {
            messageBroadcaster.run(fileInputStream)
        }catch (err: Exception) {
            Log.e(TAG, "Error reading FIT file: ${err.message}")
        }
        return points
    }

    fun computeArea(context: Context, points: List<IGeoPoint>): BoundingBox {
        var nord = 0.0
        var sud = 0.0
        var ovest = 0.0
        var est = 0.0

        for (point in points) {
            if (point.latitude > nord) {
                nord = point.latitude
            }
            if (point.latitude < sud) {
                sud = point.latitude
            }
            if (point.longitude < ovest) {
                ovest = point.longitude
            }
            if (point.longitude > est) {
                est = point.longitude
            }
        }
        return BoundingBox(nord, est, sud, ovest)
    }

}