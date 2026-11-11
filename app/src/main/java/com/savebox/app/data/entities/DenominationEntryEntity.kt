package com.savebox.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row in the "denomination_entries" table = ONE ticked box in the grid.
 *
 * Each ticked box is linked to the savings transaction it created. This gives
 * three safety guarantees, enforced by the database itself:
 *
 *  1. Unique (denomination_id, box_index): the same box can't be ticked twice.
 *  2. Unique (transaction_id): one transaction belongs to at most one box.
 *  3. CASCADE on transaction_id: if the user deletes that transaction from the
 *     history, the box automatically becomes un-ticked. Totals and grid always agree.
 */
@Entity(
    tableName = "denomination_entries",
    foreignKeys = [
        ForeignKey(
            entity = DenominationEntity::class,
            parentColumns = ["id"],
            childColumns = ["denomination_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SavingsTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["denomination_id", "box_index"], unique = true),
        Index(value = ["transaction_id"], unique = true),
    ],
)
data class DenominationEntryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "denomination_id")
    val denominationId: Long,

    /** Which box in the row: 0 for the first box, 1 for the second, ... */
    @ColumnInfo(name = "box_index")
    val boxIndex: Int,

    @ColumnInfo(name = "transaction_id")
    val transactionId: Long,

    @ColumnInfo(name = "marked_at")
    val markedAtMillis: Long,
)
