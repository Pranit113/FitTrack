package com.fittrack.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fittrack.app.data.preferences.UnitSystem
import com.fittrack.app.service.LocationUtils

@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val weeklyDist by viewModel.weeklyDistanceKm.collectAsState()
    val monthlyDist by viewModel.monthlyDistanceKm.collectAsState()
    val streak by viewModel.currentStreak.collectAsState()
    val pbs by viewModel.personalBests.collectAsState()
    val weeklyData by viewModel.weeklyData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OverviewCard("This Week", "%.2f km".format(weeklyDist), Modifier.weight(1f))
            OverviewCard("This Month", "%.2f km".format(monthlyDist), Modifier.weight(1f))
            OverviewCard("Streak", "$streak days", Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        if (weeklyData.isNotEmpty()) {
            Text("Last 8 Weeks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            WeeklyBarChart(weeklyData = weeklyData)
        }

        Spacer(Modifier.height(24.dp))

        Text("Personal Bests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

        val longestDist = pbs.longestDistanceActivity?.let {
            LocationUtils.formatDistance(it.distanceMeters, UnitSystem.KM)
        } ?: "--"
        val fastestPace = pbs.fastestPaceActivity?.let {
            LocationUtils.formatPace(it.avgPaceSecPerKm)
        } ?: "--:--"
        val longestDur = pbs.longestDurationActivity?.let {
            LocationUtils.formatDuration(it.durationSeconds)
        } ?: "--"

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PbCard("Longest", longestDist, Modifier.weight(1f))
            PbCard("Fastest Pace", fastestPace, Modifier.weight(1f))
            PbCard("Longest Run", longestDur, Modifier.weight(1f))
        }

        if (pbs.longestDistanceActivity == null) {
            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "Complete your first activity to see personal bests!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OverviewCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PbCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WeeklyBarChart(weeklyData: List<WeeklyData>) {
    val maxDist = weeklyData.maxOfOrNull { it.distanceKm }?.coerceAtLeast(1.0) ?: 1.0
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(12.dp)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                val barWidth = size.width / (weeklyData.size * 1.5f)
                val spacing = barWidth * 0.5f
                weeklyData.forEachIndexed { i, data ->
                    val fraction = (data.distanceKm / maxDist).toFloat().coerceIn(0f, 1f)
                    val barHeight = size.height * fraction
                    val x = i * (barWidth + spacing)
                    drawRect(surfaceVariant, Offset(x, 0f), Size(barWidth, size.height))
                    if (barHeight > 0f) {
                        drawRect(primaryColor, Offset(x, size.height - barHeight), Size(barWidth, barHeight))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                weeklyData.forEach { data ->
                    Text(data.weekLabel, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
