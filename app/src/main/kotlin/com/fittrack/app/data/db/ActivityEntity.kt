package com.fittrack.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class ActivityType(val displayName: String, val icon: String) {
    RUN("Run", "🏃"),
    WALK("Walk", "🚶"),
    RIDE("Ride", "🚴")
}

class ActivityTypeConverter {
    @TypeConverter
    fun fromActivityType(type: ActivityType): String {
        return type.name
    }

    @TypeConverter
    fun toActivityType(name: String): ActivityType {
        return ActivityType.valueOf(name)
    }
}

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: ActivityType,
    val startTime: Long,
    val endTime: Long,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val avgPaceSecPerKm: Double,
    val elevationGainMeters: Double,
    val calsBurned: Int,
    val title: String
)
