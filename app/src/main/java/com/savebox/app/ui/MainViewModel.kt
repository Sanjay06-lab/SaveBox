package com.savebox.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savebox.app.data.preferences.UserPreferences
import com.savebox.app.data.preferences.UserPreferencesRepository
import com.savebox.app.navigation.DashboardRoute
import com.savebox.app.navigation.OnboardingRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * App-wide state that lives as long as MainActivity.
 *
 *  - Decides the FIRST screen (onboarding for new users, dashboard otherwise).
 *  - Provides the settings the whole UI needs, such as the theme.
 *  - Holds a goal to open when the app was started from the widget or a notification.
 *
 * While the first screen is being decided (a few milliseconds), the splash screen stays visible.
 */
class MainViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    /** null while loading; then OnboardingRoute or DashboardRoute. Decided only once. */
    var startDestination by mutableStateOf<Any?>(null)
        private set

    /** Current settings (null until loaded). Used for the theme. */
    val preferences: StateFlow<UserPreferences?> = userPreferencesRepository.userPreferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    /** A goal to open as soon as possible (from the widget or a notification). */
    var pendingGoalId by mutableStateOf<Long?>(null)
        private set

    init {
        viewModelScope.launch {
            val preferences = userPreferencesRepository.userPreferences.first()
            startDestination = if (preferences.onboardingCompleted) DashboardRoute else OnboardingRoute
        }
    }

    /**
     * Remember that onboarding was seen. Runs in this Activity-level ViewModel so the
     * save finishes even though the onboarding screen closes immediately.
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.setOnboardingCompleted()
        }
    }

    fun openGoal(goalId: Long?) {
        if (goalId != null) pendingGoalId = goalId
    }

    fun onGoalOpened() {
        pendingGoalId = null
    }
}
