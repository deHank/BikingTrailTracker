import android.content.Context
import android.location.Location
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.random.Random

class FitFileWriter(private val context: Context) {
    private val TAG = "FitFileWriter"
    private val MANUFACTURER_ID = com.garmin.fit.Manufacturer.DEVELOPMENT
    private val PRODUCT_ID = 12345 // Your unique product ID for your app/device

    private var fileOutputStream: FileOutputStream? = null
    private var fileEncoder: com.garmin.fit.FileEncoder? = null
    private var fileCreationTime: com.garmin.fit.DateTime? = null
    private var firstLocationTime: com.garmin.fit.DateTime? = null

    // Cumulative metrics for Session and Lap messages
    private var cumulativeDistance: Float = 0.0F
    private var totalTimerTime: Float = 0.0F
    private var totalElapsedTime: Float = 0.0F
    private var minHeartRate: Short = 255
    private var maxHeartRate: Short = 0
    private var minSpeed: Float = Float.MAX_VALUE
    private var maxSpeed: Float = 0.0F
    private var totalCalories: Int = 0
    private var previousLocation: Location? = null // To calculate segment distance

    /**
     * Starts a new FIT file writing session. Initializes the file and writes
     * the mandatory FileId and Event.START messages.
     *
     * @param fileName The name of the FIT file to create.
     * @param sport The primary sport type for this activity.
     */
    fun startNewFitFile(fileName: String, sport: com.garmin.fit.Sport = com.garmin.fit.Sport.GENERIC) {
        if (fileEncoder != null) {
            Log.w(TAG, "File encoder already active. Call endFitFile() before starting a new one.")
            return
        }

        val file = File(context.filesDir, fileName)
        try {
            fileOutputStream = FileOutputStream(file)
            fileEncoder = com.garmin.fit.FileEncoder(file, com.garmin.fit.Fit.ProtocolVersion.V2_0)

            // Reset cumulative metrics for a new session
            cumulativeDistance = 0.0F
            totalTimerTime = 0.0F
            totalElapsedTime = 0.0F
            minHeartRate = 255
            maxHeartRate = 0
            minSpeed = Float.MAX_VALUE
            maxSpeed = 0.0F
            totalCalories = 0
            previousLocation = null
            firstLocationTime = null // Will be set with the first record

            // --- FileId Message (Mandatory first message) ---
            fileCreationTime = com.garmin.fit.DateTime(System.currentTimeMillis() / 1000L)
            val fileIdMesg = com.garmin.fit.FileIdMesg()
            fileIdMesg.setType(com.garmin.fit.File.ACTIVITY)
            fileIdMesg.setManufacturer(MANUFACTURER_ID)
            fileIdMesg.setProduct(PRODUCT_ID)
            fileIdMesg.setSerialNumber(System.currentTimeMillis()) // Use current time as a non-unique serial
            fileIdMesg.setTimeCreated(fileCreationTime)
            fileEncoder?.write(fileIdMesg)
            Log.d(TAG, "FIT file writing started: FileIdMesg written.")

            // --- Event Message (Timer Start) ---
            // Timestamp for this event will be the first location's timestamp or file creation time if no locations yet.
            // Will use first location's timestamp in addRecord for accuracy. For now, use file creation time.
            val eventMesgStart = com.garmin.fit.EventMesg()
            eventMesgStart.setTimestamp(fileCreationTime)
            eventMesgStart.setEvent(com.garmin.fit.Event.TIMER)
            eventMesgStart.setEventType(com.garmin.fit.EventType.START)
            fileEncoder?.write(eventMesgStart)
            Log.d(TAG, "Event.TIMER START written.")

            Toast.makeText(context, "Started writing FIT file: $fileName", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "Error starting new FIT file: ${e.message}", e)
            Toast.makeText(context, "Error starting FIT file: ${e.message}", Toast.LENGTH_LONG).show()
            // Clean up if initialization fails
            fileEncoder = null
            fileOutputStream = null
            previousLocation = null
        }
    }

    /**
     * Adds a new location record to the ongoing FIT file.
     * Call this for each new GPS update received.
     *
     * @param location The new Location object from GPSHandler.
     * @param heartRate Optional heart rate value.
     * @param cadence Optional cadence value.
     * @param power Optional power value.
     * @param temperature Optional temperature value.
     */
    fun addRecord(
        location: Location,
        heartRate: Short? = null,
        cadence: Short? = null,
        power: Short? = null,
        temperature: Short? = null
    ) {
        if (fileEncoder == null) {
            Log.e(TAG, "File encoder not initialized. Call startNewFitFile() first.")
            return
        }

        if (firstLocationTime == null) {
            firstLocationTime = com.garmin.fit.DateTime(location.time / 1000L)
        }

        val recordMesg = com.garmin.fit.RecordMesg()
        recordMesg.setTimestamp(com.garmin.fit.DateTime(location.time / 1000L))

        // GPS Data
        //recordMesg.setPositionLat(com.garmin.fit.Fit.floatToSemicircles(location.latitude.toFloat()))
        //recordMesg.setPositionLong(com.garmin.fit.Fit.floatToSemicircles(location.longitude.toFloat()))
        if (location.hasAltitude()) {
            recordMesg.setAltitude(location.altitude.toFloat())
        }

        // Speed and Distance calculation
        recordMesg.setSpeed(location.speed) // m/s
        if (location.speed > maxSpeed) maxSpeed = location.speed
        if (location.speed < minSpeed) minSpeed = location.speed

        if (previousLocation != null) {
            val segmentDistance = location.distanceTo(previousLocation!!) // meters
            cumulativeDistance += segmentDistance
            val timeDiff = (location.time - previousLocation!!.time) / 1000.0 // seconds
            totalElapsedTime += timeDiff.toFloat()
            if (location.hasSpeed() && location.speed > 0) {
                totalTimerTime += timeDiff.toFloat()
            } else {
                // If no speed, assume moving during the time difference for timer time
                totalTimerTime += timeDiff.toFloat()
            }
        }
        recordMesg.setDistance(cumulativeDistance)

        // Sensor Data (use provided, else dummy/null)
        //val currentHr = heartRate ?: (100 + Random().nextInt(60)).toShort()
//        recordMesg.setHeartRate(currentHr)
//        minHeartRate = minOf(minHeartRate, currentHr)
//        maxHeartRate = maxOf(maxHeartRate, currentHr)
//
//        //recordMesg.setCadence(cadence ?: (70 + Random().nextInt(20)).toShort())
//        //recordMesg.setPower(power ?: (150 + Random().nextInt(100)).toShort())
//        //recordMesg.setTemperature(temperature ?: (20 + Random().nextInt(5)).toShort())
//
//        totalCalories += (currentHr * 0.05).toInt() // Simplified calorie approx

        fileEncoder?.write(recordMesg)
        previousLocation = location // Update previous location for next calculation
        Log.d(TAG, "RecordMesg written for location: ${location.latitude}, ${location.longitude}")
    }

    /**
     * Finalizes the FIT file writing. Writes Lap, Session, and final Event.STOP messages,
     * then closes the file.
     */
    fun endFitFile() {
        if (fileEncoder == null) {
            Log.w(TAG, "No active FIT file to end.")
            return
        }

        val lastLocationTime = previousLocation?.let { com.garmin.fit.DateTime(it.time / 1000L) } ?: fileCreationTime

        if (lastLocationTime == null) {
            Log.e(TAG, "Cannot end FIT file, no time information available.")
            Toast.makeText(context, "Error ending FIT file: No time data.", Toast.LENGTH_LONG).show()
            closeResources()
            return
        }

        // --- Lap Message (Assuming one lap for the entire activity for simplicity) ---
        val lapMesg = com.garmin.fit.LapMesg()
        lapMesg.setStartTime(firstLocationTime ?: fileCreationTime)
        lapMesg.setTimestamp(lastLocationTime)
        lapMesg.setTotalElapsedTime(totalElapsedTime)
        lapMesg.setTotalTimerTime(totalTimerTime)
        lapMesg.setTotalDistance(cumulativeDistance)
        lapMesg.setTotalCalories(totalCalories)
        lapMesg.setSport(com.garmin.fit.Sport.GENERIC) // Or the sport passed in startNewFitFile
        lapMesg.setAvgHeartRate(if (totalTimerTime > 0) (totalCalories / totalTimerTime).toInt().toShort() else 0) // Very rough avg HR calc
        lapMesg.setMaxHeartRate(maxHeartRate)
        lapMesg.setAvgSpeed(if (totalTimerTime > 0) cumulativeDistance / totalTimerTime else 0F)
        lapMesg.setMaxSpeed(maxSpeed)
        lapMesg.setEvent(com.garmin.fit.Event.LAP)
        lapMesg.setEventType(com.garmin.fit.EventType.STOP)
        fileEncoder?.write(lapMesg)
        Log.d(TAG, "LapMesg written.")

        // --- Session Message ---
        val sessionMesg = com.garmin.fit.SessionMesg()
        sessionMesg.setStartTime(firstLocationTime ?: fileCreationTime)
        sessionMesg.setTimestamp(lastLocationTime)
        sessionMesg.setTotalElapsedTime(totalElapsedTime)
        sessionMesg.setTotalTimerTime(totalTimerTime)
        sessionMesg.setTotalDistance(cumulativeDistance)
        sessionMesg.setTotalCalories(totalCalories)
        sessionMesg.setSport(com.garmin.fit.Sport.GENERIC) // Or the sport passed in startNewFitFile
        sessionMesg.setSubSport(com.garmin.fit.SubSport.GENERIC)
        sessionMesg.setAvgHeartRate(if (totalTimerTime > 0) (totalCalories / totalTimerTime).toInt()
            .toShort() else 0) // Very rough avg HR calc
        sessionMesg.setMaxHeartRate(maxHeartRate)
        sessionMesg.setAvgSpeed(if (totalTimerTime > 0) cumulativeDistance / totalTimerTime else 0F)
        sessionMesg.setMaxSpeed(maxSpeed)
        sessionMesg.setNumLaps(1) // Assuming one lap
        sessionMesg.setFirstLapIndex(0)
        sessionMesg.setEvent(com.garmin.fit.Event.SESSION)
        sessionMesg.setEventType(com.garmin.fit.EventType.STOP)
        fileEncoder?.write(sessionMesg)
        Log.d(TAG, "SessionMesg written.")

        // --- Event Message (Timer Stop) ---
        val eventMesgStop = com.garmin.fit.EventMesg()
        eventMesgStop.setTimestamp(lastLocationTime)
        eventMesgStop.setEvent(com.garmin.fit.Event.TIMER)
        eventMesgStop.setEventType(com.garmin.fit.EventType.STOP)
        fileEncoder?.write(eventMesgStop)
        Log.d(TAG, "Event.TIMER STOP written.")

        // Finalize and close the file
        closeResources()
        Toast.makeText(context, "FIT file saved.", Toast.LENGTH_SHORT).show()
        Log.i(TAG, "FIT file encoding completed and closed.")
    }

    private fun closeResources() {
        try {
            fileEncoder?.close()
            fileOutputStream?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing FIT file resources: ${e.message}", e)
        } finally {
            fileEncoder = null
            fileOutputStream = null
            previousLocation = null // Clear state for next session
            firstLocationTime = null
        }
    }
}