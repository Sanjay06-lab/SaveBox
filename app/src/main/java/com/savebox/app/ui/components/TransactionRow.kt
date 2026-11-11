package com.savebox.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.savebox.app.domain.model.SavingsTransaction
import com.savebox.app.domain.model.TransactionType
import com.savebox.app.ui.theme.spacing
import com.savebox.app.utils.DateFormatter
import com.savebox.app.utils.MoneyFormatter

/**
 * One line in a savings history list: icon, type, date, optional note and amount.
 *
 * @param goalName shown on the History screen (which mixes all goals); null hides it.
 */
@Composable
fun TransactionRow(
    transaction: SavingsTransaction,
    modifier: Modifier = Modifier,
    goalName: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmojiBadge(
            emoji = transaction.type.emoji(),
            size = 40.dp,
            backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(Modifier.width(MaterialTheme.spacing.medium))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = goalName ?: transaction.type.label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val details = buildList {
                if (goalName != null) add(transaction.type.label)
                add(DateFormatter.formatDateTime(transaction.savedAtMillis))
            }.joinToString(" · ")
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (transaction.note.isNotBlank()) {
                Text(
                    text = transaction.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.width(MaterialTheme.spacing.small))
        Text(
            text = "+" + MoneyFormatter.format(transaction.amountPaise),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Small picture for each kind of transaction. */
private fun TransactionType.emoji(): String = when (this) {
    TransactionType.STARTING_AMOUNT -> "🏁"
    TransactionType.CUSTOM -> "💵"
    TransactionType.DENOMINATION -> "🧱"
}
