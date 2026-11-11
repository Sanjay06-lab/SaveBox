package com.savebox.app.ui.denomination

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.savebox.app.data.repository.SavingsRepository
import com.savebox.app.domain.calculations.DenominationSummary
import com.savebox.app.domain.calculations.SavingsCalculator
import com.savebox.app.domain.model.AddDenominationResult
import com.savebox.app.domain.model.BoxToggleResult
import com.savebox.app.domain.model.DeletedTransaction
import com.savebox.app.domain.model.Denomination
import com.savebox.app.domain.model.SavingsGoal
import com.savebox.app.domain.validation.DenominationFormErrors
import com.savebox.app.domain.validation.SavingsValidator
import com.savebox.app.navigation.DenominationRoute
import com.savebox.app.utils.MoneyFormatter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class DenominationUiState(
    val isLoading: Boolean = true,
    val goal: SavingsGoal? = null,
    val rows: List<Denomination> = emptyList(),
    val summary: DenominationSummary = DenominationSummary.EMPTY,
) {
    /** How much of the goal's target the grid does NOT cover yet (0 if fully covered). */
    val uncoveredPaise: Long
        get() = ((goal?.targetAmountPaise ?: 0L) - summary.plannedPaise).coerceAtLeast(0)
}

/** Which dialog is open on the grid screen (null = none). */
sealed interface DenominationDialog {
    data class AddRow(
        val valueText: String = "",
        val quantityText: String = "",
        val errors: DenominationFormErrors = DenominationFormErrors(),
    ) : DenominationDialog

    data class EditQuantity(
        val row: Denomination,
        val quantityText: String,
        val error: String? = null,
    ) : DenominationDialog

    data class ConfirmDeleteRow(val row: Denomination) : DenominationDialog
}

/**
 * The denomination grid of one goal: rows of boxes that the user ticks as they save.
 */
class DenominationViewModel(
    savedStateHandle: SavedStateHandle,
    private val savingsRepository: SavingsRepository,
) : ViewModel() {

    val goalId: Long = savedStateHandle.toRoute<DenominationRoute>().goalId

    val uiState: StateFlow<DenominationUiState> = combine(
        savingsRepository.observeGoal(goalId),
        savingsRepository.observeDenominations(goalId),
    ) { goal, rows ->
        DenominationUiState(
            isLoading = false,
            goal = goal,
            rows = rows,
            summary = SavingsCalculator.summarizeDenominations(rows),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DenominationUiState())

    var dialog by mutableStateOf<DenominationDialog?>(null)
        private set

    /**
     * Box taps are handled one at a time. Without this, two very fast taps on the same box
     * could both read "not ticked" and both try to tick it. (The database would also reject
     * the second one, but it's nicer not to get that far.)
     */
    private val toggleMutex = Mutex()

    /**
     * Tick or un-tick a box.
     * @param onUnmarked called when a box was un-ticked, so the screen can offer Undo.
     */
    fun toggleBox(denominationId: Long, boxIndex: Int, onUnmarked: (DeletedTransaction) -> Unit) {
        viewModelScope.launch {
            toggleMutex.withLock {
                val result = savingsRepository.toggleBox(denominationId, boxIndex)
                if (result is BoxToggleResult.Unmarked) {
                    onUnmarked(result.deleted)
                }
            }
        }
    }

    /** Called by the app-level Undo snackbar (see AddSavingsViewModel for why it's suspend). */
    suspend fun restore(deleted: DeletedTransaction) {
        savingsRepository.restoreTransaction(deleted)
    }

    // ----- Add a row -----

    fun openAddRowDialog() {
        dialog = DenominationDialog.AddRow()
    }

    fun onAddRowValueChange(text: String) {
        val current = dialog as? DenominationDialog.AddRow ?: return
        dialog = current.copy(valueText = text, errors = current.errors.copy(valueError = null))
    }

    fun onAddRowQuantityChange(text: String) {
        val current = dialog as? DenominationDialog.AddRow ?: return
        // Only digits are allowed in the quantity field.
        if (text.all { character -> character.isDigit() } && text.length <= 4) {
            dialog = current.copy(quantityText = text, errors = current.errors.copy(quantityError = null))
        }
    }

    fun confirmAddRow() {
        val current = dialog as? DenominationDialog.AddRow ?: return
        val errors = SavingsValidator.validateDenomination(current.valueText, current.quantityText)
        if (errors.hasErrors) {
            dialog = current.copy(errors = errors)
            return
        }
        val valuePaise = MoneyFormatter.parseToPaise(current.valueText) ?: return
        val quantity = current.quantityText.trim().toIntOrNull() ?: return

        viewModelScope.launch {
            when (savingsRepository.addDenomination(goalId, valuePaise, quantity)) {
                is AddDenominationResult.Added -> dialog = null
                AddDenominationResult.DuplicateValue -> dialog = current.copy(
                    errors = current.errors.copy(
                        valueError = "You already have ${MoneyFormatter.format(valuePaise)} boxes. Change that row's number of boxes instead.",
                    ),
                )
            }
        }
    }

    // ----- Change a row's number of boxes -----

    fun openEditQuantityDialog(row: Denomination) {
        dialog = DenominationDialog.EditQuantity(row = row, quantityText = row.quantity.toString())
    }

    fun onEditQuantityChange(text: String) {
        val current = dialog as? DenominationDialog.EditQuantity ?: return
        if (text.all { character -> character.isDigit() } && text.length <= 4) {
            dialog = current.copy(quantityText = text, error = null)
        }
    }

    fun confirmEditQuantity() {
        val current = dialog as? DenominationDialog.EditQuantity ?: return
        val error = SavingsValidator.validateQuantity(current.quantityText)
        if (error != null) {
            dialog = current.copy(error = error)
            return
        }
        val quantity = current.quantityText.trim().toIntOrNull() ?: return

        viewModelScope.launch {
            val updated = savingsRepository.updateDenominationQuantity(current.row.id, quantity)
            dialog = if (updated) {
                null
            } else {
                val highestTicked = (current.row.markedBoxIndices.maxOrNull() ?: 0) + 1
                current.copy(error = "Box #$highestTicked is ticked. Untick boxes above $quantity first, or keep at least $highestTicked boxes.")
            }
        }
    }

    // ----- Delete a row -----

    fun openDeleteRowDialog(row: Denomination) {
        dialog = DenominationDialog.ConfirmDeleteRow(row)
    }

    fun confirmDeleteRow() {
        val current = dialog as? DenominationDialog.ConfirmDeleteRow ?: return
        dialog = null
        viewModelScope.launch {
            savingsRepository.deleteDenomination(current.row.id)
        }
    }

    fun dismissDialog() {
        dialog = null
    }
}
