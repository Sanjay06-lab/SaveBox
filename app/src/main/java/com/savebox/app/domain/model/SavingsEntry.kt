package com.savebox.app.domain.model

/**
 * What the user fills in on the "Add savings" form.
 * Used both to create a new entry and to edit an existing one.
 */
data class SavingsEntry(
    val goalId: Long,
    /** Amount in paise. Must be positive. */
    val amountPaise: Long,
    val note: String,
    val source: String,
    val savedAtMillis: Long,
)

/** A transaction plus the name and emoji of its goal, for the History screen. */
data class TransactionWithGoal(
    val transaction: SavingsTransaction,
    val goalName: String,
    val goalEmoji: String,
)

/**
 * Everything needed to UNDO a deletion: the deleted transaction and, if it came
 * from a ticked box, which box it was (so the box can be ticked again).
 */
data class DeletedTransaction(
    val transaction: SavingsTransaction,
    val boxLink: BoxLink?,
)

/** Identifies one box in a denomination grid. */
data class BoxLink(
    val denominationId: Long,
    val boxIndex: Int,
)
