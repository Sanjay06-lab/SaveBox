package com.savebox.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row in the "denominations" table: a group of boxes in a goal's grid,
 * e.g. "₹500 × 110 boxes". The boxes themselves are not stored one by one -
 * only the ticked ones are (see DenominationEntryEntity).
 *
 * The unique index stops the same value appearing twice for one goal
 * (two separate "₹500" rows would be confusing).
 */
@Entity(
    tableName = "denominations",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["goal_id", "value_paise"], unique = true)],
)
data class DenominationEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "goal_id")
    val goalId: Long,

    /** Value of ONE box in paise, e.g. ₹500 = 50000. */
    @ColumnInfo(name = "value_paise")
    val valuePaise: Long,

    /** How many boxes of this value, e.g. 110. */
    @ColumnInfo(name = "quantity")
    val quantity: Int,

    @ColumnInfo(name = "created_at")
    val createdAtMillis: Long,
)
