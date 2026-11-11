package com.savebox.app.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savebox.app.data.repository.SavingsRepository
import com.savebox.app.domain.calculations.MonthlySavings
import com.savebox.app.domain.calculations.SavingsByType
import com.savebox.app.domain.calculations.SavingsCalculator
import com.savebox.app.domain.model.DeletedTransaction
import com.savebox.app.domain.model.TransactionWithGoal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId

/** A goal shown as a filter chip on the History screen. */
data class GoalFilter(val goalId: Long, val label: String)

/** One month in the list: its totals and its entries. */
data class MonthSection(
    val summary: MonthlySavings,
    val entries: List<TransactionWithGoal>,
)

data class HistoryUiState(
    val isLoading: Boolean = true,
    val hasAnyTransactions: Boolean = false,
    val goalFilters: List<GoalFilter> = emptyList(),
    /** null = "All goals". */
    val selectedGoalId: Long? = null,
    val months: List<MonthSection> = emptyList(),
    val byType: SavingsByType = SavingsByType(0, 0, 0),
    val thisMonthPaise: Long = 0,
)

/**
 * All savings records, grouped by month, with totals and a goal filter.
 * All maths comes from SavingsCalculator.
 */
class HistoryViewModel(
    private val savingsRepository: SavingsRepository,
) : ViewModel() {

    private val selectedGoalId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        savingsRepository.observeAllTransactions(),
        selectedGoalId,
    ) { allEntries, goalFilter ->
        buildUiState(allEntries, goalFilter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    private fun buildUiState(allEntries: List<TransactionWithGoal>, goalFilter: Long?): HistoryUiState {
        // One chip per goal that has savings, in order of first appearance.
        val filters = allEntries
            .distinctBy { entry -> entry.transaction.goalId }
            .map { entry -> GoalFilter(entry.transaction.goalId, "${entry.goalEmoji} ${entry.goalName}") }

        // If the filtered goal disappeared (deleted), fall back to "All".
        val activeFilter = goalFilter?.takeIf { id -> filters.any { filter -> filter.goalId == id } }
        val visibleEntries = if (activeFilter == null) {
            allEntries
        } else {
            allEntries.filter { entry -> entry.transaction.goalId == activeFilter }
        }

        val zone = ZoneId.systemDefault()
        val visibleTransactions = visibleEntries.map { entry -> entry.transaction }
        val entriesByMonth = visibleEntries.groupBy { entry ->
            SavingsCalculator.yearMonthOf(entry.transaction.savedAtMillis, zone)
        }
        val months = SavingsCalculator.monthlySummary(visibleTransactions, zone).map { summary ->
            MonthSection(summary = summary, entries = entriesByMonth[summary.month].orEmpty())
        }
        val currentMonth = YearMonth.now(zone)

        return HistoryUiState(
            isLoading = false,
            hasAnyTransactions = allEntries.isNotEmpty(),
            goalFilters = filters,
            selectedGoalId = activeFilter,
            months = months,
            byType = SavingsCalculator.totalsByType(visibleTransactions),
            thisMonthPaise = months.firstOrNull { section -> section.summary.month == currentMonth }?.summary?.totalPaise ?: 0,
        )
    }

    fun selectGoalFilter(goalId: Long?) {
        selectedGoalId.value = goalId
    }

    /** Delete an entry; [onDeleted] receives what's needed for Undo. */
    fun delete(transactionId: Long, onDeleted: (DeletedTransaction) -> Unit) {
        viewModelScope.launch {
            val deleted = savingsRepository.deleteTransaction(transactionId)
            if (deleted != null) onDeleted(deleted)
        }
    }

    /** Called by the app-level Undo snackbar (see AddSavingsViewModel for why it's suspend). */
    suspend fun restore(deleted: DeletedTransaction) {
        savingsRepository.restoreTransaction(deleted)
    }
}
