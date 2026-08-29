package com.fittrack.app.ui.track

import android.Manifest
import android.os.Build
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.fittrack.app.data.db.ActivityType
import com.fittrack.app.service.LocationUtils
import com.fittrack.app.service.TrackingStatus
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TrackScreen(viewModel: TrackViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val status by viewModel.trackingStatus.collectAsState()
    val prefs by viewModel.preferences.collectAsState()
    val elapsedSeconds by remember { derivedStateOf { viewModel.elapsedSeconds } }

    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    var showRationale by remember { mutableStateOf(false) }

    val notifPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS) else null

    // Collect route points for polyline
    val routeGeoPoints = remember { mutableStateListOf<GeoPoint>() }
    LaunchedEffect(status) {
        when (val s = status) {
            is TrackingStatus.Tracking -> {
                if (s.stats.currentLat != 0.0 || s.stats.currentLng != 0.0)
                    routeGeoPoints.add(GeoPoint(s.stats.currentLat, s.stats.currentLng))
            }
            is TrackingStatus.Idle -> routeGeoPoints.clear()
            else -> Unit
        }
    }

    val isTracking = status is TrackingStatus.Tracking || status is TrackingStatus.Paused
    val currentStats = when (val s = status) {
        is TrackingStatus.Tracking -> s.stats
        is TrackingStatus.Paused   -> s.stats
        else -> null
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // -- Activity type selector -----------------------------
        if (!isTracking) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ActivityType.values().forEach { type ->
                        val selected = viewModel.selectedActivityType == type
                        Button(
                            onClick = { viewModel.setActivityType(type) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface,
                                contentColor = if (selected) Color.White
                                else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                        ) {
                            Text("${type.icon} ${type.displayName}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // -- Map -----------------------------------------------
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (locationPermission.status.isGranted) {
                val mapView = remember {
                    MapView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        isTilesScaledToDpi = true
                        controller.setZoom(17.0)
                    }
                }

                val locationOverlay = remember {
                    MyLocationNewOverlay(GpsMyLocationProvider(context), mapView).apply {
                        enableMyLocation()
                        enableFollowLocation()
                    }
                }

                val polyline = remember {
                    Polyline().apply {
                        outlinePaint.color = android.graphics.Color.parseColor("#1565C0")
                        outlinePaint.strokeWidth = 10f
                    }
                }

                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize(),
                    update = { mv ->
                        if (!mv.overlays.contains(locationOverlay)) {
                            mv.overlays.add(locationOverlay)
                        }
                        if (routeGeoPoints.size >= 2) {
                            polyline.setPoints(routeGeoPoints.toList())
                            if (!mv.overlays.contains(polyline)) mv.overlays.add(polyline)
                        }
                        mv.invalidate()
                    }
                )

                DisposableEffect(Unit) {
                    mapView.onResume()
                    locationOverlay.enableMyLocation()
                    onDispose {
                        locationOverlay.disableMyLocation()
                        mapView.onPause()
                    }
                }
            } else {
                // Permission not yet granted
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("??", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Location permission needed", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        if (locationPermission.status.shouldShowRationale) showRationale = true
                        else locationPermission.launchPermissionRequest()
                    }) { Text("Grant Permission") }
                }
            }
        }

        // -- Stats row -----------------------------------------
        Surface(shadowElevation = 8.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatTile(
                        label = "Distance",
                        value = currentStats?.let {
                            LocationUtils.formatDistance(it.distanceMeters, prefs.unitSystem)
                        } ?: "0.00 km",
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Duration",
                        value = LocationUtils.formatDuration(
                            if (isTracking) elapsedSeconds else currentStats?.durationSeconds ?: 0L
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatTile(
                        label = "Pace",
                        value = currentStats?.let { LocationUtils.formatPace(it.paceSecPerKm) } ?: "--:--",
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Elevation",
                        value = currentStats?.let { "+%.0fm".format(it.elevationGainMeters) } ?: "+0m",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                // -- Control buttons ----------------------------
                when (status) {
                    is TrackingStatus.Idle -> {
                        Button(
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            onClick = {
                                if (!locationPermission.status.isGranted) {
                                    locationPermission.launchPermissionRequest(); return@Button
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    notifPermission?.status?.isGranted == false)
                                    notifPermission.launchPermissionRequest()
                                context.startForegroundService(viewModel.getStartIntent(context))
                            }
                        ) { Text("START", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                    }
                    is TrackingStatus.Tracking -> {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f).height(52.dp),
                                onClick = { context.startForegroundService(viewModel.getPauseIntent(context)) }
                            ) { Text("PAUSE", fontWeight = FontWeight.Bold) }
                            Button(
                                modifier = Modifier.weight(1f).height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                onClick = { context.startForegroundService(viewModel.getFinishIntent(context)) }
                            ) { Text("FINISH", fontWeight = FontWeight.Bold) }
                        }
                    }
                    is TrackingStatus.Paused -> {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                modifier = Modifier.weight(1f).height(52.dp),
                                onClick = { context.startForegroundService(viewModel.getResumeIntent(context)) }
                            ) { Text("RESUME", fontWeight = FontWeight.Bold) }
                            OutlinedButton(
                                modifier = Modifier.weight(1f).height(52.dp),
                                onClick = { context.startForegroundService(viewModel.getFinishIntent(context)) }
                            ) { Text("FINISH", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text("Location Required") },
            text = { Text("FitTrack needs your location to track routes and show the map.") },
            confirmButton = {
                TextButton(onClick = { showRationale = false; locationPermission.launchPermissionRequest() }) { Text("Grant") }
            },
            dismissButton = { TextButton(onClick = { showRationale = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}
