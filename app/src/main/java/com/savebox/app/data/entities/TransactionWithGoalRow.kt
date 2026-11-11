package com.savebox.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded

/**
 * NOT a table: a transaction joined with its goal's name and emoji,
 * so the History screen can show "🎮 PlayStation" next to each amount.
 */
data class TransactionWithGoalRow(
    @Embedded
    val transaction: SavingsTransactionEntity,

    @ColumnInfo(name = "goal_name")
    val goalName: String,

    @ColumnInfo(name = "goal_emoji")
    val goalEmoji: String,
)
