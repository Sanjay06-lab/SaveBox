package com.savebox.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.savebox.app.data.entities.DenominationEntity
import com.savebox.app.data.entities.DenominationEntryEntity
import com.savebox.app.data.entities.SavingsGoalEntity
import com.savebox.app.data.entities.SavingsTransactionEntity

/** Reads and writes whole tables, for backup and restore only. */
@Dao
interface BackupDao {

    @Query("SELECT * FROM savings_goals ORDER BY id")
    suspend fun allGoals(): List<SavingsGoalEntity>

    @Query("SELECT * FROM savings_transactions ORDER BY id")
    suspend fun allTransactions(): List<SavingsTransactionEntity>

    @Query("SELECT * FROM denominations ORDER BY id")
    suspend fun allDenominations(): List<DenominationEntity>

    @Query("SELECT * FROM denomination_entries ORDER BY id")
    suspend fun allDenominationEntries(): List<DenominationEntryEntity>

    /** Deleting every goal empties every table, thanks to the CASCADE foreign keys. */
    @Query("DELETE FROM savings_goals")
    suspend fun deleteEverything()

    @Insert
    suspend fun insertGoals(goals: List<SavingsGoalEntity>)

    @Insert
    suspend fun insertTransactions(transactions: List<SavingsTransactionEntity>)

    @Insert
    suspend fun insertDenominations(denominations: List<DenominationEntity>)

    @Insert
    suspend fun insertDenominationEntries(entries: List<DenominationEntryEntity>)
}
