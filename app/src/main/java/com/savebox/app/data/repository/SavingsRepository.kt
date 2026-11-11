package com.savebox.app.data.repository

import com.savebox.app.domain.model.AddDenominationResult
import com.savebox.app.domain.model.BoxToggleResult
import com.savebox.app.domain.model.DeletedTransaction
import com.savebox.app.domain.model.Denomination
import com.savebox.app.domain.model.GoalDraft
import com.savebox.app.domain.model.SavingsEntry
import com.savebox.app.domain.model.SavingsGoal
import com.savebox.app.domain.model.SavingsTransaction
import com.savebox.app.domain.model.TransactionWithGoal
import kotlinx.coroutines.flow.Flow

/**
 * Everything the app can do with goals and savings - WITHOUT saying how data is stored.
 *
 * ViewModels only talk to this interface; RoomSavingsRepository is the real implementation.
 * This keeps the UI independent of the database, and makes a future cloud-sync version
 * possible without touching any screen.
 *
 * Functions returning Flow keep sending fresh data whenever something changes,
 * so screens update automatically. "suspend" functions do one job and finish.
 */
interface SavingsRepository {

    // ----- Goals -----

    /** All goals, newest first. */
    fun observeGoals(): Flow<List<SavingsGoal>>

    /** One goal, or null if it does not exist (for example, it was deleted). */
    fun observeGoal(goalId: Long): Flow<SavingsGoal?>

    /** Load a goal once (used to fill the edit form). */
    suspend fun getGoal(goalId: Long): SavingsGoal?

    /**
     * Create a goal. If [startingAmountPaise] is above 0, a "Starting amount"
     * transaction is recorded too. Returns the new goal's id.
     */
    suspend fun createGoal(draft: GoalDraft, startingAmountPaise: Long): Long

    /** Change a goal's details. Its savings are not touched. */
    suspend fun updateGoal(goalId: Long, draft: GoalDraft)

    /** Delete a goal AND all of its savings records and boxes. */
    suspend fun deleteGoal(goalId: Long)

    // ----- Savings records (transactions) -----

    /** Savings records of one goal, newest first. */
    fun observeTransactionsForGoal(goalId: Long): Flow<List<SavingsTransaction>>

    /** Every savings record with its goal's name, newest first (History screen). */
    fun observeAllTransactions(): Flow<List<TransactionWithGoal>>

    suspend fun getTransaction(transactionId: Long): SavingsTransaction?

    /** When the latest saving was recorded (any goal), or null if none. Used by reminders. */
    suspend fun getLatestSavingCreatedAt(): Long?

    /** Record a custom amount. Returns the new transaction's id. */
    suspend fun addSavings(entry: SavingsEntry): Long

    /**
     * Edit a savings record. For a record created by a denomination box, only the note,
     * source and date change - the amount and goal stay tied to the box.
     * Returns false if the record no longer exists.
     */
    suspend fun updateSavings(transactionId: Long, entry: SavingsEntry): Boolean

    /**
     * Delete a savings record. If it came from a box, the box is un-ticked.
     * Returns what is needed to undo the deletion, or null if it didn't exist.
     */
    suspend fun deleteTransaction(transactionId: Long): DeletedTransaction?

    /**
     * Undo a deletion: puts the record back (with its original id and dates) and,
     * if it came from a box that is still free, ticks that box again.
     * Returns false if its goal was deleted in the meantime.
     */
    suspend fun restoreTransaction(deleted: DeletedTransaction): Boolean

    // ----- Denomination grid -----

    /** The goal's rows of boxes, largest value first, with ticked boxes. */
    fun observeDenominations(goalId: Long): Flow<List<Denomination>>

    suspend fun addDenomination(goalId: Long, valuePaise: Long, quantity: Int): AddDenominationResult

    /**
     * Change how many boxes a row has. Refused (returns false) if it would remove
     * a box that is already ticked.
     */
    suspend fun updateDenominationQuantity(denominationId: Long, quantity: Int): Boolean

    /** Delete a row of boxes AND the savings recorded by its ticked boxes. */
    suspend fun deleteDenomination(denominationId: Long)

    /** Tick an empty box (records a saving) or un-tick a ticked one (removes it). */
    suspend fun toggleBox(denominationId: Long, boxIndex: Int): BoxToggleResult
}
