package com.savebox.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savebox.app.ui.theme.SaveBoxTheme
import com.savebox.app.ui.theme.spacing

/**
 * A friendly "nothing here yet" message with an optional button.
 *
 * Example:
 *   EmptyState(
 *       emoji = "🎯",
 *       title = "No goals yet",
 *       message = "Create your first savings goal to get started.",
 *       actionLabel = "Create goal",
 *       onAction = { ... },
 *   )
 */
@Composable
fun EmptyState(
    emoji: String,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Emoji inside a soft green circle.
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 40.sp)
        }

        Spacer(Modifier.height(MaterialTheme.spacing.large))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(MaterialTheme.spacing.small))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        // Only show the button when both a label and an action were given.
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(MaterialTheme.spacing.large))
            Button(
                onClick = onAction,
                modifier = Modifier.height(MaterialTheme.spacing.minTouchTarget),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    SaveBoxTheme {
        EmptyState(
            emoji = "🎯",
            title = "No goals yet",
            message = "Create your first savings goal to get started.",
            actionLabel = "Create goal",
            onAction = {},
        )
    }
}
