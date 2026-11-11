package com.savebox.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.savebox.app.ui.dashboard.DashboardScreen
import com.savebox.app.ui.denomination.DenominationScreen
import com.savebox.app.ui.transactions.AddSavingsScreen
import com.savebox.app.ui.goals.GoalDetailsScreen
import com.savebox.app.ui.goals.GoalEditorScreen
import com.savebox.app.ui.onboarding.OnboardingScreen
import com.savebox.app.ui.settings.SettingsScreen
import com.savebox.app.ui.transactions.HistoryScreen

/**
 * The map of all screens in the app.
 *
 * Each composable<Route> { } block says: "when we navigate to this route, show this screen".
 * Screens never receive the NavController itself - only simple lambdas like
 * onOpenGoal = { id -> ... }. That keeps screens easy to preview, test and reuse.
 *
 * @param startDestination OnboardingRoute for new users, DashboardRoute otherwise.
 * @param onOnboardingFinished saves "onboarding seen" so it is not shown again.
 */
@Composable
fun SaveBoxNavHost(
    navController: NavHostController,
    startDestination: Any,
    onOnboardingFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable<OnboardingRoute> {
            OnboardingScreen(
                onGetStarted = {
                    onOnboardingFinished()
                    navController.leaveOnboarding()
                    // Take new users straight to creating their first goal.
                    navController.navigate(GoalEditorRoute())
                },
                onSkip = {
                    onOnboardingFinished()
                    navController.leaveOnboarding()
                },
            )
        }

        composable<DashboardRoute> {
            DashboardScreen(
                onCreateGoal = { navController.navigate(GoalEditorRoute()) },
                onOpenGoal = { goalId -> navController.navigate(GoalDetailsRoute(goalId)) },
                onQuickAdd = { navController.navigate(AddSavingsRoute()) },
            )
        }

        composable<HistoryRoute> {
            HistoryScreen(
                onEditTransaction = { transactionId ->
                    navController.navigate(AddSavingsRoute(transactionId = transactionId))
                },
            )
        }

        composable<SettingsRoute> {
            SettingsScreen()
        }

        composable<GoalEditorRoute> {
            GoalEditorScreen(
                onBack = { navController.navigateUp() },
                onSaved = { goalId, wasNewGoal ->
                    if (wasNewGoal) {
                        // Show the new goal, and remove the form from the back stack
                        // so pressing Back from the goal goes to the dashboard.
                        navController.navigate(GoalDetailsRoute(goalId)) {
                            popUpTo<GoalEditorRoute> { inclusive = true }
                        }
                    } else {
                        navController.navigateUp()
                    }
                },
            )
        }

        composable<GoalDetailsRoute> {
            GoalDetailsScreen(
                onBack = { navController.navigateUp() },
                onEditGoal = { goalId -> navController.navigate(GoalEditorRoute(goalId)) },
                onAddSavings = { goalId -> navController.navigate(AddSavingsRoute(goalId = goalId)) },
                onOpenBoxes = { goalId -> navController.navigate(DenominationRoute(goalId)) },
                onEditTransaction = { transactionId ->
                    navController.navigate(AddSavingsRoute(transactionId = transactionId))
                },
            )
        }

        composable<AddSavingsRoute> {
            AddSavingsScreen(
                onBack = { navController.navigateUp() },
                onDone = { navController.navigateUp() },
                onCreateGoal = {
                    navController.navigate(GoalEditorRoute()) {
                        popUpTo<AddSavingsRoute> { inclusive = true }
                    }
                },
            )
        }

        composable<DenominationRoute> {
            DenominationScreen(onBack = { navController.navigateUp() })
        }
    }
}

/** Go to the dashboard and remove onboarding from history (Back won't return to it). */
private fun NavHostController.leaveOnboarding() {
    navigate(DashboardRoute) {
        popUpTo<OnboardingRoute> { inclusive = true }
    }
}
