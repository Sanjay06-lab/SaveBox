package com.savebox.app.domain.validation

import com.savebox.app.utils.MoneyFormatter
import java.time.LocalDate

/**
 * Rules for the "Add savings" form and the "Add denomination" dialog.
 * Pure Kotlin, so every rule is covered by unit tests.
 */
object SavingsValidator {

    const val MAX_NOTE_LENGTH = 120
    const val MAX_SOURCE_LENGTH = 40

    /** One box can't be worth more than ₹1,00,000. */
    val MAX_BOX_VALUE_PAISE: Long = MoneyFormatter.rupeesToPaise(1_00_000)

    /** Up to 1,000 boxes per row keeps the grid usable. */
    const val MAX_BOX_QUANTITY = 1_000

    fun validateEntry(
        goalId: Long?,
        amountText: String,
        note: String,
        source: String,
        savedDate: LocalDate?,
        today: LocalDate,
    ): SavingsFormErrors {
        return SavingsFormErrors(
            goalError = if (goalId == null) "Choose a goal" else null,
            amountError = validateAmount(amountText),
            noteError = if (note.trim().length > MAX_NOTE_LENGTH) "Keep the note under $MAX_NOTE_LENGTH characters" else null,
            sourceError = if (source.trim().length > MAX_SOURCE_LENGTH) "Keep the source under $MAX_SOURCE_LENGTH characters" else null,
            dateError = when {
                savedDate == null -> "Choose a date"
                savedDate.isAfter(today) -> "The date can't be in the future"
                else -> null
            },
        )
    }

    fun validateAmount(text: String): String? {
        if (text.isBlank()) return "Enter the amount you saved"
        val paise = MoneyFormatter.parseToPaise(text) ?: return "Enter a valid amount, e.g. 250"
        return when {
            paise <= 0 -> "The amount must be more than 0"
            paise > GoalValidator.MAX_AMOUNT_PAISE -> "The amount is too large"
            else -> null
        }
    }

    fun validateDenomination(valueText: String, quantityText: String): DenominationFormErrors {
        val valueError = when (val paise = MoneyFormatter.parseToPaise(valueText)) {
            null -> if (valueText.isBlank()) "Enter the value of one box" else "Enter a valid value, e.g. 500"
            else -> when {
                paise <= 0 -> "The value must be more than 0"
                paise > MAX_BOX_VALUE_PAISE -> "One box can be at most ${MoneyFormatter.format(MAX_BOX_VALUE_PAISE)}"
                else -> null
            }
        }
        return DenominationFormErrors(
            valueError = valueError,
            quantityError = validateQuantity(quantityText),
        )
    }

    fun validateQuantity(text: String): String? {
        val quantity = text.trim().toIntOrNull()
        return when {
            text.isBlank() -> "Enter how many boxes"
            quantity == null -> "Enter a whole number"
            quantity < 1 -> "You need at least 1 box"
            quantity > MAX_BOX_QUANTITY -> "At most $MAX_BOX_QUANTITY boxes per row"
            else -> null
        }
    }
}

data class SavingsFormErrors(
    val goalError: String? = null,
    val amountError: String? = null,
    val noteError: String? = null,
    val sourceError: String? = null,
    val dateError: String? = null,
) {
    val hasErrors: Boolean
        get() = listOf(goalError, amountError, noteError, sourceError, dateError).any { error -> error != null }
}

data class DenominationFormErrors(
    val valueError: String? = null,
    val quantityError: String? = null,
) {
    val hasErrors: Boolean get() = valueError != null || quantityError != null
}
