package com.fittrack.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fittrack.app.data.preferences.UnitSystem

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.preferences.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Preferences", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RadioButton(selected = prefs.unitSystem == UnitSystem.KM, onClick = { viewModel.setUnits(UnitSystem.KM) })
            Text("KM")
            RadioButton(selected = prefs.unitSystem == UnitSystem.MI, onClick = { viewModel.setUnits(UnitSystem.MI) })
            Text("MI")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.deleteAllData() }) {
            Text("Clear All Data")
        }
    }
}
