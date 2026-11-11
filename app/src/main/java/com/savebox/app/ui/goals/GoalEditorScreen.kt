package com.savebox.app.ui.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.savebox.app.domain.validation.GoalValidator
import com.savebox.app.ui.AppViewModelProvider
import com.savebox.app.ui.components.AmountTextField
import com.savebox.app.ui.components.DateSelectorField
import com.savebox.app.ui.components.SaveBoxDatePickerDialog
import com.savebox.app.ui.components.EmptyState
import com.savebox.app.ui.components.SaveBoxTopBar
import com.savebox.app.ui.components.SectionHeader
import com.savebox.app.ui.theme.spacing
import java.time.LocalDate

/**
 * Create a new goal or edit an existing one.
 *
 * @param onBack close without saving.
 * @param onSaved called after saving, with the goal id and whether it was a new goal.
 */
@Composable
fun GoalEditorScreen(
    onBack: () -> Unit,
    onSaved: (goalId: Long, wasNewGoal: Boolean) -> Unit,
    viewModel: GoalEditorViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState = viewModel.uiState

    // When the ViewModel reports a successful save, leave this screen (only once).
    LaunchedEffect(uiState.savedGoalId) {
        val savedGoalId = uiState.savedGoalId
        if (savedGoalId != null) {
            onSaved(savedGoalId, uiState.isNewGoal)
        }
    }

    Scaffold(
        topBar = {
            SaveBoxTopBar(
                title = if (uiState.isNewGoal) "New goal" else "Edit goal",
                onBack = onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.goalNotFound -> {
                EmptyState(
                    emoji = "🔍",
                    title = "Goal not found",
                    message = "This goal may have been deleted.",
                    actionLabel = "Go back",
                    onAction = onBack,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            else -> {
                GoalEditorForm(
                    uiState = uiState,
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalEditorForm(
    uiState: GoalEditorUiState,
    viewModel: GoalEditorViewModel,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding() // move content above the keyboard
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        // Example goals (only when creating).
        if (uiState.isNewGoal) {
            SectionHeader(title = "Need an idea?")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                GoalPresets.examples.forEach { preset ->
                    SuggestionChip(
                        onClick = { viewModel.onPresetSelected(preset) },
                        label = { Text("${preset.emoji}  ${preset.name}") },
                    )
                }
            }
        }

        SectionHeader(title = "Icon")
        EmojiPicker(selectedEmoji = uiState.emoji, onEmojiSelected = viewModel::onEmojiChange)

        SectionHeader(title = "Details")
        OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Goal name") },
            placeholder = { Text("e.g. Buy a PlayStation 5") },
            singleLine = true,
            isError = uiState.errors.nameError != null,
            supportingText = {
                Text(uiState.errors.nameError ?: "${uiState.name.length}/${GoalValidator.MAX_NAME_LENGTH}")
            },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        AmountTextField(
            value = uiState.targetAmountText,
            onValueChange = viewModel::onTargetAmountChange,
            label = "Target amount",
            errorMessage = uiState.errors.targetAmountError,
        )

        if (uiState.isNewGoal) {
            AmountTextField(
                value = uiState.startingAmountText,
                onValueChange = viewModel::onStartingAmountChange,
                label = "Already saved (optional)",
                errorMessage = uiState.errors.startingAmountError,
                supportingText = "Money you have already put aside for this goal",
            )
        }

        DateSelectorField(
            label = "Deadline (optional)",
            date = uiState.deadline,
            emptyText = "No deadline - tap to choose",
            errorMessage = uiState.errors.deadlineError,
            onClick = { showDatePicker = true },
            onClear = { viewModel.onDeadlineChange(null) },
        )

        OutlinedTextField(
            value = uiState.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text("Description (optional)") },
            placeholder = { Text("Why does this goal matter to you?") },
            minLines = 3,
            isError = uiState.errors.descriptionError != null,
            supportingText = {
                Text(uiState.errors.descriptionError ?: "${uiState.description.length}/${GoalValidator.MAX_DESCRIPTION_LENGTH}")
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = viewModel::save,
            enabled = !uiState.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Text(
                text = if (uiState.isNewGoal) "Create goal" else "Save changes",
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.height(MaterialTheme.spacing.large))
    }

    if (showDatePicker) {
        SaveBoxDatePickerDialog(
            initialDate = uiState.deadline,
            earliestDate = LocalDate.now(), // a deadline can't be in the past
            onDateSelected = { date ->
                viewModel.onDeadlineChange(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

/** A wrapping grid of emoji. The selected one gets a green border. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmojiPicker(selectedEmoji: String, onEmojiSelected: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        GoalPresets.emojiChoices.forEach { emoji ->
            val isSelected = emoji == selectedEmoji
            Box(
                modifier = Modifier
                    .size(48.dp) // 48dp = comfortable touch target
                    .background(
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small,
                    )
                    .border(
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        ),
                        shape = MaterialTheme.shapes.small,
                    )
                    .clickable(role = Role.RadioButton) { onEmojiSelected(emoji) }
                    .semantics {
                        selected = isSelected
                        contentDescription = "Icon $emoji"
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, fontSize = 22.sp)
            }
        }
    }
}
