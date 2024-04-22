package com.example.uitutorial

import android.content.Context
import android.util.Log
import java.io.File

class TrackWriter(private val context: Context) {

    val filename = "track.txt"




    val fileContents = "Hello world!"
    val file = File(context.filesDir, filename)

    init{


        context.openFileOutput(filename, Context.MODE_PRIVATE).use {
            it.write(fileContents.toByteArray())
        }

        var files: Array<String> = context.fileList()
        for (fileName in files) {
            Log.d("File name:", fileName)
        }
    }




}