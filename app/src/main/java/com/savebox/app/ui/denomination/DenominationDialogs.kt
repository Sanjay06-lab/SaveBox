package com.savebox.app.ui.denomination

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.savebox.app.domain.calculations.SavingsCalculator
import com.savebox.app.domain.validation.SavingsValidator
import com.savebox.app.ui.components.AmountTextField
import com.savebox.app.ui.theme.spacing
import com.savebox.app.utils.MoneyFormatter

/** Common note values in rupees, shown as quick-pick chips. Edit freely. */
private val COMMON_DENOMINATIONS_RUPEES = listOf(10L, 20L, 50L, 100L, 200L, 500L, 2_000L)

/**
 * Dialog to add a row of boxes, e.g. ₹500 × 110.
 *
 * @param uncoveredPaise how much of the goal the grid doesn't cover yet; used to suggest a quantity.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddRowDialog(
    state: DenominationDialog.AddRow,
    uncoveredPaise: Long,
    onValueChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val valuePaise = MoneyFormatter.parseToPaise(state.valueText)?.takeIf { paise -> paise > 0 }
    val quantity = state.quantityText.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add boxes") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            ) {
                AmountTextField(
                    value = state.valueText,
                    onValueChange = onValueChange,
                    label = "Value of one box",
                    errorMessage = state.errors.valueError,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                    COMMON_DENOMINATIONS_RUPEES.forEach { rupees ->
                        FilterChip(
                            selected = state.valueText == rupees.toString(),
                            onClick = { onValueChange(rupees.toString()) },
                            label = { Text(MoneyFormatter.format(MoneyFormatter.rupeesToPaise(rupees))) },
                        )
                    }
                }

                OutlinedTextField(
                    value = state.quantityText,
                    onValueChange = onQuantityChange,
                    label = { Text("Number of boxes") },
                    singleLine = true,
                    isError = state.errors.quantityError != null,
                    supportingText = state.errors.quantityError?.let { error -> { Text(error) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                // Live preview: "= ₹55,000 in total"
                if (valuePaise != null && quantity != null && quantity > 0) {
                    Text(
                        text = "= ${MoneyFormatter.format(valuePaise * quantity)} in total",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                // Helpful suggestion based on the goal's target (never a hardcoded plan).
                if (valuePaise != null && uncoveredPaise > 0) {
                    val suggested = SavingsCalculator.boxesToCover(uncoveredPaise, valuePaise)
                        .coerceAtMost(SavingsValidator.MAX_BOX_QUANTITY.toLong())
                    TextButton(onClick = { onQuantityChange(suggested.toString()) }) {
                        Text("Use $suggested boxes to cover the remaining ${MoneyFormatter.format(uncoveredPaise)}")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Dialog to change how many boxes a row has. */
@Composable
fun EditQuantityDialog(
    state: DenominationDialog.EditQuantity,
    onQuantityChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${MoneyFormatter.format(state.row.valuePaise)} boxes") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)) {
                Text(
                    text = "${state.row.markedCount} of ${state.row.quantity} boxes are ticked.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = state.quantityText,
                    onValueChange = onQuantityChange,
                    label = { Text("Number of boxes") },
                    singleLine = true,
                    isError = state.error != null,
                    supportingText = state.error?.let { error -> { Text(error) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
