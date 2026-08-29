package com.fittrack.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fittrack.app.data.db.ActivityEntity
import com.fittrack.app.data.db.ActivityType
import com.fittrack.app.data.preferences.UnitSystem
import com.fittrack.app.service.LocationUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onActivityClick: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val activities by viewModel.activities.collectAsState()
    val prefs by viewModel.preferences.collectAsState()
    var activityToDelete by remember { mutableStateOf<ActivityEntity?>(null) }

    if (activities.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏃", fontSize = 64.sp)
                Spacer(Modifier.height(16.dp))
                Text("No activities yet", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Start your first activity on the Track tab",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(activities, key = { it.id }) { activity ->
                ActivityCard(
                    activity = activity,
                    unitSystem = prefs.unitSystem,
                    onClick = { onActivityClick(activity.id) },
                    onDelete = { activityToDelete = activity }
                )
            }
        }
    }

    activityToDelete?.let { act ->
        AlertDialog(
            onDismissRequest = { activityToDelete = null },
            title = { Text("Delete Activity?") },
            text = { Text("This will permanently delete this activity. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteActivity(act); activityToDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { activityToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ActivityCard(
    activity: ActivityEntity,
    unitSystem: UnitSystem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("EEE, d MMM  HH:mm", Locale.getDefault())
        .format(Date(activity.startTime))

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = when (activity.type) {
                    ActivityType.RUN  -> Color(0xFF4CAF50)
                    ActivityType.WALK -> Color(0xFF2196F3)
                    ActivityType.RIDE -> Color(0xFFFF9800)
                },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(activity.type.icon, fontSize = 24.sp)
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(activity.title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Text(dateStr, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip(LocationUtils.formatDistance(activity.distanceMeters, unitSystem))
                    StatChip(LocationUtils.formatDuration(activity.durationSeconds))
                    if (activity.avgPaceSecPerKm > 0)
                        StatChip(LocationUtils.formatPace(activity.avgPaceSecPerKm))
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
                Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun StatChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Text(text, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}