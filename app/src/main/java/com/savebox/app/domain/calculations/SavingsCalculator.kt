package com.savebox.app.domain.calculations

import com.savebox.app.domain.model.Denomination
import com.savebox.app.domain.model.SavingsGoal
import com.savebox.app.domain.model.SavingsTransaction
import com.savebox.app.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * THE ONE PLACE where SaveBox does savings maths.
 *
 * Every screen (dashboard, goal details, grid, history, widget...) calls these functions
 * instead of calculating on its own, so the numbers are always consistent.
 * All amounts are in paise (Long), so there are no rounding errors.
 *
 * Rules:
 *  - Progress (%)      = saved / target × 100   (can go above 100 if the user saved extra)
 *  - Visual progress   = the same, but capped at 100% for progress bars
 *  - Remaining amount  = target − saved, but never below 0
 *  - Extra amount      = saved − target, when the user saved more than the target
 */
object SavingsCalculator {

    // ------------------------------------------------------------------
    // Single goal
    // ------------------------------------------------------------------

    /**
     * Real progress percentage, rounded down. 1 saved of 3 target -> 33.
     * May be above 100 when the user saved more than the target.
     */
    fun progressPercent(savedPaise: Long, targetPaise: Long): Int {
        if (targetPaise <= 0) return 0
        val percent = savedPaise.coerceAtLeast(0) * 100 / targetPaise
        return percent.toInt()
    }

    /**
     * Progress between 0.0 and 1.0 for progress bars and rings.
     * Capped at 1.0 so a bar never draws past its end.
     */
    fun progressFraction(savedPaise: Long, targetPaise: Long): Float {
        if (targetPaise <= 0) return 0f
        val fraction = savedPaise.toDouble() / targetPaise.toDouble()
        return fraction.coerceIn(0.0, 1.0).toFloat()
    }

    /** How much is still needed. Never negative. */
    fun remainingAmount(savedPaise: Long, targetPaise: Long): Long {
        return (targetPaise - savedPaise).coerceAtLeast(0)
    }

    /** How much was saved above the target (0 if the target is not reached yet). */
    fun excessAmount(savedPaise: Long, targetPaise: Long): Long {
        return (savedPaise - targetPaise).coerceAtLeast(0)
    }

    fun isCompleted(savedPaise: Long, targetPaise: Long): Boolean {
        return targetPaise > 0 && savedPaise >= targetPaise
    }

    // Convenience versions that take a whole goal.
    fun progressPercent(goal: SavingsGoal): Int = progressPercent(goal.savedAmountPaise, goal.targetAmountPaise)
    fun progressFraction(goal: SavingsGoal): Float = progressFraction(goal.savedAmountPaise, goal.targetAmountPaise)
    fun remainingAmount(goal: SavingsGoal): Long = remainingAmount(goal.savedAmountPaise, goal.targetAmountPaise)
    fun excessAmount(goal: SavingsGoal): Long = excessAmount(goal.savedAmountPaise, goal.targetAmountPaise)
    fun isCompleted(goal: SavingsGoal): Boolean = isCompleted(goal.savedAmountPaise, goal.targetAmountPaise)

    /**
     * Days from [today] until the goal's deadline.
     * Positive = days left, 0 = due today, negative = overdue, null = no deadline.
     */
    fun daysUntilDeadline(goal: SavingsGoal, today: LocalDate): Long? {
        val deadline = goal.deadline ?: return null
        return ChronoUnit.DAYS.between(today, deadline)
    }

    /** Sum of a list of transactions. Used to check that totals match the history. */
    fun totalOf(transactions: List<SavingsTransaction>): Long {
        return transactions.sumOf { transaction -> transaction.amountPaise }
    }

    // ------------------------------------------------------------------
    // All goals (dashboard)
    // ------------------------------------------------------------------

    /**
     * Totals across all goals, shown on the dashboard.
     *
     * Note on "remaining": we add up each goal's own remaining amount.
     * If one goal has ₹1,000 extra, that extra does NOT reduce what another goal still needs.
     * Overall progress uses the same idea: extra savings count only up to each goal's target.
     */
    fun summarize(goals: List<SavingsGoal>): SavingsSummary {
        var totalSaved = 0L
        var totalTarget = 0L
        var totalRemaining = 0L
        var savedTowardsTargets = 0L
        var completedCount = 0

        for (goal in goals) {
            totalSaved += goal.savedAmountPaise
            totalTarget += goal.targetAmountPaise
            totalRemaining += remainingAmount(goal)
            // Count savings only up to this goal's target.
            savedTowardsTargets += minOf(goal.savedAmountPaise, goal.targetAmountPaise).coerceAtLeast(0)
            if (isCompleted(goal)) completedCount++
        }

        return SavingsSummary(
            totalSavedPaise = totalSaved,
            totalTargetPaise = totalTarget,
            totalRemainingPaise = totalRemaining,
            overallPercent = progressPercent(savedTowardsTargets, totalTarget),
            overallFraction = progressFraction(savedTowardsTargets, totalTarget),
            goalCount = goals.size,
            completedGoalCount = completedCount,
        )
    }

    // ------------------------------------------------------------------
    // Denomination grid
    // ------------------------------------------------------------------

    /** Box counts and amounts for a goal's whole grid. */
    fun summarizeDenominations(denominations: List<Denomination>): DenominationSummary {
        var totalBoxes = 0
        var completedBoxes = 0
        var savedPaise = 0L
        var plannedPaise = 0L

        for (row in denominations) {
            totalBoxes += row.quantity
            completedBoxes += row.markedCount
            savedPaise += row.valuePaise * row.markedCount
            plannedPaise += row.valuePaise * row.quantity
        }

        return DenominationSummary(
            totalBoxes = totalBoxes,
            completedBoxes = completedBoxes,
            savedPaise = savedPaise,
            plannedPaise = plannedPaise,
        )
    }

    /**
     * How many boxes of [boxValuePaise] are needed to cover [amountPaise] (rounded up).
     * 1,000 with ₹300 boxes -> 4.  Returns 0 if nothing needs covering.
     */
    fun boxesToCover(amountPaise: Long, boxValuePaise: Long): Long {
        if (amountPaise <= 0 || boxValuePaise <= 0) return 0
        return (amountPaise + boxValuePaise - 1) / boxValuePaise
    }

    // ------------------------------------------------------------------
    // Transactions (history)
    // ------------------------------------------------------------------

    /**
     * Splits savings by how they were added.
     * "Manual" = custom entries + starting amounts; "boxes" = ticked denomination boxes.
     */
    fun totalsByType(transactions: List<SavingsTransaction>): SavingsByType {
        var boxes = 0L
        var custom = 0L
        var starting = 0L
        for (transaction in transactions) {
            when (transaction.type) {
                TransactionType.DENOMINATION -> boxes += transaction.amountPaise
                TransactionType.CUSTOM -> custom += transaction.amountPaise
                TransactionType.STARTING_AMOUNT -> starting += transaction.amountPaise
            }
        }
        return SavingsByType(boxesPaise = boxes, customPaise = custom, startingPaise = starting)
    }

    /**
     * Total saved per calendar month (by the date the money was saved), newest month first.
     * @param zone the phone's time zone - a saving at 11:30 pm belongs to that local day.
     */
    fun monthlySummary(
        transactions: List<SavingsTransaction>,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<MonthlySavings> {
        return transactions
            .groupBy { transaction -> yearMonthOf(transaction.savedAtMillis, zone) }
            .map { (month, monthTransactions) ->
                MonthlySavings(
                    month = month,
                    totalPaise = monthTransactions.sumOf { transaction -> transaction.amountPaise },
                    entryCount = monthTransactions.size,
                )
            }
            .sortedByDescending { summary -> summary.month }
    }

    /** Which calendar month a timestamp falls in, in the given time zone. */
    fun yearMonthOf(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): YearMonth {
        return YearMonth.from(Instant.ofEpochMilli(epochMillis).atZone(zone))
    }
}

/** Dashboard totals. Money values are in paise. */
data class SavingsSummary(
    val totalSavedPaise: Long,
    val totalTargetPaise: Long,
    val totalRemainingPaise: Long,
    /** 0..100 */
    val overallPercent: Int,
    /** 0.0..1.0 for progress bars */
    val overallFraction: Float,
    val goalCount: Int,
    val completedGoalCount: Int,
) {
    val activeGoalCount: Int get() = goalCount - completedGoalCount

    companion object {
        val EMPTY = SavingsSummary(0, 0, 0, 0, 0f, 0, 0)
    }
}

/** Grid totals for one goal. Money values are in paise. */
data class DenominationSummary(
    val totalBoxes: Int,
    val completedBoxes: Int,
    /** Money recorded by ticked boxes. */
    val savedPaise: Long,
    /** What the whole grid adds up to if every box is ticked. */
    val plannedPaise: Long,
) {
    val remainingBoxes: Int get() = totalBoxes - completedBoxes

    companion object {
        val EMPTY = DenominationSummary(0, 0, 0, 0)
    }
}

/** Savings split by how they were recorded. Money values are in paise. */
data class SavingsByType(
    val boxesPaise: Long,
    val customPaise: Long,
    val startingPaise: Long,
) {
    /** Everything typed in by hand (custom entries + starting amounts). */
    val manualPaise: Long get() = customPaise + startingPaise
    val totalPaise: Long get() = boxesPaise + customPaise + startingPaise
}

/** Total saved in one calendar month. */
data class MonthlySavings(
    val month: YearMonth,
    val totalPaise: Long,
    val entryCount: Int,
)
