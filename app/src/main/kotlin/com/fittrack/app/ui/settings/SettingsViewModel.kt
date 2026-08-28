package com.fittrack.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.app.data.preferences.AppPreferences
import com.fittrack.app.data.preferences.UnitSystem
import com.fittrack.app.data.preferences.UserPreferences
import com.fittrack.app.data.repository.ActivityRepository
import com.fittrack.app.export.GpxExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ExportState {
    object Idle : ExportState()
    object Exporting : ExportState()
    data class Success(val uri: String) : ExportState()
    data class Error(val msg: String) : ExportState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val repository: ActivityRepository,
    private val gpxExporter: GpxExporter
) : ViewModel() {

    val preferences: StateFlow<AppPreferences> = userPreferences.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences())

    val exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val deleteState = MutableStateFlow(false)

    fun setUnits(unit: UnitSystem) {
        viewModelScope.launch {
            userPreferences.updateUnitSystem(unit)
        }
    }

    fun exportActivity(activityId: Long) {
        exportState.value = ExportState.Exporting
        // Placeholder export logic
        exportState.value = ExportState.Success("uri://placeholder")
    }

    fun deleteAllData() {
        viewModelScope.launch {
            repository.deleteAllData()
            deleteState.value = true
        }
    }
}
