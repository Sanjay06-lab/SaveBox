package com.savebox.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row in the "savings_transactions" table: a single amount saved for a goal.
 *
 * FOREIGN KEY: goal_id must point to an existing goal. With onDelete = CASCADE,
 * deleting a goal automatically deletes its transactions - so we can never end up
 * with savings that belong to no goal.
 */
@Entity(
    tableName = "savings_transactions",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    // An index makes "all transactions of goal X" fast.
    indices = [Index(value = ["goal_id"])],
)
data class SavingsTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "goal_id")
    val goalId: Long,

    /** Amount in paise. Always positive. */
    @ColumnInfo(name = "amount_paise")
    val amountPaise: Long,

    /**
     * The TransactionType name, e.g. "CUSTOM". Stored as text so the database stays
     * readable and adding new types later needs no migration.
     */
    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "note")
    val note: String,

    /** Optional source/category, e.g. "Pocket money" (used from Phase 4). */
    @ColumnInfo(name = "source")
    val source: String,

    /** The date the user says the money was saved (can be in the past). */
    @ColumnInfo(name = "saved_at")
    val savedAtMillis: Long,

    /** When the record was created in the app (used for "Recently updated"). */
    @ColumnInfo(name = "created_at")
    val createdAtMillis: Long,
)
