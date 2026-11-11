package com.savebox.app.ui.transactions

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.savebox.app.domain.validation.SavingsValidator
import com.savebox.app.ui.AppViewModelProvider
import com.savebox.app.ui.components.AmountTextField
import com.savebox.app.ui.components.DateSelectorField
import com.savebox.app.ui.components.EmptyState
import com.savebox.app.ui.components.LocalAppSnackbar
import com.savebox.app.ui.components.SaveBoxDatePickerDialog
import com.savebox.app.ui.components.SaveBoxTopBar
import com.savebox.app.ui.components.SectionHeader
import com.savebox.app.ui.theme.spacing
import com.savebox.app.utils.MoneyFormatter
import java.time.LocalDate

/**
 * Add a custom amount to a goal, or edit any existing savings entry.
 *
 * @param onDone close this screen (after saving, deleting or when there's nothing to edit).
 * @param onCreateGoal used by the "no goals yet" state.
 */
@Composable
fun AddSavingsScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    onCreateGoal: () -> Unit,
    viewModel: AddSavingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState = viewModel.uiState
    val snackbar = LocalAppSnackbar.current

    // After a successful save: show a message (with Undo for new entries) and close.
    LaunchedEffect(uiState.savedResult) {
        val result = uiState.savedResult ?: return@LaunchedEffect
        if (result.wasNewEntry) {
            snackbar.showUndo(result.message) { viewModel.undoAdd(result.transactionId) }
        } else {
            snackbar.showMessage(result.message)
        }
        onDone()
    }

    // After deleting from this screen: offer Undo and close.
    LaunchedEffect(uiState.deleted) {
        val deleted = uiState.deleted ?: return@LaunchedEffect
        snackbar.showUndo("Savings entry deleted") { viewModel.restore(deleted) }
        onDone()
    }

    LaunchedEffect(uiState.notFound) {
        if (uiState.notFound) {
            snackbar.showMessage("That entry no longer exists")
            onDone()
        }
    }

    Scaffold(
        topBar = {
            SaveBoxTopBar(
                title = if (uiState.isNewEntry) "Add savings" else "Edit savings",
                onBack = onBack,
                actions = {
                    if (!uiState.isNewEntry && !uiState.isLoading) {
                        IconButton(onClick = viewModel::delete) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete entry")
                        }
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when {
            uiState.isLoading || !uiState.goalsLoaded -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.goals.isEmpty() -> {
                EmptyState(
                    emoji = "🎯",
                    title = "Create a goal first",
                    message = "Savings are always recorded for a goal.",
                    actionLabel = "Create a goal",
                    onAction = onCreateGoal,
                    modifier = Modifier.padding(innerPadding),
                )
            }

            else -> {
                AddSavingsForm(uiState = uiState, viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddSavingsForm(
    uiState: AddSavingsUiState,
    viewModel: AddSavingsViewModel,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = MaterialTheme.spacing.screenPadding),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        if (uiState.isBoxSaving) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                Text(
                    text = "🧱 This saving came from a denomination box, so its amount and goal can't change. " +
                        "You can still edit the date, source and note.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(MaterialTheme.spacing.medium),
                )
            }
        }

        GoalPicker(
            uiState = uiState,
            enabled = !uiState.isBoxSaving,
            onGoalSelected = viewModel::onGoalSelected,
        )

        AmountTextField(
            value = uiState.amountText,
            onValueChange = viewModel::onAmountChange,
            label = "Amount saved",
            errorMessage = uiState.errors.amountError,
            enabled = !uiState.isBoxSaving,
        )
        if (!uiState.isBoxSaving) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                QuickAmounts.rupees.forEach { rupees ->
                    val paiseText = MoneyFormatter.paiseToInputText(MoneyFormatter.rupeesToPaise(rupees))
                    FilterChip(
                        selected = uiState.amountText == paiseText,
                        onClick = { viewModel.onQuickAmount(rupees) },
                        label = { Text(MoneyFormatter.format(MoneyFormatter.rupeesToPaise(rupees))) },
                    )
                }
            }
        }

        DateSelectorField(
            label = "Date saved",
            date = uiState.date,
            emptyText = "Choose a date",
            errorMessage = uiState.errors.dateError,
            onClick = { showDatePicker = true },
        )

        SectionHeader(title = "Optional")
        OutlinedTextField(
            value = uiState.source,
            onValueChange = viewModel::onSourceChange,
            label = { Text("Source") },
            placeholder = { Text("Where did this money come from?") },
            singleLine = true,
            isError = uiState.errors.sourceError != null,
            supportingText = uiState.errors.sourceError?.let { error -> { Text(error) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
            SavingsSources.suggestions.forEach { suggestion ->
                SuggestionChip(onClick = { viewModel.onSourceChange(suggestion) }, label = { Text(suggestion) })
            }
        }

        OutlinedTextField(
            value = uiState.note,
            onValueChange = viewModel::onNoteChange,
            label = { Text("Note") },
            placeholder = { Text("e.g. Skipped eating out") },
            minLines = 2,
            isError = uiState.errors.noteError != null,
            supportingText = {
                Text(uiState.errors.noteError ?: "${uiState.note.length}/${SavingsValidator.MAX_NOTE_LENGTH}")
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier.fillMaxWidth(),
        )

        if (uiState.generalError != null) {
            Text(uiState.generalError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = viewModel::save,
            enabled = !uiState.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        ) {
            Text(if (uiState.isNewEntry) "Add savings" else "Save changes", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(MaterialTheme.spacing.large))
    }

    if (showDatePicker) {
        SaveBoxDatePickerDialog(
            initialDate = uiState.date,
            latestDate = LocalDate.now(), // savings can't be recorded in the future
            onDateSelected = { date ->
                viewModel.onDateChange(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

/** Drop-down list of goals. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalPicker(
    uiState: AddSavingsUiState,
    enabled: Boolean,
    onGoalSelected: (Long) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val selected = uiState.selectedGoal

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { wantsOpen -> if (enabled) expanded = wantsOpen },
    ) {
        OutlinedTextField(
            value = selected?.let { goal -> "${goal.emoji}  ${goal.name}" } ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Goal") },
            placeholder = { Text("Choose a goal") },
            isError = uiState.errors.goalError != null,
            supportingText = uiState.errors.goalError?.let { error -> { Text(error) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            uiState.goals.forEach { goal ->
                DropdownMenuItem(
                    text = { Text("${goal.emoji}  ${goal.name}") },
                    onClick = {
                        onGoalSelected(goal.id)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
