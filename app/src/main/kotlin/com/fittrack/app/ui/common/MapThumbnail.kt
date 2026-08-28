package com.fittrack.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.fittrack.app.data.db.RoutePointEntity
import com.fittrack.app.ui.theme.PrimaryBlue

@Composable
fun MapThumbnail(routePoints: List<RoutePointEntity>, modifier: Modifier = Modifier) {
    if (routePoints.isEmpty()) {
        Box(modifier = modifier.background(Color.LightGray), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Place, contentDescription = null, tint = Color.Gray)
        }
    } else {
        Canvas(modifier = modifier.background(Color(0xFFE0E0E0))) {
            val minLat = routePoints.minOf { it.latitude }
            val maxLat = routePoints.maxOf { it.latitude }
            val minLng = routePoints.minOf { it.longitude }
            val maxLng = routePoints.maxOf { it.longitude }

            val latRange = (maxLat - minLat).coerceAtLeast(0.0001)
            val lngRange = (maxLng - minLng).coerceAtLeast(0.0001)

            val width = size.width
            val height = size.height

            val path = Path()
            routePoints.forEachIndexed { index, point ->
                val x = ((point.longitude - minLng) / lngRange * width).toFloat()
                val y = height - ((point.latitude - minLat) / latRange * height).toFloat()
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = PrimaryBlue,
                style = Stroke(width = 4f)
            )
        }
    }
}
