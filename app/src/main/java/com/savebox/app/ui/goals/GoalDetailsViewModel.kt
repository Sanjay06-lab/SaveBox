package com.savebox.app.ui.goals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.savebox.app.data.repository.SavingsRepository
import com.savebox.app.domain.model.DeletedTransaction
import com.savebox.app.domain.model.SavingsGoal
import com.savebox.app.domain.model.SavingsTransaction
import com.savebox.app.navigation.GoalDetailsRoute
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The three possible states of the Goal Details screen.
 * A "sealed interface" means these are the ONLY options, so the screen's
 * `when` statement must handle every one of them.
 */
sealed interface GoalDetailsUiState {
    data object Loading : GoalDetailsUiState

    /** The goal doesn't exist (for example, it was just deleted). */
    data object NotFound : GoalDetailsUiState

    data class Ready(
        val goal: SavingsGoal,
        val transactions: List<SavingsTransaction>,
    ) : GoalDetailsUiState
}

class GoalDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val savingsRepository: SavingsRepository,
) : ViewModel() {

    val goalId: Long = savedStateHandle.toRoute<GoalDetailsRoute>().goalId

    /** Goal + its savings history, kept up to date automatically. */
    val uiState: StateFlow<GoalDetailsUiState> = combine(
        savingsRepository.observeGoal(goalId),
        savingsRepository.observeTransactionsForGoal(goalId),
    ) { goal, transactions ->
        if (goal == null) {
            GoalDetailsUiState.NotFound
        } else {
            GoalDetailsUiState.Ready(goal, transactions)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GoalDetailsUiState.Loading,
    )

    /**
     * Delete this goal and its savings records.
     * The goal Flow then sends null -> NotFound, and the screen closes itself.
     */
    fun deleteGoal() {
        viewModelScope.launch {
            savingsRepository.deleteGoal(goalId)
        }
    }

    /** Delete one savings entry; [onDeleted] receives what's needed for Undo. */
    fun deleteTransaction(transactionId: Long, onDeleted: (DeletedTransaction) -> Unit) {
        viewModelScope.launch {
            val deleted = savingsRepository.deleteTransaction(transactionId)
            if (deleted != null) onDeleted(deleted)
        }
    }

    /** Called by the app-level Undo snackbar (see AddSavingsViewModel for why it's suspend). */
    suspend fun restoreTransaction(deleted: DeletedTransaction) {
        savingsRepository.restoreTransaction(deleted)
    }
}
