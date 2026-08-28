package com.fittrack.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fittrack.app.data.db.ActivityType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityTypeSelector(selected: ActivityType, onSelect: (ActivityType) -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActivityType.values().forEach { type ->
            val icon = when (type) {
                ActivityType.RUN -> "🏃"
                ActivityType.WALK -> "🚶"
                ActivityType.RIDE -> "🚴"
            }
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = { Text("$icon ${type.name}") },
                enabled = enabled
            )
        }
    }
}
