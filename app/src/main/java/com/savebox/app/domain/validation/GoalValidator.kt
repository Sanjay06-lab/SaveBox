package com.savebox.app.domain.validation

import com.savebox.app.utils.MoneyFormatter
import java.time.LocalDate

/**
 * Checks the goal form and returns friendly error messages.
 *
 * It works on the raw text the user typed, so it can be unit tested without any UI.
 * An empty GoalFormErrors (hasErrors == false) means the form is valid.
 */
object GoalValidator {

    const val MAX_NAME_LENGTH = 40
    const val MAX_DESCRIPTION_LENGTH = 200

    /** ₹10 crore - a sensible upper limit that also protects against typing mistakes. */
    val MAX_AMOUNT_PAISE: Long = MoneyFormatter.rupeesToPaise(10_00_00_000)

    fun validate(
        name: String,
        targetAmountText: String,
        startingAmountText: String,
        description: String,
        deadline: LocalDate?,
        today: LocalDate,
        isNewGoal: Boolean,
    ): GoalFormErrors {
        return GoalFormErrors(
            nameError = validateName(name),
            targetAmountError = validateTargetAmount(targetAmountText),
            startingAmountError = if (isNewGoal) validateStartingAmount(startingAmountText) else null,
            descriptionError = validateDescription(description),
            // Old goals may keep a past deadline; new goals must look forward.
            deadlineError = if (isNewGoal) validateDeadline(deadline, today) else null,
        )
    }

    fun validateName(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "Please give your goal a name"
            trimmed.length > MAX_NAME_LENGTH -> "Keep the name under $MAX_NAME_LENGTH characters"
            else -> null
        }
    }

    fun validateTargetAmount(text: String): String? {
        if (text.isBlank()) return "Enter how much you want to save"
        val paise = MoneyFormatter.parseToPaise(text) ?: return "Enter a valid amount, e.g. 70000"
        return when {
            paise <= 0 -> "The target must be more than 0"
            paise > MAX_AMOUNT_PAISE -> "The target is too large"
            else -> null
        }
    }

    /** The starting amount is optional; blank means ₹0. */
    fun validateStartingAmount(text: String): String? {
        if (text.isBlank()) return null
        val paise = MoneyFormatter.parseToPaise(text) ?: return "Enter a valid amount, or leave it empty"
        return when {
            paise < 0 -> "The starting amount can't be negative"
            paise > MAX_AMOUNT_PAISE -> "The starting amount is too large"
            else -> null
        }
    }

    fun validateDescription(description: String): String? {
        return if (description.trim().length > MAX_DESCRIPTION_LENGTH) {
            "Keep the description under $MAX_DESCRIPTION_LENGTH characters"
        } else {
            null
        }
    }

    fun validateDeadline(deadline: LocalDate?, today: LocalDate): String? {
        if (deadline == null) return null
        return if (deadline.isBefore(today)) "The deadline can't be in the past" else null
    }
}

/** One optional error message per form field. null = that field is fine. */
data class GoalFormErrors(
    val nameError: String? = null,
    val targetAmountError: String? = null,
    val startingAmountError: String? = null,
    val descriptionError: String? = null,
    val deadlineError: String? = null,
) {
    val hasErrors: Boolean
        get() = listOf(nameError, targetAmountError, startingAmountError, descriptionError, deadlineError)
            .any { error -> error != null }
}
