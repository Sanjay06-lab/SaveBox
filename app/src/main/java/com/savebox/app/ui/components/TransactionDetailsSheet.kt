package com.savebox.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.savebox.app.domain.model.SavingsTransaction
import com.savebox.app.domain.model.TransactionType
import com.savebox.app.ui.theme.spacing
import com.savebox.app.utils.DateFormatter
import com.savebox.app.utils.MoneyFormatter

/**
 * Bottom sheet with every detail of one savings record, plus Edit and Delete buttons.
 * Used by the History screen and the Goal Details screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsSheet(
    transaction: SavingsTransaction,
    goalLabel: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.spacing.screenPadding)
                .padding(bottom = MaterialTheme.spacing.large),
        ) {
            Text(
                text = "+" + MoneyFormatter.format(transaction.amountPaise),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(goalLabel, style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(MaterialTheme.spacing.medium))
            DetailLine(label = "Type", value = transaction.type.label)
            DetailLine(label = "Saved on", value = DateFormatter.formatDateTime(transaction.savedAtMillis))
            if (transaction.source.isNotBlank()) {
                DetailLine(label = "Source", value = transaction.source)
            }
            if (transaction.note.isNotBlank()) {
                DetailLine(label = "Note", value = transaction.note)
            }
            DetailLine(label = "Recorded", value = DateFormatter.formatDateTime(transaction.createdAtMillis))

            if (transaction.type == TransactionType.DENOMINATION) {
                Spacer(Modifier.height(MaterialTheme.spacing.small))
                Text(
                    text = "This saving came from a denomination box. Deleting it un-ticks the box.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(MaterialTheme.spacing.large))
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.padding(end = ButtonDefaults.IconSpacing))
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.padding(end = ButtonDefaults.IconSpacing))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = MaterialTheme.spacing.extraSmall)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.65f))
    }
}
