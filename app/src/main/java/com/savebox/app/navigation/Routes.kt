package com.savebox.app.navigation

import kotlinx.serialization.Serializable

/*
 * Every screen in SaveBox has a "route": a small class that names the screen
 * and carries the arguments it needs (for example, which goal to open).
 *
 * These are "type-safe routes": instead of building strings like "goal/42",
 * we navigate with navController.navigate(GoalDetailsRoute(goalId = 42)).
 * The compiler checks the arguments for us.
 *
 * TO ADD A NEW SCREEN:
 * 1. Add a route here (object if it has no arguments, data class if it has).
 * 2. Add a composable<YourRoute> { ... } entry in SaveBoxNavHost.kt.
 * 3. Navigate to it with navController.navigate(YourRoute(...)).
 */

/** First-launch introduction. */
@Serializable
object OnboardingRoute

/** Home dashboard (bottom bar tab). */
@Serializable
object DashboardRoute

/** All savings activity (bottom bar tab). */
@Serializable
object HistoryRoute

/** App settings (bottom bar tab). */
@Serializable
object SettingsRoute

/**
 * Create a new goal, or edit an existing one.
 * @param goalId null = create a new goal; a number = edit that goal.
 */
@Serializable
data class GoalEditorRoute(val goalId: Long? = null)

/** Details of one goal. */
@Serializable
data class GoalDetailsRoute(val goalId: Long)

/** Denomination box grid for one goal. */
@Serializable
data class DenominationRoute(val goalId: Long)

/**
 * Add (or edit) a custom savings entry.
 * @param goalId the goal to pre-select, or null to let the user choose.
 * @param transactionId null = new entry; a number = edit that entry.
 */
@Serializable
data class AddSavingsRoute(val goalId: Long? = null, val transactionId: Long? = null)
