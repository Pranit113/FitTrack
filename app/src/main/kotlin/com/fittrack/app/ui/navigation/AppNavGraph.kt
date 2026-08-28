package com.fittrack.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.fittrack.app.ui.history.ActivityDetailScreen
import com.fittrack.app.ui.history.HistoryScreen
import com.fittrack.app.ui.settings.SettingsScreen
import com.fittrack.app.ui.stats.StatsScreen
import com.fittrack.app.ui.track.TrackScreen

@Composable
fun FitTrackApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val items = listOf(
        NavigationItem("Track", "track", Icons.Filled.PlayArrow),
        NavigationItem("History", "history", Icons.Filled.List),
        NavigationItem("Stats", "stats", Icons.Filled.Person),
        NavigationItem("Settings", "settings", Icons.Filled.Settings)
    )

    val showBottomBar = items.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = "track", Modifier.padding(innerPadding)) {
            composable("track") { TrackScreen() }
            composable("history") { 
                HistoryScreen(onActivityClick = { id -> navController.navigate("history/detail/$id") }) 
            }
            composable("history/detail/{activityId}") { backStackEntry ->
                val activityId = backStackEntry.arguments?.getString("activityId")?.toLongOrNull() ?: 0L
                ActivityDetailScreen(activityId = activityId, onBack = { navController.popBackStack() })
            }
            composable("stats") { StatsScreen() }
            composable("settings") { SettingsScreen() }
        }
    }
}

data class NavigationItem(val title: String, val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
