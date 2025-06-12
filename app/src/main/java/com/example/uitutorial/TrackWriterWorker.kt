package com.example.uitutorial

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.osmdroid.views.MapView

class TrackWriterWorker(appContext: Context, workerParams: WorkerParameters, private val map: MapView) :
    Worker(appContext, workerParams) {
    override fun doWork(): Result {
        //TrackWriter(map).GPSTrackWriter(map = map)
        //you will need to put a flow or LiveData for the UI components
        return Result.success()

    }

}