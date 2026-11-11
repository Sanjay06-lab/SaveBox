package com.savebox.app.ui.dashboard

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.savebox.app.domain.calculations.SavingsCalculator
import com.savebox.app.domain.calculations.SavingsSummary
import com.savebox.app.domain.model.SavingsGoal
import com.savebox.app.ui.AppViewModelProvider
import com.savebox.app.ui.components.EmojiBadge
import com.savebox.app.ui.components.EmptyState
import com.savebox.app.ui.components.GoalCard
import com.savebox.app.ui.components.SavingsProgressBar
import com.savebox.app.ui.components.ScreenHeader
import com.savebox.app.ui.components.SectionHeader
import com.savebox.app.ui.components.StatItem
import com.savebox.app.ui.theme.spacing
import com.savebox.app.utils.AppCurrency
import com.savebox.app.utils.MoneyFormatter

/**
 * Home dashboard: totals, quick actions and the list of goals.
 *
 * This first function connects the screen to its ViewModel.
 * The second one (DashboardContent) only draws, which makes it easy to preview.
 */
@Composable
fun DashboardScreen(
    onCreateGoal: () -> Unit,
    onOpenGoal: (goalId: Long) -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        DashboardContent(
            uiState = uiState,
            onCreateGoal = onCreateGoal,
            onOpenGoal = onOpenGoal,
            onQuickAdd = onQuickAdd,
            contentPadding = innerPadding,
        )
    }
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onCreateGoal: () -> Unit,
    onOpenGoal: (Long) -> Unit,
    onQuickAdd: () -> Unit,
    contentPadding: PaddingValues,
) {
    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + MaterialTheme.spacing.large,
        ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
    ) {
        item {
            ScreenHeader(title = "My savings", subtitle = "Small savings. Bigger possibilities.")
        }

        if (!uiState.hasGoals) {
            item {
                EmptyState(
                    emoji = "🐷",
                    title = "Start your first goal",
                    message = "What are you saving for? A phone, a trip, an emergency fund? " +
                        "Create a goal and every rupee you save will move you closer.",
                    actionLabel = "Create a goal",
                    onAction = onCreateGoal,
                    modifier = Modifier.padding(top = MaterialTheme.spacing.extraLarge),
                )
            }
            return@LazyColumn
        }

        item {
            SummaryCard(
                summary = uiState.summary,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screenPadding),
            )
        }

        item {
            QuickActions(
                onCreateGoal = onCreateGoal,
                onQuickAdd = onQuickAdd,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screenPadding),
            )
        }

        // "Recently updated" only makes sense when there is more than one goal.
        if (uiState.recentlyUpdatedGoals.size > 1) {
            item {
                SectionHeader(
                    title = "Recently updated",
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screenPadding),
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = MaterialTheme.spacing.screenPadding),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                ) {
                    items(uiState.recentlyUpdatedGoals, key = { goal -> goal.id }) { goal ->
                        RecentGoalCard(goal = goal, onClick = { onOpenGoal(goal.id) })
                    }
                }
            }
        }

        if (uiState.activeGoals.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Active goals",
                    trailingText = "${uiState.activeGoals.size}",
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screenPadding),
                )
            }
            items(uiState.activeGoals, key = { goal -> "active-${goal.id}" }) { goal ->
                GoalCard(
                    goal = goal,
                    onClick = { onOpenGoal(goal.id) },
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screenPadding),
                )
            }
        }

        if (uiState.completedGoals.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Completed 🎉",
                    trailingText = "${uiState.completedGoals.size}",
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screenPadding),
                )
            }
            items(uiState.completedGoals, key = { goal -> "done-${goal.id}" }) { goal ->
                GoalCard(
                    goal = goal,
                    onClick = { onOpenGoal(goal.id) },
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.screenPadding),
                )
            }
        }
    }
}

/** Dark navy card with the overall totals. */
@Composable
private fun SummaryCard(summary: SavingsSummary, modifier: Modifier = Modifier) {
    // Dark navy card in both themes: "secondary" in light mode, "secondaryContainer" in dark mode
    // (in dark mode "secondary" is a light blue, which would look too bright here).
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val containerColor = if (isDarkTheme) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.secondary
    val contentColor = if (isDarkTheme) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSecondary
    val mutedColor = contentColor.copy(alpha = 0.7f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.large)) {
            Text("Total saved", style = MaterialTheme.typography.labelLarge, color = mutedColor)
            Text(
                text = MoneyFormatter.format(summary.totalSavedPaise),
                style = MaterialTheme.typography.displaySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(MaterialTheme.spacing.medium))
            SavingsProgressBar(
                progress = summary.overallFraction,
                height = 10.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = contentColor.copy(alpha = 0.15f),
            )
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = "${summary.overallPercent}% of all goals",
                style = MaterialTheme.typography.labelMedium,
                color = mutedColor,
            )

            Spacer(Modifier.height(MaterialTheme.spacing.medium))
            Row(modifier = Modifier.fillMaxWidth()) {
                StatItem(
                    label = "Target",
                    value = MoneyFormatter.format(summary.totalTargetPaise),
                    labelColor = mutedColor,
                    valueColor = contentColor,
                    modifier = Modifier.weight(1f),
                )
                StatItem(
                    label = "Remaining",
                    value = MoneyFormatter.format(summary.totalRemainingPaise),
                    labelColor = mutedColor,
                    valueColor = contentColor,
                    modifier = Modifier.weight(1f),
                )
                StatItem(
                    label = "Goals",
                    value = "${summary.completedGoalCount}/${summary.goalCount} done",
                    labelColor = mutedColor,
                    valueColor = contentColor,
                    modifier = Modifier.weight(0.8f),
                )
            }
        }
    }
}

@Composable
private fun QuickActions(onCreateGoal: () -> Unit, onQuickAdd: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        Button(
            onClick = onCreateGoal,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 52.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            Text("New goal")
        }
        FilledTonalButton(
            onClick = onQuickAdd,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 52.dp),
        ) {
            Text("${AppCurrency.current.symbol.trim()}  Quick add")
        }
    }
}

/** Small square card used in the "Recently updated" row. */
@Composable
private fun RecentGoalCard(goal: SavingsGoal, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(150.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            EmojiBadge(emoji = goal.emoji, size = 40.dp)
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = goal.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${SavingsCalculator.progressPercent(goal)}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            SavingsProgressBar(progress = SavingsCalculator.progressFraction(goal), height = 6.dp)
        }
    }
}
