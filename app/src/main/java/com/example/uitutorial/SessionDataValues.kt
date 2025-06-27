package com.example.uitutorial

import com.garmin.fit.SportEvent

data class SessionDataValues(
    val totalDistance: Double,
    val avgPace: Double,
    val movingTime: Double,
    //val elevationGain: Double,
    val sportType: SportEvent,
)
