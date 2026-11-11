package com.savebox.app.ui.goals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.savebox.app.domain.calculations.SavingsCalculator
import com.savebox.app.domain.model.MotivationMessages
import com.savebox.app.domain.model.SavingsGoal
import com.savebox.app.domain.model.SavingsTransaction
import com.savebox.app.ui.AppViewModelProvider
import com.savebox.app.ui.components.ConfirmDialog
import com.savebox.app.ui.components.EmojiBadge
import com.savebox.app.ui.components.LocalAppSnackbar
import com.savebox.app.ui.components.ProgressRing
import com.savebox.app.ui.components.SaveBoxTopBar
import com.savebox.app.ui.components.SectionHeader
import com.savebox.app.ui.components.StatItem
import com.savebox.app.ui.components.TransactionDetailsSheet
import com.savebox.app.ui.components.TransactionRow
import com.savebox.app.ui.theme.spacing
import com.savebox.app.utils.DateFormatter
import com.savebox.app.utils.MoneyFormatter
import java.time.LocalDate

/**
 * Details of one goal: progress, numbers, deadline, history and actions.
 */
@Composable
fun GoalDetailsScreen(
    onBack: () -> Unit,
    onEditGoal: (goalId: Long) -> Unit,
    onAddSavings: (goalId: Long) -> Unit,
    onOpenBoxes: (goalId: Long) -> Unit,
    onEditTransaction: (transactionId: Long) -> Unit,
    viewModel: GoalDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    // Which savings entry's details sheet is open.
    var openTransactionId by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbar = LocalAppSnackbar.current

    // If the goal disappears (deleted here or elsewhere), close this screen.
    LaunchedEffect(uiState) {
        if (uiState is GoalDetailsUiState.NotFound) onBack()
    }

    Scaffold(
        topBar = {
            val title = (uiState as? GoalDetailsUiState.Ready)?.goal?.name ?: ""
            SaveBoxTopBar(
                title = title,
                onBack = onBack,
                actions = {
                    if (uiState is GoalDetailsUiState.Ready) {
                        IconButton(onClick = { onEditGoal(viewModel.goalId) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit goal")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete goal")
                        }
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when (val state = uiState) {
            GoalDetailsUiState.Loading, GoalDetailsUiState.NotFound -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is GoalDetailsUiState.Ready -> {
                GoalDetailsContent(
                    goal = state.goal,
                    transactions = state.transactions,
                    contentPadding = innerPadding,
                    onAddSavings = { onAddSavings(state.goal.id) },
                    onOpenBoxes = { onOpenBoxes(state.goal.id) },
                    onTransactionClick = { transactionId -> openTransactionId = transactionId },
                )
            }
        }
    }

    // Details sheet for a tapped savings entry.
    val ready = uiState as? GoalDetailsUiState.Ready
    val openTransaction = ready?.transactions?.firstOrNull { transaction -> transaction.id == openTransactionId }
    if (ready != null && openTransaction != null) {
        TransactionDetailsSheet(
            transaction = openTransaction,
            goalLabel = "${ready.goal.emoji} ${ready.goal.name}",
            onEdit = {
                openTransactionId = null
                onEditTransaction(openTransaction.id)
            },
            onDelete = {
                openTransactionId = null
                viewModel.deleteTransaction(openTransaction.id) { deleted ->
                    snackbar.showUndo("${MoneyFormatter.format(deleted.transaction.amountPaise)} entry deleted") {
                        viewModel.restoreTransaction(deleted)
                    }
                }
            },
            onDismiss = { openTransactionId = null },
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete this goal?",
            message = "The goal and all of its savings records will be removed. This can't be undone.",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteGoal()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun GoalDetailsContent(
    goal: SavingsGoal,
    transactions: List<SavingsTransaction>,
    contentPadding: PaddingValues,
    onAddSavings: () -> Unit,
    onOpenBoxes: () -> Unit,
    onTransactionClick: (Long) -> Unit,
) {
    val horizontalPadding = Modifier.padding(horizontal = MaterialTheme.spacing.screenPadding)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + MaterialTheme.spacing.large,
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        item { GoalHeader(goal = goal, modifier = horizontalPadding) }
        item { ProgressCard(goal = goal, transactions = transactions, modifier = horizontalPadding) }
        item { MotivationBanner(goal = goal, modifier = horizontalPadding) }

        if (goal.deadline != null) {
            item { DeadlineCard(goal = goal, modifier = horizontalPadding) }
        }

        item {
            Row(
                modifier = horizontalPadding.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                Button(onClick = onAddSavings, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = ButtonDefaults.IconSpacing))
                    Text("Add savings")
                }
                OutlinedButton(onClick = onOpenBoxes, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                    Text("🧱  Boxes")
                }
            }
        }

        item {
            SectionHeader(
                title = "Savings history",
                trailingText = if (transactions.isEmpty()) null else "${transactions.size}",
                modifier = horizontalPadding,
            )
        }

        if (transactions.isEmpty()) {
            item {
                Text(
                    text = "No savings recorded yet. Your first one will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = horizontalPadding,
                )
            }
        } else {
            items(transactions, key = { transaction -> transaction.id }) { transaction ->
                Column(
                    modifier = Modifier
                        .clickable { onTransactionClick(transaction.id) }
                        .then(horizontalPadding),
                ) {
                    TransactionRow(transaction = transaction)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun GoalHeader(goal: SavingsGoal, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        EmojiBadge(emoji = goal.emoji, size = 64.dp)
        Spacer(Modifier.width(MaterialTheme.spacing.medium))
        Column(modifier = Modifier.weight(1f)) {
            Text(goal.name, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
            if (goal.description.isNotBlank()) {
                Text(goal.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProgressCard(goal: SavingsGoal, transactions: List<SavingsTransaction>, modifier: Modifier = Modifier) {
    val percent = SavingsCalculator.progressPercent(goal)
    val excess = SavingsCalculator.excessAmount(goal)
    val byType = SavingsCalculator.totalsByType(transactions)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ProgressRing(progress = SavingsCalculator.progressFraction(goal), size = 160.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$percent%", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("saved", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(MaterialTheme.spacing.large))

            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem(
                    label = "Saved",
                    value = MoneyFormatter.format(goal.savedAmountPaise),
                    valueColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                StatItem(
                    label = "Remaining",
                    value = MoneyFormatter.format(SavingsCalculator.remainingAmount(goal)),
                    modifier = Modifier.weight(1f),
                )
                StatItem(
                    label = "Target",
                    value = MoneyFormatter.format(goal.targetAmountPaise),
                    modifier = Modifier.weight(1f),
                )
            }

            // How the savings were added: ticked boxes vs. typed-in amounts.
            if (byType.boxesPaise > 0 && byType.manualPaise > 0) {
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                Text(
                    text = "🧱 Boxes ${MoneyFormatter.format(byType.boxesPaise)}  ·  ✍️ Added by hand ${MoneyFormatter.format(byType.manualPaise)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            // Savings above the target are kept and shown, not hidden.
            if (excess > 0) {
                Spacer(Modifier.height(MaterialTheme.spacing.medium))
                Text(
                    text = "🎉 You saved ${MoneyFormatter.format(excess)} more than your target!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun MotivationBanner(goal: SavingsGoal, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(
            text = MotivationMessages.forProgress(SavingsCalculator.progressPercent(goal)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
        )
    }
}

@Composable
private fun DeadlineCard(goal: SavingsGoal, modifier: Modifier = Modifier) {
    val deadline = goal.deadline ?: return
    val daysLeft = SavingsCalculator.daysUntilDeadline(goal, LocalDate.now()) ?: return
    val isOverdue = daysLeft < 0 && !SavingsCalculator.isCompleted(goal)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⏳", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(MaterialTheme.spacing.medium))
            Column(modifier = Modifier.weight(1f)) {
                Text("Deadline", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(DateFormatter.formatDate(deadline), style = MaterialTheme.typography.titleMedium)
            }
            if (!SavingsCalculator.isCompleted(goal)) {
                Text(
                    text = DateFormatter.describeDaysLeft(daysLeft),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
