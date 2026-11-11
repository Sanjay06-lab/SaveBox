package com.savebox.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.savebox.app.data.entities.GoalWithTotals
import com.savebox.app.data.entities.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

/**
 * The SELECT used by every "goal + saved total" query.
 *
 * - LEFT JOIN keeps goals that have no transactions yet (their SUM is NULL -> COALESCE makes it 0).
 * - last_activity_at = the newer of "goal edited" and "latest transaction added".
 *   (In SQLite, MAX(a, b) with two values returns the larger one.)
 */
private const val GOAL_WITH_TOTALS_SELECT = """
    SELECT g.*,
           COALESCE(SUM(t.amount_paise), 0) AS saved_amount_paise,
           MAX(g.updated_at, COALESCE(MAX(t.created_at), 0)) AS last_activity_at
    FROM savings_goals AS g
    LEFT JOIN savings_transactions AS t ON t.goal_id = g.id
"""

/**
 * DAO = "Data Access Object": the SQL for the savings_goals table.
 * Room writes the actual code for these functions at build time (via KSP).
 *
 * - Functions returning Flow re-run automatically whenever the tables they read change.
 * - "suspend" functions run once, off the main thread.
 */
@Dao
interface GoalDao {

    @Query("$GOAL_WITH_TOTALS_SELECT GROUP BY g.id ORDER BY g.created_at DESC, g.id DESC")
    fun observeGoalsWithTotals(): Flow<List<GoalWithTotals>>

    @Query("$GOAL_WITH_TOTALS_SELECT WHERE g.id = :goalId GROUP BY g.id")
    fun observeGoalWithTotals(goalId: Long): Flow<GoalWithTotals?>

    @Query("$GOAL_WITH_TOTALS_SELECT WHERE g.id = :goalId GROUP BY g.id")
    suspend fun getGoalWithTotals(goalId: Long): GoalWithTotals?

    @Query("SELECT EXISTS(SELECT 1 FROM savings_goals WHERE id = :goalId)")
    suspend fun exists(goalId: Long): Boolean

    /** Returns the new goal's id. */
    @Insert
    suspend fun insert(goal: SavingsGoalEntity): Long

    /**
     * Updates only the editable details, so created_at can never be overwritten by mistake.
     * Returns the number of rows changed (0 if the goal doesn't exist).
     */
    @Query(
        """
        UPDATE savings_goals
        SET name = :name,
            emoji = :emoji,
            description = :description,
            target_amount_paise = :targetAmountPaise,
            deadline_epoch_day = :deadlineEpochDay,
            updated_at = :updatedAtMillis
        WHERE id = :goalId
        """,
    )
    suspend fun updateDetails(
        goalId: Long,
        name: String,
        emoji: String,
        description: String,
        targetAmountPaise: Long,
        deadlineEpochDay: Long?,
        updatedAtMillis: Long,
    ): Int

    /**
     * Deletes the goal. Thanks to the CASCADE foreign keys, its transactions,
     * denominations and ticked boxes are deleted by the database at the same time.
     */
    @Query("DELETE FROM savings_goals WHERE id = :goalId")
    suspend fun deleteById(goalId: Long): Int
}
