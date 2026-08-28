package com.fittrack.app.service

import com.fittrack.app.data.db.ActivityType
import com.fittrack.app.data.preferences.UnitSystem
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object LocationUtils {
    fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // Earth's radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    fun calculatePace(distanceMeters: Double, durationSeconds: Long): Double {
        if (distanceMeters < 10.0) return 0.0
        val distanceKm = distanceMeters / 1000.0
        return durationSeconds / distanceKm
    }

    fun formatPace(secPerKm: Double): String {
        if (secPerKm <= 0.0) return "--:--"
        val minutes = (secPerKm / 60).toInt()
        val seconds = (secPerKm % 60).toInt()
        return String.format("%02d:%02d /km", minutes, seconds)
    }

    fun formatDistance(meters: Double, unitSystem: UnitSystem): String {
        val distance = (meters / 1000.0) * unitSystem.distanceFactor
        return String.format("%.2f %s", distance, unitSystem.distanceLabel)
    }

    fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format("%02d:%02d:%02d", h, m, s)
        } else {
            String.format("%02d:%02d", m, s)
        }
    }

    fun estimateCalories(activityType: ActivityType, distanceMeters: Double, durationSeconds: Long): Int {
        val distanceKm = distanceMeters / 1000.0
        return when (activityType) {
            ActivityType.RUN -> (distanceKm * 70).toInt()
            ActivityType.WALK -> (distanceKm * 50).toInt()
            ActivityType.RIDE -> (distanceKm * 30).toInt()
        }
    }

    fun smoothElevationGain(altitudes: List<Double>, threshold: Double = 2.0): Double {
        var gain = 0.0
        for (i in 1 until altitudes.size) {
            val diff = altitudes[i] - altitudes[i - 1]
            if (diff > threshold) {
                gain += diff
            }
        }
        return gain
    }
}
