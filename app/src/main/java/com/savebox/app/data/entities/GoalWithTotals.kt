package com.savebox.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded

/**
 * NOT a table: the result of a query that joins a goal with the sum of its transactions.
 * See GoalDao.GOAL_WITH_TOTALS_SELECT.
 *
 * @Embedded means "the goal's own columns come first, as a SavingsGoalEntity".
 */
data class GoalWithTotals(
    @Embedded
    val goal: SavingsGoalEntity,

    @ColumnInfo(name = "saved_amount_paise")
    val savedAmountPaise: Long,

    @ColumnInfo(name = "last_activity_at")
    val lastActivityAtMillis: Long,
)
