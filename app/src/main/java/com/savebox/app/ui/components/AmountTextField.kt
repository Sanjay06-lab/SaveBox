package com.savebox.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.savebox.app.utils.AppCurrency

/**
 * Text field for money. Shows a ₹ prefix, opens the number keyboard and ignores
 * anything that isn't a digit or a single decimal point.
 *
 * The value stays as TEXT while the user types; convert it with
 * MoneyFormatter.parseToPaise(...) when saving.
 */
@Composable
fun AmountTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    supportingText: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        enabled = enabled,
        onValueChange = { newText ->
            if (isAllowedAmountInput(newText)) onValueChange(newText)
        },
        label = { Text(label) },
        prefix = { Text(AppCurrency.current.symbol.trim()) },
        singleLine = true,
        isError = errorMessage != null,
        supportingText = when {
            errorMessage != null -> ({ Text(errorMessage) })
            supportingText != null -> ({ Text(supportingText) })
            else -> null
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Allows "", "12", "12.", "12.5", "12.50" - at most 2 decimals and 10 digits before the point. */
private fun isAllowedAmountInput(text: String): Boolean {
    return text.matches(Regex("""^\d{0,10}(\.\d{0,2})?$"""))
}
