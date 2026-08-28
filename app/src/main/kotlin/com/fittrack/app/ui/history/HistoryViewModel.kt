package com.fittrack.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.db.ActivityEntity
import com.fittrack.app.data.db.RoutePointEntity
import com.fittrack.app.data.preferences.AppPreferences
import com.fittrack.app.data.preferences.UserPreferences
import com.fittrack.app.data.repository.ActivityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: ActivityRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val activities: StateFlow<List<ActivityEntity>> = repository.getAllActivities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val preferences: StateFlow<AppPreferences> = userPreferences.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences())

    suspend fun getRoutePoints(activityId: Long): List<RoutePointEntity> {
        return repository.getActivityWithRoute(activityId)?.routePoints ?: emptyList()
    }

    fun deleteActivity(activity: ActivityEntity) {
        viewModelScope.launch {
            repository.deleteActivity(activity)
        }
    }
}
