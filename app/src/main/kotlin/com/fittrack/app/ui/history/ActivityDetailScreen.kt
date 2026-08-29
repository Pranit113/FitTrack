package com.fittrack.app.ui.history

import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.fittrack.app.data.db.ActivityEntity
import com.fittrack.app.data.db.RoutePointEntity
import com.fittrack.app.data.preferences.UnitSystem
import com.fittrack.app.service.LocationUtils
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    activityId: Long,
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    var activity by remember { mutableStateOf<ActivityEntity?>(null) }
    var routePoints by remember { mutableStateOf<List<RoutePointEntity>>(emptyList()) }
    val prefs by viewModel.preferences.collectAsState()

    LaunchedEffect(activityId) {
        routePoints = viewModel.getRoutePoints(activityId)
        activity = viewModel.activities.value.find { it.id == activityId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(activity?.title ?: "Activity Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val act = activity
        if (act == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // -- Route Map -------------------------------------
            if (routePoints.size >= 2) {
                val geoPoints = routePoints.map { GeoPoint(it.latitude, it.longitude) }
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, 900
                            )
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            isTilesScaledToDpi = true
                            val polyline = Polyline().apply {
                                setPoints(geoPoints)
                                outlinePaint.color = android.graphics.Color.parseColor("#1565C0")
                                outlinePaint.strokeWidth = 10f
                            }
                            overlays.add(polyline)
                            // Fit map to route bounds
                            val lats = geoPoints.map { it.latitude }
                            val lons = geoPoints.map { it.longitude }
                            val bbox = BoundingBox(
                                lats.max(), lons.max(), lats.min(), lons.min()
                            )
                            post { zoomToBoundingBox(bbox.increaseByScale(1.3f), true) }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(280.dp)
                )
            } else {
                Box(
                    Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No route recorded", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // -- Stats Grid ------------------------------------
            Column(modifier = Modifier.padding(16.dp)) {
                val dateStr = SimpleDateFormat("EEE, d MMM yyyy  HH:mm", Locale.getDefault())
                    .format(Date(act.startTime))

                Text(dateStr, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))

                val isKm = prefs.unitSystem == UnitSystem.KM
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailCard("Distance",
                        LocationUtils.formatDistance(act.distanceMeters, prefs.unitSystem),
                        Modifier.weight(1f))
                    DetailCard("Duration",
                        LocationUtils.formatDuration(act.durationSeconds),
                        Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailCard("Avg Pace",
                        LocationUtils.formatPace(act.avgPaceSecPerKm) + if (isKm) " /km" else " /mi",
                        Modifier.weight(1f))
                    DetailCard("Elevation",
                        "+%.0f m".format(act.elevationGainMeters),
                        Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailCard("Calories", "${act.calsBurned} kcal", Modifier.weight(1f))
                    DetailCard("Type", "${act.type.icon} ${act.type.displayName}", Modifier.weight(1f))
                }

                // -- Splits table -----------------------------
                if (routePoints.size >= 2) {
                    Spacer(Modifier.height(20.dp))
                    Text("Splits", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    val splits = computeSplits(routePoints, isKm)
                    splits.forEachIndexed { i, split ->
                        SplitRow(km = i + 1, label = if (isKm) "km" else "mi", paceStr = split)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SplitRow(km: Int, label: String, paceStr: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("$label $km", style = MaterialTheme.typography.bodyMedium)
        Text(paceStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
    HorizontalDivider()
}

private fun computeSplits(points: List<RoutePointEntity>, isKm: Boolean): List<String> {
    val splits = mutableListOf<String>()
    val unitMeters = if (isKm) 1000.0 else 1609.34
    var accumulated = 0.0
    var splitStartTime = points.first().timestamp
    var prev = points.first()

    for (pt in points.drop(1)) {
        val d = haversine(prev.latitude, prev.longitude, pt.latitude, pt.longitude)
        accumulated += d
        if (accumulated >= unitMeters) {
            val splitSeconds = (pt.timestamp - splitStartTime) / 1000L
            val pace = if (accumulated > 0) (splitSeconds * unitMeters / accumulated).toLong() else 0L
            val m = pace / 60; val s = pace % 60
            splits.add("%d:%02d".format(m, s))
            accumulated -= unitMeters
            splitStartTime = pt.timestamp
        }
        prev = pt
    }
    return splits
}

private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2).let { it * it } +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2).let { it * it }
    return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}
