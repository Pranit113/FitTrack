package com.fittrack.app.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.fittrack.app.data.db.ActivityEntity
import com.fittrack.app.data.db.ActivityType
import com.fittrack.app.data.db.RoutePointEntity
import com.fittrack.app.data.repository.ActivityRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TrackingService : LifecycleService() {

    @Inject
    lateinit var repository: ActivityRepository

    @Inject
    lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_FINISH = "ACTION_FINISH"
        const val EXTRA_ACTIVITY_TYPE = "EXTRA_ACTIVITY_TYPE"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "tracking_channel"

        private val _trackingStatus = MutableStateFlow<TrackingStatus>(TrackingStatus.Idle)
        val trackingStatus: StateFlow<TrackingStatus> = _trackingStatus.asStateFlow()
    }

    private var activityType = ActivityType.RUN
    private var startTime = 0L
    private var pauseTime = 0L
    private var totalPausedMs = 0L
    
    private val routePoints = mutableListOf<RoutePointEntity>()
    private val altitudes = mutableListOf<Double>()
    
    private var lastLat = 0.0
    private var lastLng = 0.0
    private var totalDistance = 0.0
    private var totalElevationGain = 0.0

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            for (location in result.locations) {
                if (lastLat != 0.0 && lastLng != 0.0) {
                    val distance = LocationUtils.haversineDistance(lastLat, lastLng, location.latitude, location.longitude)
                    totalDistance += distance
                }
                lastLat = location.latitude
                lastLng = location.longitude
                
                altitudes.add(location.altitude)
                totalElevationGain = LocationUtils.smoothElevationGain(altitudes)
                
                val now = System.currentTimeMillis()
                val durationMs = now - startTime - totalPausedMs
                val durationSeconds = durationMs / 1000
                
                val pace = LocationUtils.calculatePace(totalDistance, durationSeconds)
                
                val point = RoutePointEntity(
                    activityId = 0,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitudeMeters = location.altitude,
                    timestamp = now,
                    speedMps = location.speed
                )
                routePoints.add(point)

                val stats = LiveStats(
                    distanceMeters = totalDistance,
                    durationSeconds = durationSeconds,
                    paceSecPerKm = pace,
                    elevationGainMeters = totalElevationGain,
                    currentLat = location.latitude,
                    currentLng = location.longitude,
                    speedMps = location.speed
                )
                _trackingStatus.value = TrackingStatus.Tracking(stats, activityType)
                updateNotification(stats)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Tracking Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        intent?.let {
            when (it.action) {
                ACTION_START -> {
                    val typeName = it.getStringExtra(EXTRA_ACTIVITY_TYPE) ?: ActivityType.RUN.name
                    startTracking(ActivityType.valueOf(typeName))
                }
                ACTION_PAUSE -> pauseTracking()
                ACTION_RESUME -> resumeTracking()
                ACTION_FINISH -> finishTracking()
            }
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startTracking(type: ActivityType) {
        activityType = type
        startTime = System.currentTimeMillis()
        totalPausedMs = 0L
        lastLat = 0.0
        lastLng = 0.0
        totalDistance = 0.0
        totalElevationGain = 0.0
        routePoints.clear()
        altitudes.clear()
        
        startForeground(NOTIFICATION_ID, buildNotification(LiveStats.EMPTY))
        requestLocationUpdates()
        _trackingStatus.value = TrackingStatus.Tracking(LiveStats.EMPTY, activityType)
    }

    private fun pauseTracking() {
        pauseTime = System.currentTimeMillis()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        
        val currentState = _trackingStatus.value
        if (currentState is TrackingStatus.Tracking) {
            _trackingStatus.value = TrackingStatus.Paused(currentState.stats, activityType)
            updateNotification(currentState.stats)
        }
    }

    private fun resumeTracking() {
        val currentPauseMs = System.currentTimeMillis() - pauseTime
        totalPausedMs += currentPauseMs
        requestLocationUpdates()
        
        val currentState = _trackingStatus.value
        if (currentState is TrackingStatus.Paused) {
            _trackingStatus.value = TrackingStatus.Tracking(currentState.stats, activityType)
        }
    }

    private fun finishTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        
        val now = System.currentTimeMillis()
        val durationMs = now - startTime - totalPausedMs
        val durationSeconds = durationMs / 1000
        val finalPace = LocationUtils.calculatePace(totalDistance, durationSeconds)
        val calories = LocationUtils.estimateCalories(activityType, totalDistance, durationSeconds)
        
        val activity = ActivityEntity(
            type = activityType,
            startTime = startTime,
            endTime = now,
            distanceMeters = totalDistance,
            durationSeconds = durationSeconds,
            avgPaceSecPerKm = finalPace,
            elevationGainMeters = totalElevationGain,
            calsBurned = calories,
            title = "${activityType.displayName} Activity"
        )
        
        lifecycleScope.launch {
            repository.saveActivity(activity, routePoints.toList())
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            _trackingStatus.value = TrackingStatus.Idle
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(5f)
            .build()
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun buildNotification(stats: LiveStats?): Notification {
        // We'll use a placeholder activity intent for now, assuming a MainActivity exists
        val pendingIntent = try {
            val intent = Intent(this, Class.forName("com.fittrack.app.MainActivity")).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } catch (e: Exception) {
            null
        }
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(com.fittrack.app.R.drawable.ic_notification)
            .setContentTitle("FitTrack")
            .setContentText(
                stats?.let { 
                    "Distance: ${LocationUtils.formatDistance(it.distanceMeters, com.fittrack.app.data.preferences.UnitSystem.KM)} " +
                    "Duration: ${LocationUtils.formatDuration(it.durationSeconds)}"
                } ?: "Tracking your activity..."
            )
            .setOngoing(true)
            
        pendingIntent?.let { builder.setContentIntent(it) }
        
        val currentStatus = _trackingStatus.value
        
        if (currentStatus is TrackingStatus.Tracking) {
            val pauseIntent = Intent(this, TrackingService::class.java).apply { action = ACTION_PAUSE }
            val pausePendingIntent = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
        } else if (currentStatus is TrackingStatus.Paused) {
            val resumeIntent = Intent(this, TrackingService::class.java).apply { action = ACTION_RESUME }
            val resumePendingIntent = PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePendingIntent)
        }
        
        return builder.build()
    }

    private fun updateNotification(stats: LiveStats) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(stats))
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
