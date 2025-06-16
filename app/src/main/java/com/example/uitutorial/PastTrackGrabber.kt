package com.example.uitutorial

import android.content.Context
import android.util.Log
import java.io.File

/* This class is used to grab a list of tracks to send to the PastTracksViewerActivity */
class PastTrackGrabber {
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
}