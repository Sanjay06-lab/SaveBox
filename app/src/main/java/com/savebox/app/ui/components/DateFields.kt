package com.savebox.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.savebox.app.ui.theme.spacing
import com.savebox.app.utils.DateFormatter
import java.time.LocalDate

/**
 * A tappable field that shows a date and opens a calendar when tapped.
 *
 * @param onClear if not null, an ✕ button lets the user remove the date (optional dates).
 */
@Composable
fun DateSelectorField(
    label: String,
    date: LocalDate?,
    emptyText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    onClear: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedCard(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(
                width = 1.dp,
                color = if (errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
            ),
        ) {
            Row(
                modifier = Modifier.padding(start = MaterialTheme.spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(MaterialTheme.spacing.medium))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = MaterialTheme.spacing.medium),
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (date == null) emptyText else DateFormatter.formatDate(date),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (date != null && onClear != null) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove date")
                    }
                }
            }
        }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = MaterialTheme.spacing.medium, top = MaterialTheme.spacing.extraSmall),
            )
        }
    }
}

/**
 * Material calendar dialog.
 *
 * @param earliestDate days before this can't be chosen (null = no limit).
 * @param latestDate days after this can't be chosen (null = no limit).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveBoxDatePickerDialog(
    initialDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    earliestDate: LocalDate? = null,
    latestDate: LocalDate? = null,
) {
    val earliestMillis = earliestDate?.let(DateFormatter::localDateToPickerMillis)
    val latestMillis = latestDate?.let(DateFormatter::localDateToPickerMillis)

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.let(DateFormatter::localDateToPickerMillis),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val afterEarliest = earliestMillis == null || utcTimeMillis >= earliestMillis
                val beforeLatest = latestMillis == null || utcTimeMillis <= latestMillis
                return afterEarliest && beforeLatest
            }

            override fun isSelectableYear(year: Int): Boolean {
                val afterEarliest = earliestDate == null || year >= earliestDate.year
                val beforeLatest = latestDate == null || year <= latestDate.year
                return afterEarliest && beforeLatest
            }
        },
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        onDateSelected(DateFormatter.pickerMillisToLocalDate(selectedMillis))
                    } else {
                        onDismiss()
                    }
                },
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    ) {
        DatePicker(state = datePickerState, modifier = Modifier.widthIn(max = 420.dp))
    }
}
