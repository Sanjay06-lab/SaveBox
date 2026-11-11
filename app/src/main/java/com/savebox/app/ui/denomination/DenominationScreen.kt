package com.savebox.app.ui.denomination

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.savebox.app.domain.calculations.DenominationSummary
import com.savebox.app.domain.calculations.SavingsCalculator
import com.savebox.app.domain.model.Denomination
import com.savebox.app.domain.model.SavingsGoal
import com.savebox.app.ui.AppViewModelProvider
import com.savebox.app.ui.components.ConfirmDialog
import com.savebox.app.ui.components.EmptyState
import com.savebox.app.ui.components.LocalAppSnackbar
import com.savebox.app.ui.components.SaveBoxTopBar
import com.savebox.app.ui.components.SavingsProgressBar
import com.savebox.app.ui.components.StatItem
import com.savebox.app.ui.theme.spacing
import com.savebox.app.utils.MoneyFormatter

/**
 * The denomination grid: one tappable box per note/coin the user plans to save.
 * Tapping an empty box records that amount; tapping a ticked box removes it (with Undo).
 */
@Composable
fun DenominationScreen(
    onBack: () -> Unit,
    viewModel: DenominationViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalAppSnackbar.current

    // If the goal is deleted while this screen is open, close it.
    LaunchedEffect(uiState.isLoading, uiState.goal) {
        if (!uiState.isLoading && uiState.goal == null) onBack()
    }

    Scaffold(
        topBar = {
            SaveBoxTopBar(title = uiState.goal?.let { goal -> "${goal.emoji} ${goal.name}" } ?: "Savings boxes", onBack = onBack)
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val goal = uiState.goal
        if (uiState.isLoading || goal == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyVerticalGrid(
            // As many columns as fit, each at least 48dp wide (the minimum comfortable
            // touch size) - so phones get ~6 columns and tablets many more.
            columns = GridCells.Adaptive(minSize = 48.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = MaterialTheme.spacing.screenPadding,
                end = MaterialTheme.spacing.screenPadding,
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + MaterialTheme.spacing.large,
            ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            fullWidthItem("summary") {
                GridSummaryCard(goal = goal, summary = uiState.summary, uncoveredPaise = uiState.uncoveredPaise)
            }
            fullWidthItem("add") {
                Button(
                    onClick = viewModel::openAddRowDialog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Add a row of boxes")
                }
            }

            if (uiState.rows.isEmpty()) {
                fullWidthItem("empty") {
                    EmptyState(
                        emoji = "🧱",
                        title = "Plan your savings in boxes",
                        message = "Add rows like ₹500 × 110 or ₹100 × 30. Each time you put a note aside, tick a box.",
                    )
                }
            }

            uiState.rows.forEach { row ->
                fullWidthItem("header-${row.id}") {
                    RowHeader(
                        row = row,
                        onChangeQuantity = { viewModel.openEditQuantityDialog(row) },
                        onDelete = { viewModel.openDeleteRowDialog(row) },
                    )
                }
                items(
                    count = row.quantity,
                    key = { boxIndex -> "box-${row.id}-$boxIndex" },
                ) { boxIndex ->
                    SavingsBox(
                        valuePaise = row.valuePaise,
                        boxNumber = boxIndex + 1,
                        isTicked = row.isMarked(boxIndex),
                        onToggle = {
                            viewModel.toggleBox(row.id, boxIndex) { deleted ->
                                snackbar.showUndo("${MoneyFormatter.format(row.valuePaise)} box #${boxIndex + 1} unticked") {
                                    viewModel.restore(deleted)
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    DenominationDialogs(viewModel = viewModel, uncoveredPaise = uiState.uncoveredPaise)
}

/** Shows whichever dialog the ViewModel says is open. */
@Composable
private fun DenominationDialogs(viewModel: DenominationViewModel, uncoveredPaise: Long) {
    when (val dialog = viewModel.dialog) {
        null -> Unit
        is DenominationDialog.AddRow -> AddRowDialog(
            state = dialog,
            uncoveredPaise = uncoveredPaise,
            onValueChange = viewModel::onAddRowValueChange,
            onQuantityChange = viewModel::onAddRowQuantityChange,
            onConfirm = viewModel::confirmAddRow,
            onDismiss = viewModel::dismissDialog,
        )
        is DenominationDialog.EditQuantity -> EditQuantityDialog(
            state = dialog,
            onQuantityChange = viewModel::onEditQuantityChange,
            onConfirm = viewModel::confirmEditQuantity,
            onDismiss = viewModel::dismissDialog,
        )
        is DenominationDialog.ConfirmDeleteRow -> {
            val row = dialog.row
            val tickedAmount = MoneyFormatter.format(row.valuePaise * row.markedCount)
            ConfirmDialog(
                title = "Delete the ${MoneyFormatter.format(row.valuePaise)} boxes?",
                message = if (row.markedCount == 0) {
                    "This row has no ticked boxes."
                } else {
                    "The ${row.markedCount} ticked boxes ($tickedAmount) will also be removed from your savings."
                },
                confirmLabel = "Delete",
                isDestructive = true,
                onConfirm = viewModel::confirmDeleteRow,
                onDismiss = viewModel::dismissDialog,
            )
        }
    }
}

/** Helper: an item that spans the full width of the grid (headers, cards). */
private fun LazyGridScope.fullWidthItem(key: String, content: @Composable () -> Unit) {
    item(key = key, span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun GridSummaryCard(goal: SavingsGoal, summary: DenominationSummary, uncoveredPaise: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem(label = "Boxes", value = "${summary.totalBoxes}", modifier = Modifier.weight(1f))
                StatItem(label = "Ticked", value = "${summary.completedBoxes}", valueColor = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                StatItem(label = "Left", value = "${summary.remainingBoxes}", modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(MaterialTheme.spacing.medium))
            StatItem(
                label = "Saved with boxes",
                value = "${MoneyFormatter.format(summary.savedPaise)} of ${MoneyFormatter.format(summary.plannedPaise)}",
            )
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            SavingsProgressBar(progress = SavingsCalculator.progressFraction(summary.savedPaise, summary.plannedPaise))

            if (summary.totalBoxes > 0) {
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                Text(
                    text = planMessage(goal, summary, uncoveredPaise),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Compares the grid's total with the goal's target. */
private fun planMessage(goal: SavingsGoal, summary: DenominationSummary, uncoveredPaise: Long): String {
    val target = MoneyFormatter.format(goal.targetAmountPaise)
    return when {
        uncoveredPaise > 0 -> "Your boxes cover ${MoneyFormatter.format(summary.plannedPaise)} of your $target goal. " +
            "Add boxes worth ${MoneyFormatter.format(uncoveredPaise)} more to cover it all."
        summary.plannedPaise == goal.targetAmountPaise -> "Your boxes add up to exactly your $target goal. 🎯"
        else -> "Your boxes add up to ${MoneyFormatter.format(summary.plannedPaise - goal.targetAmountPaise)} more than your $target goal."
    }
}

@Composable
private fun RowHeader(row: Denomination, onChangeQuantity: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val value = MoneyFormatter.format(row.valuePaise)

    Column(modifier = Modifier.padding(top = MaterialTheme.spacing.medium)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("$value × ${row.quantity}", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${row.markedCount} of ${row.quantity} ticked · ${MoneyFormatter.format(row.valuePaise * row.markedCount)} saved",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Options for $value boxes")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Change number of boxes") },
                        onClick = {
                            menuOpen = false
                            onChangeQuantity()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete row", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
        SavingsProgressBar(
            progress = SavingsCalculator.progressFraction(row.markedCount.toLong(), row.quantity.toLong()),
            height = 4.dp,
        )
    }
}

/**
 * One square box. Ticked = filled green with a check mark.
 * Uses Modifier.toggleable so TalkBack reads it as a checkbox ("ticked"/"not ticked").
 */
@Composable
private fun SavingsBox(valuePaise: Long, boxNumber: Int, isTicked: Boolean, onToggle: () -> Unit) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isTicked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        label = "boxColor",
    )
    val contentColor = if (isTicked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val borderColor = if (isTicked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(backgroundColor, MaterialTheme.shapes.small)
            .border(1.dp, borderColor, MaterialTheme.shapes.small)
            .toggleable(value = isTicked, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = "${MoneyFormatter.format(valuePaise)} box $boxNumber" },
        contentAlignment = Alignment.Center,
    ) {
        if (isTicked) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = contentColor, modifier = Modifier.size(22.dp))
        } else {
            Text(
                text = MoneyFormatter.compact(valuePaise),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}
