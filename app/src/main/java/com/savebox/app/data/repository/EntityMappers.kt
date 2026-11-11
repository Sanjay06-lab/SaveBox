package com.savebox.app.data.repository

import com.savebox.app.data.entities.DenominationEntity
import com.savebox.app.data.entities.DenominationEntryEntity
import com.savebox.app.data.entities.GoalWithTotals
import com.savebox.app.data.entities.SavingsTransactionEntity
import com.savebox.app.data.entities.TransactionWithGoalRow
import com.savebox.app.domain.model.Denomination
import com.savebox.app.domain.model.SavingsGoal
import com.savebox.app.domain.model.SavingsTransaction
import com.savebox.app.domain.model.TransactionType
import com.savebox.app.domain.model.TransactionWithGoal
import java.time.LocalDate

/*
 * Converters between database rows (entities) and the models the app uses (domain).
 * Keeping these in one file means a database change only needs edits here,
 * not in every screen.
 */

fun GoalWithTotals.toDomain(): SavingsGoal {
    return SavingsGoal(
        id = goal.id,
        name = goal.name,
        emoji = goal.emoji,
        description = goal.description,
        targetAmountPaise = goal.targetAmountPaise,
        deadline = goal.deadlineEpochDay?.let { epochDay -> LocalDate.ofEpochDay(epochDay) },
        createdAtMillis = goal.createdAtMillis,
        savedAmountPaise = savedAmountPaise,
        lastActivityAtMillis = lastActivityAtMillis,
    )
}

fun SavingsTransactionEntity.toDomain(): SavingsTransaction {
    return SavingsTransaction(
        id = id,
        goalId = goalId,
        amountPaise = amountPaise,
        type = transactionTypeFromName(type),
        note = note,
        source = source,
        savedAtMillis = savedAtMillis,
        createdAtMillis = createdAtMillis,
    )
}

/** Used when restoring a deleted transaction: keeps the original id. */
fun SavingsTransaction.toEntity(): SavingsTransactionEntity {
    return SavingsTransactionEntity(
        id = id,
        goalId = goalId,
        amountPaise = amountPaise,
        type = type.name,
        note = note,
        source = source,
        savedAtMillis = savedAtMillis,
        createdAtMillis = createdAtMillis,
    )
}

fun TransactionWithGoalRow.toDomain(): TransactionWithGoal {
    return TransactionWithGoal(
        transaction = transaction.toDomain(),
        goalName = goalName,
        goalEmoji = goalEmoji,
    )
}

/** Combines a row with ALL ticked boxes of the goal, keeping only this row's boxes. */
fun DenominationEntity.toDomain(entriesByDenomination: Map<Long, List<DenominationEntryEntity>>): Denomination {
    val markedIndices = entriesByDenomination[id].orEmpty()
        .map { entry -> entry.boxIndex }
        .toSet()
    return Denomination(
        id = id,
        goalId = goalId,
        valuePaise = valuePaise,
        quantity = quantity,
        markedBoxIndices = markedIndices,
    )
}

/**
 * Reads a stored type name safely. If the name is unknown (for example, data restored
 * from a newer app version), we show it as a custom entry instead of crashing.
 */
fun transactionTypeFromName(name: String): TransactionType {
    return TransactionType.entries.firstOrNull { type -> type.name == name } ?: TransactionType.CUSTOM
}
