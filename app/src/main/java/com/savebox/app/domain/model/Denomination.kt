package com.savebox.app.domain.model

/**
 * One row of boxes in a goal's denomination grid, e.g. "₹500 × 110".
 *
 * @param markedBoxIndices which boxes are ticked (0 = first box). A Set, so a box
 *                         can never be counted twice.
 */
data class Denomination(
    val id: Long,
    val goalId: Long,
    /** Value of ONE box, in paise. */
    val valuePaise: Long,
    /** Number of boxes in this row. */
    val quantity: Int,
    val markedBoxIndices: Set<Int>,
) {
    val markedCount: Int get() = markedBoxIndices.size

    fun isMarked(boxIndex: Int): Boolean = boxIndex in markedBoxIndices
}

/** Result of adding a new denomination row. */
sealed interface AddDenominationResult {
    data class Added(val denominationId: Long) : AddDenominationResult

    /** The goal already has a row with this value. */
    data object DuplicateValue : AddDenominationResult
}

/** Result of tapping a box. */
sealed interface BoxToggleResult {
    /** The box was empty and is now ticked. A transaction was created. */
    data class Marked(val transactionId: Long) : BoxToggleResult

    /** The box was ticked and is now empty. Its transaction was removed (and can be restored). */
    data class Unmarked(val deleted: DeletedTransaction) : BoxToggleResult

    /** The row or box doesn't exist (for example, it was just deleted). */
    data object NotFound : BoxToggleResult
}
