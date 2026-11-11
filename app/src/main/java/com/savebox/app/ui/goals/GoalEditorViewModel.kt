package com.savebox.app.ui.goals

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.savebox.app.data.repository.SavingsRepository
import com.savebox.app.domain.model.GoalDraft
import com.savebox.app.domain.validation.GoalFormErrors
import com.savebox.app.domain.validation.GoalValidator
import com.savebox.app.navigation.GoalEditorRoute
import com.savebox.app.utils.MoneyFormatter
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Everything shown on the Create / Edit goal form.
 * Amounts are kept as TEXT while typing and converted to paise only when saving.
 */
data class GoalEditorUiState(
    val isNewGoal: Boolean = true,
    val isLoading: Boolean = false,
    val name: String = "",
    val emoji: String = GoalPresets.DEFAULT_EMOJI,
    val description: String = "",
    val targetAmountText: String = "",
    val startingAmountText: String = "",
    val deadline: LocalDate? = null,
    val errors: GoalFormErrors = GoalFormErrors(),
    val isSaving: Boolean = false,
    /** Set after a successful save. The screen reacts by navigating away. */
    val savedGoalId: Long? = null,
    /** True if we were asked to edit a goal that doesn't exist (e.g. it was deleted). */
    val goalNotFound: Boolean = false,
)

/**
 * Handles the goal form: holds what the user typed, validates it and saves it.
 *
 * Used for BOTH creating (GoalEditorRoute(goalId = null)) and
 * editing (GoalEditorRoute(goalId = 5)).
 */
class GoalEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val savingsRepository: SavingsRepository,
) : ViewModel() {

    /** The navigation arguments of this screen. */
    private val route: GoalEditorRoute = savedStateHandle.toRoute<GoalEditorRoute>()
    private val editingGoalId: Long? = route.goalId

    /**
     * Compose-observable state: when we assign a new value, the screen redraws.
     * (For simple forms this is easier to read than a StateFlow.)
     */
    var uiState by mutableStateOf(GoalEditorUiState(isNewGoal = editingGoalId == null))
        private set

    init {
        if (editingGoalId != null) {
            loadGoalForEditing(editingGoalId)
        }
    }

    private fun loadGoalForEditing(goalId: Long) {
        uiState = uiState.copy(isLoading = true)
        viewModelScope.launch {
            val goal = savingsRepository.getGoal(goalId)
            uiState = if (goal == null) {
                uiState.copy(isLoading = false, goalNotFound = true)
            } else {
                uiState.copy(
                    isLoading = false,
                    name = goal.name,
                    emoji = goal.emoji,
                    description = goal.description,
                    targetAmountText = MoneyFormatter.paiseToInputText(goal.targetAmountPaise),
                    deadline = goal.deadline,
                )
            }
        }
    }

    // ----- Called by the screen when the user changes something -----
    // Each one also clears that field's error so the red message disappears while typing.

    fun onNameChange(newName: String) {
        uiState = uiState.copy(name = newName, errors = uiState.errors.copy(nameError = null))
    }

    fun onEmojiChange(newEmoji: String) {
        uiState = uiState.copy(emoji = newEmoji)
    }

    fun onPresetSelected(preset: GoalPreset) {
        uiState = uiState.copy(
            name = preset.name,
            emoji = preset.emoji,
            errors = uiState.errors.copy(nameError = null),
        )
    }

    fun onTargetAmountChange(newText: String) {
        uiState = uiState.copy(targetAmountText = newText, errors = uiState.errors.copy(targetAmountError = null))
    }

    fun onStartingAmountChange(newText: String) {
        uiState = uiState.copy(startingAmountText = newText, errors = uiState.errors.copy(startingAmountError = null))
    }

    fun onDescriptionChange(newText: String) {
        uiState = uiState.copy(description = newText, errors = uiState.errors.copy(descriptionError = null))
    }

    fun onDeadlineChange(newDeadline: LocalDate?) {
        uiState = uiState.copy(deadline = newDeadline, errors = uiState.errors.copy(deadlineError = null))
    }

    /** Validate, then create or update the goal. */
    fun save() {
        if (uiState.isSaving) return // ignore double taps

        val state = uiState
        val errors = GoalValidator.validate(
            name = state.name,
            targetAmountText = state.targetAmountText,
            startingAmountText = state.startingAmountText,
            description = state.description,
            deadline = state.deadline,
            today = LocalDate.now(),
            isNewGoal = state.isNewGoal,
        )
        if (errors.hasErrors) {
            uiState = state.copy(errors = errors)
            return
        }

        // The validator already checked this, but we stay safe instead of using "!!".
        val targetAmountPaise = MoneyFormatter.parseToPaise(state.targetAmountText) ?: return

        val draft = GoalDraft(
            name = state.name.trim(),
            emoji = state.emoji,
            description = state.description.trim(),
            targetAmountPaise = targetAmountPaise,
            deadline = state.deadline,
        )
        val startingAmountPaise = MoneyFormatter.parseToPaise(state.startingAmountText) ?: 0L

        uiState = state.copy(isSaving = true)
        viewModelScope.launch {
            val goalId = if (editingGoalId == null) {
                savingsRepository.createGoal(draft, startingAmountPaise)
            } else {
                savingsRepository.updateGoal(editingGoalId, draft)
                editingGoalId
            }
            uiState = uiState.copy(isSaving = false, savedGoalId = goalId)
        }
    }
}
