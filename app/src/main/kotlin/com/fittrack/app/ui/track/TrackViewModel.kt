package com.fittrack.app.ui.track

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.db.ActivityType
import com.fittrack.app.data.preferences.AppPreferences
import com.fittrack.app.data.preferences.UserPreferences
import com.fittrack.app.service.TrackingService
import com.fittrack.app.service.TrackingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrackViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    val trackingStatus: StateFlow<TrackingStatus> = TrackingService.trackingStatus

    val preferences: StateFlow<AppPreferences> = userPreferences.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences())

    var selectedActivityType by mutableStateOf(ActivityType.RUN)
        private set

    // Real-time elapsed seconds for live timer display (updates every second)
    var elapsedSeconds by mutableLongStateOf(0L)
        private set

    private var tickerJob: Job? = null
    private var trackingStartMs = 0L
    private var pausedAccumulatedMs = 0L
    private var pauseStartMs = 0L

    init {
        viewModelScope.launch {
            trackingStatus.collect { status ->
                when (status) {
                    is TrackingStatus.Tracking -> {
                        if (trackingStartMs == 0L) trackingStartMs = System.currentTimeMillis() - (status.stats.durationSeconds * 1000)
                        startTicker()
                    }
                    is TrackingStatus.Paused -> {
                        if (pauseStartMs == 0L) pauseStartMs = System.currentTimeMillis()
                        stopTicker()
                        elapsedSeconds = status.stats.durationSeconds
                    }
                    is TrackingStatus.Idle -> {
                        stopTicker()
                        elapsedSeconds = 0L
                        trackingStartMs = 0L
                        pausedAccumulatedMs = 0L
                        pauseStartMs = 0L
                    }
                }
            }
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                val current = trackingStatus.value
                if (current is TrackingStatus.Tracking) {
                    elapsedSeconds = current.stats.durationSeconds + 
                        ((System.currentTimeMillis() - (trackingStartMs + (current.stats.durationSeconds * 1000) + pausedAccumulatedMs)) / 1000).coerceAtLeast(0)
                }
                delay(1000)
            }
        }
    }

    private fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun setActivityType(type: ActivityType) {
        selectedActivityType = type
    }

    fun getStartIntent(context: Context): Intent =
        Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_START
            putExtra(TrackingService.EXTRA_ACTIVITY_TYPE, selectedActivityType.name)
        }

    fun getPauseIntent(context: Context): Intent =
        Intent(context, TrackingService::class.java).apply { action = TrackingService.ACTION_PAUSE }

    fun getResumeIntent(context: Context): Intent =
        Intent(context, TrackingService::class.java).apply { action = TrackingService.ACTION_RESUME }

    fun getFinishIntent(context: Context): Intent =
        Intent(context, TrackingService::class.java).apply { action = TrackingService.ACTION_FINISH }

    override fun onCleared() {
        super.onCleared()
        stopTicker()
    }
}
