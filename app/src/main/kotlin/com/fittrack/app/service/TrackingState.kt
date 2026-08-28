package com.fittrack.app.service

import com.fittrack.app.data.db.ActivityType

data class LiveStats(
    val distanceMeters: Double,
    val durationSeconds: Long,
    val paceSecPerKm: Double,
    val elevationGainMeters: Double,
    val currentLat: Double,
    val currentLng: Double,
    val speedMps: Float
) {
    companion object {
        val EMPTY = LiveStats(0.0, 0L, 0.0, 0.0, 0.0, 0.0, 0f)
    }
}

sealed class TrackingStatus {
    object Idle : TrackingStatus()
    data class Tracking(val stats: LiveStats, val activityType: ActivityType) : TrackingStatus()
    data class Paused(val stats: LiveStats, val activityType: ActivityType) : TrackingStatus()
}
