package com.savebox.app.domain.calculations

import com.savebox.app.domain.model.SavingsGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SavingsCalculatorTest {

    /** Helper to build a goal with only the values that matter for maths. */
    private fun goal(savedPaise: Long, targetPaise: Long, id: Long = 1, deadline: LocalDate? = null) = SavingsGoal(
        id = id,
        name = "Goal $id",
        emoji = "🎯",
        description = "",
        targetAmountPaise = targetPaise,
        deadline = deadline,
        createdAtMillis = 0,
        savedAmountPaise = savedPaise,
        lastActivityAtMillis = 0,
    )

    private val target = 7_000_000L // ₹70,000

    @Test
    fun `zero savings`() {
        val goal = goal(savedPaise = 0, targetPaise = target)
        assertEquals(0, SavingsCalculator.progressPercent(goal))
        assertEquals(0f, SavingsCalculator.progressFraction(goal))
        assertEquals(target, SavingsCalculator.remainingAmount(goal))
        assertFalse(SavingsCalculator.isCompleted(goal))
    }

    @Test
    fun `partial savings`() {
        val goal = goal(savedPaise = 1_750_000, targetPaise = target) // ₹17,500 = 25%
        assertEquals(25, SavingsCalculator.progressPercent(goal))
        assertEquals(0.25f, SavingsCalculator.progressFraction(goal))
        assertEquals(5_250_000L, SavingsCalculator.remainingAmount(goal))
    }

    @Test
    fun `percentage is rounded down`() {
        // 1/3 = 33.33% -> 33
        assertEquals(33, SavingsCalculator.progressPercent(savedPaise = 100, targetPaise = 300))
    }

    @Test
    fun `completed goal`() {
        val goal = goal(savedPaise = target, targetPaise = target)
        assertEquals(100, SavingsCalculator.progressPercent(goal))
        assertEquals(1f, SavingsCalculator.progressFraction(goal))
        assertEquals(0L, SavingsCalculator.remainingAmount(goal))
        assertTrue(SavingsCalculator.isCompleted(goal))
    }

    @Test
    fun `savings above target keep real numbers but cap the visual progress`() {
        val goal = goal(savedPaise = 7_700_000, targetPaise = target) // ₹77,000 of ₹70,000
        assertEquals(110, SavingsCalculator.progressPercent(goal)) // real percentage
        assertEquals(1f, SavingsCalculator.progressFraction(goal)) // bar stays full, not 110%
        assertEquals(0L, SavingsCalculator.remainingAmount(goal)) // never negative
        assertEquals(700_000L, SavingsCalculator.excessAmount(goal)) // extra ₹7,000 is kept
    }

    @Test
    fun `zero target never divides by zero`() {
        assertEquals(0, SavingsCalculator.progressPercent(savedPaise = 500, targetPaise = 0))
        assertEquals(0f, SavingsCalculator.progressFraction(savedPaise = 500, targetPaise = 0))
    }

    @Test
    fun `summary of no goals is empty`() {
        val summary = SavingsCalculator.summarize(emptyList())
        assertEquals(SavingsSummary.EMPTY, summary)
    }

    @Test
    fun `summary adds up all goals`() {
        val goals = listOf(
            goal(id = 1, savedPaise = 500_000, targetPaise = 1_000_000),   // ₹5,000 of ₹10,000
            goal(id = 2, savedPaise = 1_000_000, targetPaise = 3_000_000), // ₹10,000 of ₹30,000
        )
        val summary = SavingsCalculator.summarize(goals)

        assertEquals(1_500_000L, summary.totalSavedPaise)
        assertEquals(4_000_000L, summary.totalTargetPaise)
        assertEquals(2_500_000L, summary.totalRemainingPaise)
        assertEquals(37, summary.overallPercent) // 15,000 / 40,000 = 37.5%
        assertEquals(2, summary.goalCount)
        assertEquals(0, summary.completedGoalCount)
    }

    @Test
    fun `extra savings in one goal do not reduce another goal's remaining amount`() {
        val goals = listOf(
            goal(id = 1, savedPaise = 1_500_000, targetPaise = 1_000_000), // ₹5,000 extra
            goal(id = 2, savedPaise = 0, targetPaise = 1_000_000),
        )
        val summary = SavingsCalculator.summarize(goals)

        assertEquals(1_500_000L, summary.totalSavedPaise) // real total is preserved
        assertEquals(1_000_000L, summary.totalRemainingPaise) // goal 2 still needs ₹10,000
        assertEquals(50, summary.overallPercent) // extra doesn't count towards goal 2
        assertEquals(1, summary.completedGoalCount)
        assertEquals(1, summary.activeGoalCount)
    }

    @Test
    fun `days until deadline`() {
        val today = LocalDate.of(2026, 1, 1)
        assertNull(SavingsCalculator.daysUntilDeadline(goal(0, target), today))
        assertEquals(10L, SavingsCalculator.daysUntilDeadline(goal(0, target, deadline = LocalDate.of(2026, 1, 11)), today))
        assertEquals(-1L, SavingsCalculator.daysUntilDeadline(goal(0, target, deadline = LocalDate.of(2025, 12, 31)), today))
    }
}
