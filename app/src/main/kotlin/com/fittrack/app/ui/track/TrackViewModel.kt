package com.fittrack.app.ui.track

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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

    fun setActivityType(type: ActivityType) {
        selectedActivityType = type
    }

    fun getStartIntent(context: Context): Intent {
        return Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_START
            putExtra(TrackingService.EXTRA_ACTIVITY_TYPE, selectedActivityType.name)
        }
    }

    fun getPauseIntent(context: Context): Intent {
        return Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_PAUSE
        }
    }

    fun getResumeIntent(context: Context): Intent {
        return Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_RESUME
        }
    }

    fun getFinishIntent(context: Context): Intent {
        return Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_FINISH
        }
    }
}
