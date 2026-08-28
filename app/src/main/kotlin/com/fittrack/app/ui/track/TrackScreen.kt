package com.fittrack.app.ui.track

import android.Manifest
import android.content.Intent
import android.os.Build
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.fittrack.app.service.LocationUtils
import com.fittrack.app.service.TrackingStatus
import com.fittrack.app.ui.common.ActivityTypeSelector
import com.fittrack.app.ui.common.StatCard
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TrackScreen(viewModel: TrackViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val status by viewModel.trackingStatus.collectAsState()
    val prefs by viewModel.preferences.collectAsState()

    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var showRationale by remember { mutableStateOf(false) }
    var notifPermLaunched by remember { mutableStateOf(false) }

    val notifPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null

    // Route points collected for the map
    val routePoints = remember { mutableStateListOf<GeoPoint>() }

    LaunchedEffect(status) {
        when (val s = status) {
            is TrackingStatus.Tracking -> {
                if (s.stats.currentLat != 0.0 || s.stats.currentLng != 0.0) {
                    routePoints.add(GeoPoint(s.stats.currentLat, s.stats.currentLng))
                }
            }
            is TrackingStatus.Idle -> routePoints.clear()
            else -> Unit
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Activity type selector
        ActivityTypeSelector(
            selected = viewModel.selectedActivityType,
            onSelect = { viewModel.setActivityType(it) },
            enabled = status is TrackingStatus.Idle
        )

        Spacer(Modifier.height(12.dp))

        // Map
        if (locationPermission.status.isGranted) {
            val mapView = remember {
                MapView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(16.0)
                    controller.setCenter(GeoPoint(28.6139, 77.2090)) // Delhi default
                }
            }

            val routeOverlay = remember { Polyline().apply { outlinePaint.strokeWidth = 8f } }

            AndroidView(
                factory = { mapView },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                update = { mv ->
                    routeOverlay.setPoints(routePoints.toList())
                    if (!mv.overlays.contains(routeOverlay)) mv.overlays.add(routeOverlay)
                    if (routePoints.isNotEmpty()) {
                        mv.controller.animateTo(routePoints.last())
                    }
                    mv.invalidate()
                }
            )

            DisposableEffect(Unit) {
                mapView.onResume()
                onDispose { mapView.onPause() }
            }
        } else {
            Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Location permission required", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        if (locationPermission.status.shouldShowRationale) {
                            showRationale = true
                        } else {
                            locationPermission.launchPermissionRequest()
                        }
                    }) { Text("Grant Permission") }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Stats
        val currentStats = when (val s = status) {
            is TrackingStatus.Tracking -> s.stats
            is TrackingStatus.Paused -> s.stats
            else -> null
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                label = "Distance",
                value = currentStats?.let {
                    LocationUtils.formatDistance(it.distanceMeters, prefs.unitSystem)
                } ?: "0.00 km",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Duration",
                value = currentStats?.let { LocationUtils.formatDuration(it.durationSeconds) } ?: "0:00",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                label = "Pace",
                value = currentStats?.let { LocationUtils.formatPace(it.paceSecPerKm) } ?: "--:--",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Elevation",
                value = currentStats?.let { "+%.0fm".format(it.elevationGainMeters) } ?: "+0m",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(16.dp))

        // Control buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            when (status) {
                is TrackingStatus.Idle -> {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        onClick = {
                            if (!locationPermission.status.isGranted) {
                                locationPermission.launchPermissionRequest()
                                return@Button
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                notifPermission?.status?.isGranted == false && !notifPermLaunched) {
                                notifPermission.launchPermissionRequest()
                                notifPermLaunched = true
                            }
                            context.startForegroundService(viewModel.getStartIntent(context))
                        }
                    ) {
                        Text("START", fontWeight = FontWeight.Bold)
                    }
                }
                is TrackingStatus.Tracking -> {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { context.startForegroundService(viewModel.getPauseIntent(context)) }
                    ) { Text("PAUSE") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = { context.startForegroundService(viewModel.getFinishIntent(context)) }
                    ) { Text("FINISH") }
                }
                is TrackingStatus.Paused -> {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { context.startForegroundService(viewModel.getResumeIntent(context)) }
                    ) { Text("RESUME") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { context.startForegroundService(viewModel.getFinishIntent(context)) }
                    ) { Text("FINISH") }
                }
            }
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Location Required") },
            text = { Text("FitTrack needs precise location to track your route. Please grant the permission.") },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    locationPermission.launchPermissionRequest()
                }) { Text("Grant") }
            },
            dismissButton = {
                TextButton(onClick = { showRationale = false }) { Text("Cancel") }
            }
        )
    }
}
