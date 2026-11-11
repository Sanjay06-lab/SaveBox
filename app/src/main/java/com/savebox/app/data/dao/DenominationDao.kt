package com.savebox.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.savebox.app.data.entities.DenominationEntity
import com.savebox.app.data.entities.DenominationEntryEntity
import kotlinx.coroutines.flow.Flow

/** SQL for the denomination grid tables (rows of boxes and ticked boxes). */
@Dao
interface DenominationDao {

    // ----- Rows ("₹500 × 110") -----

    @Query("SELECT * FROM denominations WHERE goal_id = :goalId ORDER BY value_paise DESC")
    fun observeDenominationsForGoal(goalId: Long): Flow<List<DenominationEntity>>

    @Query("SELECT * FROM denominations WHERE id = :denominationId")
    suspend fun getDenomination(denominationId: Long): DenominationEntity?

    @Query("SELECT * FROM denominations WHERE goal_id = :goalId AND value_paise = :valuePaise")
    suspend fun findByValue(goalId: Long, valuePaise: Long): DenominationEntity?

    @Insert
    suspend fun insertDenomination(denomination: DenominationEntity): Long

    @Query("UPDATE denominations SET quantity = :quantity WHERE id = :denominationId")
    suspend fun updateQuantity(denominationId: Long, quantity: Int): Int

    /** Ticked boxes of this row are removed by CASCADE. */
    @Query("DELETE FROM denominations WHERE id = :denominationId")
    suspend fun deleteDenomination(denominationId: Long): Int

    @Query("SELECT COUNT(*) FROM denominations WHERE goal_id = :goalId")
    suspend fun countDenominationsForGoal(goalId: Long): Int

    // ----- Ticked boxes -----

    /** All ticked boxes of one goal (every row). */
    @Query(
        """
        SELECT e.* FROM denomination_entries AS e
        INNER JOIN denominations AS d ON d.id = e.denomination_id
        WHERE d.goal_id = :goalId
        """,
    )
    fun observeEntriesForGoal(goalId: Long): Flow<List<DenominationEntryEntity>>

    @Query("SELECT * FROM denomination_entries WHERE denomination_id = :denominationId AND box_index = :boxIndex")
    suspend fun getEntry(denominationId: Long, boxIndex: Int): DenominationEntryEntity?

    @Query("SELECT * FROM denomination_entries WHERE transaction_id = :transactionId")
    suspend fun getEntryForTransaction(transactionId: Long): DenominationEntryEntity?

    @Insert
    suspend fun insertEntry(entry: DenominationEntryEntity): Long

    /** Highest ticked box number in a row, or null if nothing is ticked. */
    @Query("SELECT MAX(box_index) FROM denomination_entries WHERE denomination_id = :denominationId")
    suspend fun maxMarkedBoxIndex(denominationId: Long): Int?

    /** The transactions created by this row's ticked boxes. */
    @Query("SELECT transaction_id FROM denomination_entries WHERE denomination_id = :denominationId")
    suspend fun transactionIdsForDenomination(denominationId: Long): List<Long>

    @Query("SELECT COUNT(*) FROM denomination_entries WHERE denomination_id = :denominationId")
    suspend fun countEntriesForDenomination(denominationId: Long): Int
}
