package com.fittrack.app.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fittrack.app.data.db.RoutePointEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(activityId: Long, onBack: () -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    var routePoints by remember { mutableStateOf<List<RoutePointEntity>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(activityId) {
        scope.launch {
            routePoints = viewModel.getRoutePoints(activityId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                Text("Map Placeholder")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Stats Grid Here")
        }
    }
}
