package com.savebox.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.savebox.app.domain.calculations.SavingsCalculator
import com.savebox.app.domain.model.SavingsGoal
import com.savebox.app.ui.theme.SaveBoxTheme
import com.savebox.app.ui.theme.spacing
import com.savebox.app.utils.DateFormatter
import com.savebox.app.utils.MoneyFormatter
import java.time.LocalDate

/**
 * A tappable card that summarises one goal: emoji, name, saved/target, progress bar.
 */
@Composable
fun GoalCard(
    goal: SavingsGoal,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
) {
    val percent = SavingsCalculator.progressPercent(goal)
    val isCompleted = SavingsCalculator.isCompleted(goal)
    val daysLeft = SavingsCalculator.daysUntilDeadline(goal, today)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EmojiBadge(emoji = goal.emoji)
                Spacer(Modifier.width(MaterialTheme.spacing.medium))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${MoneyFormatter.format(goal.savedAmountPaise)} of ${MoneyFormatter.format(goal.targetAmountPaise)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(MaterialTheme.spacing.small))
                Text(
                    text = if (isCompleted) "🎉 $percent%" else "$percent%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(MaterialTheme.spacing.medium))
            SavingsProgressBar(progress = SavingsCalculator.progressFraction(goal))

            // Small footer: remaining amount and deadline.
            Spacer(Modifier.height(MaterialTheme.spacing.small))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (isCompleted) "Goal reached" else "${MoneyFormatter.format(SavingsCalculator.remainingAmount(goal))} to go",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (daysLeft != null && !isCompleted) {
                    Text(
                        text = DateFormatter.describeDaysLeft(daysLeft),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (daysLeft < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun GoalCardPreview() {
    SaveBoxTheme {
        GoalCard(
            goal = SavingsGoal(
                id = 1,
                name = "PlayStation 5",
                emoji = "🎮",
                description = "",
                targetAmountPaise = 7_000_000,
                deadline = LocalDate.now().plusDays(90),
                createdAtMillis = 0,
                savedAmountPaise = 2_150_000,
                lastActivityAtMillis = 0,
            ),
            onClick = {},
        )
    }
}
