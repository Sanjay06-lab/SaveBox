package com.savebox.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.savebox.app.data.entities.SavingsTransactionEntity
import com.savebox.app.data.entities.TransactionWithGoalRow
import kotlinx.coroutines.flow.Flow

/** SQL for the savings_transactions table. */
@Dao
interface TransactionDao {

    /** Newest first. "id DESC" breaks ties when two entries have the same date. */
    @Query("SELECT * FROM savings_transactions WHERE goal_id = :goalId ORDER BY saved_at DESC, id DESC")
    fun observeForGoal(goalId: Long): Flow<List<SavingsTransactionEntity>>

    /** Every transaction of every goal, with the goal's name and emoji. */
    @Query(
        """
        SELECT t.*, g.name AS goal_name, g.emoji AS goal_emoji
        FROM savings_transactions AS t
        INNER JOIN savings_goals AS g ON g.id = t.goal_id
        ORDER BY t.saved_at DESC, t.id DESC
        """,
    )
    fun observeAllWithGoal(): Flow<List<TransactionWithGoalRow>>

    @Query("SELECT * FROM savings_transactions WHERE id = :transactionId")
    suspend fun getById(transactionId: Long): SavingsTransactionEntity?

    /**
     * Inserts a transaction and returns its id.
     * If the entity's id is 0 the database picks a new id; otherwise that exact id is used
     * (this is how "Undo delete" puts a transaction back).
     */
    @Insert
    suspend fun insert(transaction: SavingsTransactionEntity): Long

    @Update
    suspend fun update(transaction: SavingsTransactionEntity): Int

    /** Deleting a box's transaction also un-ticks the box (CASCADE). */
    @Query("DELETE FROM savings_transactions WHERE id = :transactionId")
    suspend fun deleteById(transactionId: Long): Int

    @Query("DELETE FROM savings_transactions WHERE id IN (:transactionIds)")
    suspend fun deleteByIds(transactionIds: List<Long>): Int

    /** When the most recent saving was recorded, or null if there are none. */
    @Query("SELECT MAX(created_at) FROM savings_transactions")
    suspend fun latestCreatedAt(): Long?

    @Query("SELECT COUNT(*) FROM savings_transactions WHERE goal_id = :goalId")
    suspend fun countForGoal(goalId: Long): Int
}
