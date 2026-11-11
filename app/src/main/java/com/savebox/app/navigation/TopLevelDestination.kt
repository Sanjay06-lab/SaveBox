package com.savebox.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.reflect.KClass

/**
 * The tabs shown in the bottom navigation bar.
 *
 * To add a tab: add an entry here with a route from Routes.kt.
 * The bottom bar in SaveBoxApp.kt reads this list automatically.
 */
enum class TopLevelDestination(
    val label: String,
    val icon: ImageVector,
    val route: Any,
    val routeClass: KClass<*>,
) {
    HOME(
        label = "Home",
        icon = Icons.Filled.Home,
        route = DashboardRoute,
        routeClass = DashboardRoute::class,
    ),
    HISTORY(
        label = "History",
        icon = Icons.AutoMirrored.Filled.List,
        route = HistoryRoute,
        routeClass = HistoryRoute::class,
    ),
    SETTINGS(
        label = "Settings",
        icon = Icons.Filled.Settings,
        route = SettingsRoute,
        routeClass = SettingsRoute::class,
    ),
}
