package com.savebox.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.savebox.app.navigation.DashboardRoute
import com.savebox.app.navigation.GoalDetailsRoute
import com.savebox.app.navigation.OnboardingRoute
import com.savebox.app.navigation.SaveBoxNavHost
import com.savebox.app.navigation.TopLevelDestination
import com.savebox.app.ui.components.LocalAppSnackbar
import com.savebox.app.ui.components.rememberAppSnackbarState

/**
 * The root of the SaveBox user interface.
 *
 * It contains:
 *  - the NavHost (which screen is visible), and
 *  - the bottom navigation bar, shown only on the main tabs
 *    (Home / History / Settings), hidden on detail screens.
 */
@Composable
fun SaveBoxApp(
    startDestination: Any,
    onOnboardingFinished: () -> Unit,
    pendingGoalId: Long? = null,
    onGoalOpened: () -> Unit = {},
) {
    val navController = rememberNavController()

    // Opened from the widget or a notification: show that goal (once).
    LaunchedEffect(pendingGoalId) {
        if (pendingGoalId != null) {
            if (startDestination != OnboardingRoute) {
                navController.navigate(GoalDetailsRoute(pendingGoalId)) { launchSingleTop = true }
            }
            onGoalOpened()
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // Show the bottom bar only when the current screen is one of the tabs.
    val showBottomBar = TopLevelDestination.entries.any { tab ->
        currentDestination.isTab(tab)
    }

    // One snackbar for the whole app; screens reach it through LocalAppSnackbar.
    val appSnackbar = rememberAppSnackbarState()

    CompositionLocalProvider(LocalAppSnackbar provides appSnackbar) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(appSnackbar.hostState) },
            bottomBar = {
                if (showBottomBar) {
                    SaveBoxBottomBar(
                        currentDestination = currentDestination,
                        navController = navController,
                    )
                }
            },
        ) { innerPadding ->
            // On tablets and in landscape, keep content at a comfortable reading width
            // (centred) instead of stretching it across the whole screen.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
                contentAlignment = Alignment.TopCenter,
            ) {
                SaveBoxNavHost(
                    navController = navController,
                    startDestination = startDestination,
                    onOnboardingFinished = onOnboardingFinished,
                    modifier = Modifier
                        .widthIn(max = MAX_CONTENT_WIDTH)
                        .fillMaxSize(),
                )
            }
        }
    }
}

/** Widest the app's content gets on big screens. Change to taste. */
private val MAX_CONTENT_WIDTH = 840.dp

@Composable
private fun SaveBoxBottomBar(
    currentDestination: NavDestination?,
    navController: NavHostController,
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        TopLevelDestination.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentDestination.isTab(tab),
                onClick = { navController.navigateToTab(tab) },
                icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

/** True when this destination (or one of its parents) is the given tab. */
private fun NavDestination?.isTab(tab: TopLevelDestination): Boolean {
    return this?.hierarchy?.any { destination -> destination.hasRoute(tab.routeClass) } == true
}

/**
 * Switch tabs the standard Android way:
 *  - don't stack the same tab twice,
 *  - pressing Back from any tab returns to the dashboard,
 *  - remember each tab's scroll position.
 *
 * We pop up to DashboardRoute (not the graph's start destination) because for new users
 * the start destination is onboarding, which is removed from the back stack afterwards.
 */
private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.route) {
        popUpTo<DashboardRoute> {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
