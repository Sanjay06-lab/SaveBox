package com.savebox.app.ui.transactions

import android.database.SQLException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.savebox.app.data.repository.SavingsRepository
import com.savebox.app.domain.model.DeletedTransaction
import com.savebox.app.domain.model.SavingsEntry
import com.savebox.app.domain.model.SavingsGoal
import com.savebox.app.domain.model.TransactionType
import com.savebox.app.domain.validation.SavingsFormErrors
import com.savebox.app.domain.validation.SavingsValidator
import com.savebox.app.navigation.AddSavingsRoute
import com.savebox.app.utils.MoneyFormatter
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Everything shown on the Add / Edit savings form. */
data class AddSavingsUiState(
    val isNewEntry: Boolean = true,
    val isLoading: Boolean = false,
    val goals: List<SavingsGoal> = emptyList(),
    val goalsLoaded: Boolean = false,
    val selectedGoalId: Long? = null,
    val amountText: String = "",
    val date: LocalDate = LocalDate.now(),
    /** Time of day kept when editing, so changing only the date doesn't move the time. */
    val timeOfDay: LocalTime = LocalTime.now(),
    val note: String = "",
    val source: String = "",
    /** True when editing a record created by a denomination box (amount/goal are locked). */
    val isBoxSaving: Boolean = false,
    val errors: SavingsFormErrors = SavingsFormErrors(),
    val isSaving: Boolean = false,
    /** A problem that isn't about one field, e.g. the goal was deleted meanwhile. */
    val generalError: String? = null,
    val notFound: Boolean = false,
    /** Set after saving; the screen closes and shows this message. */
    val savedResult: SavedResult? = null,
    /** Set after deleting from this screen. */
    val deleted: DeletedTransaction? = null,
) {
    val selectedGoal: SavingsGoal? get() = goals.firstOrNull { goal -> goal.id == selectedGoalId }
}

data class SavedResult(val transactionId: Long, val message: String, val wasNewEntry: Boolean)

/**
 * Handles adding a custom saving, or editing any existing saving.
 *   AddSavingsRoute(goalId = 3)          -> new saving, goal 3 pre-selected
 *   AddSavingsRoute()                    -> new saving, user picks the goal
 *   AddSavingsRoute(transactionId = 10)  -> edit saving 10
 */
class AddSavingsViewModel(
    savedStateHandle: SavedStateHandle,
    private val savingsRepository: SavingsRepository,
) : ViewModel() {

    private val route: AddSavingsRoute = savedStateHandle.toRoute<AddSavingsRoute>()
    private val editingTransactionId: Long? = route.transactionId

    var uiState by mutableStateOf(
        AddSavingsUiState(
            isNewEntry = editingTransactionId == null,
            isLoading = editingTransactionId != null,
            selectedGoalId = route.goalId,
        ),
    )
        private set

    init {
        observeGoals()
        if (editingTransactionId != null) loadTransaction(editingTransactionId)
    }

    /** Keeps the goal list for the drop-down up to date. */
    private fun observeGoals() {
        viewModelScope.launch {
            savingsRepository.observeGoals().collect { goals ->
                // If nothing is selected and there is only one goal, select it for the user.
                val selected = uiState.selectedGoalId ?: goals.singleOrNull()?.id
                uiState = uiState.copy(goals = goals, goalsLoaded = true, selectedGoalId = selected)
            }
        }
    }

    private fun loadTransaction(transactionId: Long) {
        viewModelScope.launch {
            val transaction = savingsRepository.getTransaction(transactionId)
            if (transaction == null) {
                uiState = uiState.copy(isLoading = false, notFound = true)
                return@launch
            }
            val savedAt = Instant.ofEpochMilli(transaction.savedAtMillis).atZone(ZoneId.systemDefault())
            uiState = uiState.copy(
                isLoading = false,
                selectedGoalId = transaction.goalId,
                amountText = MoneyFormatter.paiseToInputText(transaction.amountPaise),
                date = savedAt.toLocalDate(),
                timeOfDay = savedAt.toLocalTime(),
                note = transaction.note,
                source = transaction.source,
                isBoxSaving = transaction.type == TransactionType.DENOMINATION,
            )
        }
    }

    // ----- Form changes -----

    fun onGoalSelected(goalId: Long) {
        uiState = uiState.copy(selectedGoalId = goalId, errors = uiState.errors.copy(goalError = null), generalError = null)
    }

    fun onAmountChange(text: String) {
        uiState = uiState.copy(amountText = text, errors = uiState.errors.copy(amountError = null))
    }

    /** Quick-amount chips (₹50, ₹100...) set the amount directly. */
    fun onQuickAmount(rupees: Long) {
        onAmountChange(rupees.toString())
    }

    fun onDateChange(date: LocalDate) {
        uiState = uiState.copy(date = date, errors = uiState.errors.copy(dateError = null))
    }

    fun onNoteChange(text: String) {
        uiState = uiState.copy(note = text, errors = uiState.errors.copy(noteError = null))
    }

    fun onSourceChange(text: String) {
        uiState = uiState.copy(source = text, errors = uiState.errors.copy(sourceError = null))
    }

    // ----- Actions -----

    fun save() {
        val state = uiState
        if (state.isSaving) return

        val errors = SavingsValidator.validateEntry(
            goalId = state.selectedGoalId,
            amountText = state.amountText,
            note = state.note,
            source = state.source,
            savedDate = state.date,
            today = LocalDate.now(),
        )
        if (errors.hasErrors) {
            uiState = state.copy(errors = errors)
            return
        }
        val goalId = state.selectedGoalId ?: return
        val amountPaise = MoneyFormatter.parseToPaise(state.amountText) ?: return

        val entry = SavingsEntry(
            goalId = goalId,
            amountPaise = amountPaise,
            note = state.note.trim(),
            source = state.source.trim(),
            savedAtMillis = state.date.atTime(state.timeOfDay).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )

        uiState = state.copy(isSaving = true, generalError = null)
        viewModelScope.launch {
            try {
                val result = if (editingTransactionId == null) {
                    val newId = savingsRepository.addSavings(entry)
                    SavedResult(
                        transactionId = newId,
                        message = "${MoneyFormatter.format(amountPaise)} saved for ${state.selectedGoal?.name ?: "your goal"}",
                        wasNewEntry = true,
                    )
                } else {
                    val updated = savingsRepository.updateSavings(editingTransactionId, entry)
                    if (!updated) {
                        uiState = uiState.copy(isSaving = false, notFound = true)
                        return@launch
                    }
                    SavedResult(transactionId = editingTransactionId, message = "Changes saved", wasNewEntry = false)
                }
                uiState = uiState.copy(isSaving = false, savedResult = result)
            } catch (exception: SQLException) {
                // Most likely the goal was deleted while this form was open.
                uiState = uiState.copy(isSaving = false, generalError = "Couldn't save: that goal no longer exists.")
            }
        }
    }

    /** Delete the record being edited. The screen closes and offers Undo. */
    fun delete() {
        val transactionId = editingTransactionId ?: return
        viewModelScope.launch {
            val deleted = savingsRepository.deleteTransaction(transactionId)
            uiState = if (deleted == null) uiState.copy(notFound = true) else uiState.copy(deleted = deleted)
        }
    }

    /*
     * The two functions below are "suspend" and do NOT use viewModelScope on purpose:
     * they are called from the app-level Undo snackbar, which may run after this screen
     * (and its viewModelScope) has already closed.
     */

    suspend fun undoAdd(transactionId: Long) {
        savingsRepository.deleteTransaction(transactionId)
    }

    suspend fun restore(deleted: DeletedTransaction) {
        savingsRepository.restoreTransaction(deleted)
    }
}

/** Suggestions shown under the "Source" field. Edit freely. */
object SavingsSources {
    val suggestions = listOf("Pocket money", "Salary", "Gift", "Side income", "Spare change")
}

/** Quick-amount chips in rupees. Edit freely. */
object QuickAmounts {
    val rupees = listOf(50L, 100L, 200L, 500L, 1_000L)
}
