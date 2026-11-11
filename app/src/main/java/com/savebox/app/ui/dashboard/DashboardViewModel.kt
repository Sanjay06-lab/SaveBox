package com.savebox.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savebox.app.data.repository.SavingsRepository
import com.savebox.app.domain.calculations.SavingsCalculator
import com.savebox.app.domain.calculations.SavingsSummary
import com.savebox.app.domain.model.SavingsGoal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Everything the dashboard needs to draw itself.
 * The screen just displays these values - it does no calculations.
 */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val summary: SavingsSummary = SavingsSummary.EMPTY,
    val activeGoals: List<SavingsGoal> = emptyList(),
    val completedGoals: List<SavingsGoal> = emptyList(),
    /** Up to 3 goals with the most recent changes. */
    val recentlyUpdatedGoals: List<SavingsGoal> = emptyList(),
) {
    val hasGoals: Boolean get() = activeGoals.isNotEmpty() || completedGoals.isNotEmpty()
}

/**
 * Turns the list of goals from the repository into dashboard state.
 * Because it uses a Flow, the dashboard refreshes automatically when any goal changes.
 */
class DashboardViewModel(
    savingsRepository: SavingsRepository,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = savingsRepository.observeGoals()
        .map { goals -> buildUiState(goals) }
        .stateIn(
            scope = viewModelScope,
            // Keep listening for 5 s after the screen disappears (e.g. screen rotation).
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState(isLoading = true),
        )

    private fun buildUiState(goals: List<SavingsGoal>): DashboardUiState {
        val (completed, active) = goals.partition { goal -> SavingsCalculator.isCompleted(goal) }
        return DashboardUiState(
            isLoading = false,
            summary = SavingsCalculator.summarize(goals),
            activeGoals = active,
            completedGoals = completed,
            recentlyUpdatedGoals = goals
                .sortedByDescending { goal -> goal.lastActivityAtMillis }
                .take(RECENT_GOAL_COUNT),
        )
    }

    private companion object {
        const val RECENT_GOAL_COUNT = 3
    }
}
