package com.savebox.app.domain.model

/**
 * One savings record, e.g. "₹500 saved on 12 Mar for PlayStation".
 * A goal's saved amount is always the sum of its transactions.
 */
data class SavingsTransaction(
    val id: Long,
    val goalId: Long,
    /** Amount in paise. Always positive. */
    val amountPaise: Long,
    val type: TransactionType,
    val note: String,
    /** Optional source/category, e.g. "Pocket money". Empty if not given. */
    val source: String,
    /** When the money was saved (chosen by the user, defaults to now). */
    val savedAtMillis: Long,
    /** When this record was created in the app. */
    val createdAtMillis: Long,
)

/**
 * How a transaction was created. Stored in the database by its name (e.g. "CUSTOM"),
 * so never rename an existing entry - only add new ones.
 */
enum class TransactionType(val label: String) {
    /** Amount entered when the goal was created. */
    STARTING_AMOUNT("Starting amount"),

    /** Any amount added manually (Phase 4). */
    CUSTOM("Custom savings"),

    /** A box ticked in the denomination grid (Phase 4). */
    DENOMINATION("Denomination box"),
}
