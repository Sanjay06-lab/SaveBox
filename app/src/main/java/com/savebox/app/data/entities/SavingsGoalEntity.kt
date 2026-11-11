package com.savebox.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row in the "savings_goals" table.
 *
 * Entities describe how data is STORED. The UI never sees them: the repository converts
 * them to domain models (see data/repository/EntityMappers.kt).
 *
 * Note what is NOT stored here: the saved amount. It is always calculated from the
 * goal's transactions, so it can never disagree with the savings history.
 *
 * IMPORTANT: changing this class (adding/removing/renaming a field) changes the database.
 * You must then increase the version in SaveBoxDatabase and add a migration.
 */
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0, // 0 = "let the database choose a new id"

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "emoji")
    val emoji: String,

    @ColumnInfo(name = "description")
    val description: String,

    /** Target in paise (₹1 = 100). */
    @ColumnInfo(name = "target_amount_paise")
    val targetAmountPaise: Long,

    /** Deadline stored as "days since 1 Jan 1970" (LocalDate.toEpochDay()), or null. */
    @ColumnInfo(name = "deadline_epoch_day")
    val deadlineEpochDay: Long?,

    @ColumnInfo(name = "created_at")
    val createdAtMillis: Long,

    /** Last time the goal's details were edited. */
    @ColumnInfo(name = "updated_at")
    val updatedAtMillis: Long,
)
