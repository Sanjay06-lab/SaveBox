package com.savebox.app.ui.transactions

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.savebox.app.domain.model.TransactionWithGoal
import com.savebox.app.ui.AppViewModelProvider
import com.savebox.app.ui.components.EmptyState
import com.savebox.app.ui.components.LocalAppSnackbar
import com.savebox.app.ui.components.ScreenHeader
import com.savebox.app.ui.components.StatItem
import com.savebox.app.ui.components.TransactionDetailsSheet
import com.savebox.app.ui.components.TransactionRow
import com.savebox.app.ui.theme.spacing
import com.savebox.app.utils.MoneyFormatter
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthTitleFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

/**
 * Every savings record of every goal, newest first, grouped by month.
 * Tap an entry to see its details, edit it or delete it (with Undo).
 */
@Composable
fun HistoryScreen(
    onEditTransaction: (transactionId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalAppSnackbar.current
    // Which entry's details sheet is open (saved across rotation).
    var openTransactionId by rememberSaveable { mutableStateOf<Long?>(null) }

    if (uiState.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = MaterialTheme.spacing.large),
    ) {
        item { ScreenHeader(title = "History", subtitle = "Every rupee you've recorded") }

        if (!uiState.hasAnyTransactions) {
            item {
                EmptyState(
                    emoji = "🧾",
                    title = "No savings yet",
                    message = "When you add savings or tick a box, it will appear here.",
                    modifier = Modifier.padding(top = MaterialTheme.spacing.extraLarge),
                )
            }
            return@LazyColumn
        }

        item {
            HistorySummaryCard(
                uiState = uiState,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screenPadding),
            )
        }

        if (uiState.goalFilters.size > 1) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.screenPadding),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                    modifier = Modifier.padding(top = MaterialTheme.spacing.medium),
                ) {
                    item {
                        FilterChip(
                            selected = uiState.selectedGoalId == null,
                            onClick = { viewModel.selectGoalFilter(null) },
                            label = { Text("All goals") },
                        )
                    }
                    items(uiState.goalFilters, key = { filter -> filter.goalId }) { filter ->
                        FilterChip(
                            selected = uiState.selectedGoalId == filter.goalId,
                            onClick = { viewModel.selectGoalFilter(filter.goalId) },
                            label = { Text(filter.label) },
                        )
                    }
                }
            }
        }

        uiState.months.forEach { section ->
            item(key = "month-${section.summary.month}") {
                MonthHeader(section = section)
            }
            items(section.entries, key = { entry -> entry.transaction.id }) { entry ->
                Column(
                    modifier = Modifier
                        .clickable { openTransactionId = entry.transaction.id }
                        .padding(horizontal = MaterialTheme.spacing.screenPadding),
                ) {
                    TransactionRow(transaction = entry.transaction, goalName = "${entry.goalEmoji} ${entry.goalName}")
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }

    // Details sheet for the tapped entry.
    val openEntry: TransactionWithGoal? = uiState.months
        .flatMap { section -> section.entries }
        .firstOrNull { entry -> entry.transaction.id == openTransactionId }
    if (openEntry != null) {
        TransactionDetailsSheet(
            transaction = openEntry.transaction,
            goalLabel = "${openEntry.goalEmoji} ${openEntry.goalName}",
            onEdit = {
                openTransactionId = null
                onEditTransaction(openEntry.transaction.id)
            },
            onDelete = {
                openTransactionId = null
                viewModel.delete(openEntry.transaction.id) { deleted ->
                    snackbar.showUndo("${MoneyFormatter.format(deleted.transaction.amountPaise)} entry deleted") {
                        viewModel.restore(deleted)
                    }
                }
            },
            onDismiss = { openTransactionId = null },
        )
    }
}

@Composable
private fun HistorySummaryCard(uiState: HistoryUiState, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            StatItem(
                label = "Saved this month",
                value = MoneyFormatter.format(uiState.thisMonthPaise),
                valueColor = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(MaterialTheme.spacing.medium))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem(label = "With boxes", value = MoneyFormatter.format(uiState.byType.boxesPaise), modifier = Modifier.weight(1f))
                StatItem(label = "Added by hand", value = MoneyFormatter.format(uiState.byType.manualPaise), modifier = Modifier.weight(1f))
                StatItem(label = "Total", value = MoneyFormatter.format(uiState.byType.totalPaise), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MonthHeader(section: MonthSection) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.screenPadding)
            .padding(top = MaterialTheme.spacing.large, bottom = MaterialTheme.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = section.summary.month.format(monthTitleFormat),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${MoneyFormatter.format(section.summary.totalPaise)} · ${section.summary.entryCount} " +
                if (section.summary.entryCount == 1) "entry" else "entries",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
