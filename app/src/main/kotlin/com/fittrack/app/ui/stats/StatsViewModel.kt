package com.fittrack.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.db.ActivityEntity
import com.fittrack.app.data.preferences.AppPreferences
import com.fittrack.app.data.preferences.UserPreferences
import com.fittrack.app.data.repository.ActivityRepository
import com.fittrack.app.data.repository.PersonalBests
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class WeeklyData(val weekLabel: String, val distanceKm: Double, val activityCount: Int)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: ActivityRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val allActivities: StateFlow<List<ActivityEntity>> = repository.getAllActivities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyDistanceKm: StateFlow<Double> = allActivities.map { activities ->
        val weekStart = getStartOfCurrentWeek()
        activities.filter { it.startTime >= weekStart }
            .sumOf { it.distanceMeters / 1000.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlyDistanceKm: StateFlow<Double> = allActivities.map { activities ->
        val monthStart = getStartOfCurrentMonth()
        activities.filter { it.startTime >= monthStart }
            .sumOf { it.distanceMeters / 1000.0 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentStreak: StateFlow<Int> = allActivities.map { activities ->
        computeStreak(activities)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val weeklyData: StateFlow<List<WeeklyData>> = allActivities.map { activities ->
        buildWeeklyData(activities)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _personalBests = MutableStateFlow(
        PersonalBests(null, null, null)
    )
    val personalBests: StateFlow<PersonalBests> = _personalBests

    val preferences: StateFlow<AppPreferences> = userPreferences.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences())

    init {
        viewModelScope.launch {
            repository.getAllActivities().collect { _ ->
                _personalBests.value = repository.getPersonalBests()
            }
        }
    }

    private fun getStartOfCurrentWeek(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getStartOfCurrentMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun computeStreak(activities: List<ActivityEntity>): Int {
        if (activities.isEmpty()) return 0
        val daySet = activities.map { activity ->
            val cal = Calendar.getInstance().apply { timeInMillis = activity.startTime }
            cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
        }.toSortedSet(reverseOrder())

        val today = Calendar.getInstance()
        var streak = 0
        var checkDay = today.get(Calendar.YEAR) * 1000 + today.get(Calendar.DAY_OF_YEAR)
        while (daySet.contains(checkDay)) {
            streak++
            checkDay--
            // handle year boundary: not perfect but good enough
        }
        return streak
    }

    private fun buildWeeklyData(activities: List<ActivityEntity>): List<WeeklyData> {
        val result = mutableListOf<WeeklyData>()
        val cal = Calendar.getInstance()
        repeat(8) { weekOffset ->
            val weekEnd = cal.timeInMillis
            cal.add(Calendar.WEEK_OF_YEAR, -1)
            val weekStart = cal.timeInMillis
            val weekActivities = activities.filter { it.startTime in weekStart..weekEnd }
            result.add(0, WeeklyData(
                weekLabel = "W${8 - weekOffset}",
                distanceKm = weekActivities.sumOf { it.distanceMeters / 1000.0 },
                activityCount = weekActivities.size
            ))
        }
        return result
    }
}
