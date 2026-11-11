package com.savebox.app.domain.calculations

import com.savebox.app.domain.model.Denomination
import com.savebox.app.domain.model.SavingsTransaction
import com.savebox.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * Tests for the grid, by-type and monthly calculations, plus the
 * "deleted / edited / unmarked" cases from the requirements.
 */
class SavingsCalculatorPhase4Test {

    private val utc = ZoneOffset.UTC

    private fun transaction(
        id: Long,
        amountPaise: Long,
        type: TransactionType = TransactionType.CUSTOM,
        savedAt: LocalDateTime = LocalDateTime.of(2026, 11, 5, 10, 0),
    ) = SavingsTransaction(
        id = id,
        goalId = 1,
        amountPaise = amountPaise,
        type = type,
        note = "",
        source = "",
        savedAtMillis = savedAt.toInstant(utc).toEpochMilli(),
        createdAtMillis = 0,
    )

    private fun row(valueRupees: Long, quantity: Int, marked: Set<Int>) = Denomination(
        id = valueRupees,
        goalId = 1,
        valuePaise = valueRupees * 100,
        quantity = quantity,
        markedBoxIndices = marked,
    )

    // ----- Denomination grid -----

    @Test
    fun `the PS5 example adds up to 70,000`() {
        val grid = listOf(row(500, 110, emptySet()), row(200, 60, emptySet()), row(100, 30, emptySet()))
        val summary = SavingsCalculator.summarizeDenominations(grid)

        assertEquals(200, summary.totalBoxes)
        assertEquals(0, summary.completedBoxes)
        assertEquals(7_000_000L, summary.plannedPaise) // ₹70,000
        assertEquals(0L, summary.savedPaise)
    }

    @Test
    fun `ticked boxes are counted and valued`() {
        val grid = listOf(row(500, 110, setOf(0, 1, 2)), row(100, 30, setOf(5)))
        val summary = SavingsCalculator.summarizeDenominations(grid)

        assertEquals(4, summary.completedBoxes)
        assertEquals(136, summary.remainingBoxes)
        assertEquals(160_000L, summary.savedPaise) // 3 × ₹500 + ₹100 = ₹1,600
    }

    @Test
    fun `unmarking a box lowers the grid total`() {
        val before = SavingsCalculator.summarizeDenominations(listOf(row(500, 10, setOf(0, 1, 2))))
        val after = SavingsCalculator.summarizeDenominations(listOf(row(500, 10, setOf(0, 2)))) // box 1 unticked

        assertEquals(150_000L, before.savedPaise)
        assertEquals(100_000L, after.savedPaise)
        assertEquals(before.completedBoxes - 1, after.completedBoxes)
    }

    @Test
    fun `empty grid`() {
        assertEquals(DenominationSummary.EMPTY, SavingsCalculator.summarizeDenominations(emptyList()))
    }

    @Test
    fun `boxes needed to cover an amount round up`() {
        assertEquals(110L, SavingsCalculator.boxesToCover(5_500_000, 50_000)) // ₹55,000 / ₹500
        assertEquals(4L, SavingsCalculator.boxesToCover(100_000, 30_000))     // ₹1,000 / ₹300 -> 4
        assertEquals(0L, SavingsCalculator.boxesToCover(0, 50_000))
        assertEquals(0L, SavingsCalculator.boxesToCover(100, 0))
    }

    // ----- By type -----

    @Test
    fun `totals by type separate boxes from manual entries`() {
        val transactions = listOf(
            transaction(1, 50_000, TransactionType.DENOMINATION),
            transaction(2, 50_000, TransactionType.DENOMINATION),
            transaction(3, 25_000, TransactionType.CUSTOM),
            transaction(4, 1_000_000, TransactionType.STARTING_AMOUNT),
        )
        val byType = SavingsCalculator.totalsByType(transactions)

        assertEquals(100_000L, byType.boxesPaise)
        assertEquals(25_000L, byType.customPaise)
        assertEquals(1_000_000L, byType.startingPaise)
        assertEquals(1_025_000L, byType.manualPaise)
        assertEquals(1_125_000L, byType.totalPaise)
    }

    // ----- Deleted and edited transactions -----

    @Test
    fun `deleting a transaction removes exactly its amount`() {
        val all = listOf(transaction(1, 50_000), transaction(2, 15_000), transaction(3, 100_000))
        val afterDelete = all.filterNot { it.id == 2L }

        assertEquals(165_000L, SavingsCalculator.totalOf(all))
        assertEquals(150_000L, SavingsCalculator.totalOf(afterDelete))
    }

    @Test
    fun `editing a transaction amount changes the total by the difference`() {
        val all = listOf(transaction(1, 50_000), transaction(2, 15_000))
        val edited = all.map { if (it.id == 2L) it.copy(amountPaise = 25_050) else it }

        assertEquals(65_000L, SavingsCalculator.totalOf(all))
        assertEquals(75_050L, SavingsCalculator.totalOf(edited))
    }

    // ----- Monthly summary -----

    @Test
    fun `monthly summary groups by month newest first`() {
        val transactions = listOf(
            transaction(1, 10_000, savedAt = LocalDateTime.of(2026, 10, 2, 9, 0)),
            transaction(2, 20_000, savedAt = LocalDateTime.of(2026, 11, 1, 9, 0)),
            transaction(3, 30_000, savedAt = LocalDateTime.of(2026, 11, 30, 23, 59)),
            transaction(4, 40_000, savedAt = LocalDateTime.of(2025, 11, 15, 12, 0)),
        )
        val months = SavingsCalculator.monthlySummary(transactions, utc)

        assertEquals(
            listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 10), YearMonth.of(2025, 11)),
            months.map { it.month },
        )
        assertEquals(50_000L, months[0].totalPaise)
        assertEquals(2, months[0].entryCount)
        assertEquals(10_000L, months[1].totalPaise)
        assertEquals(40_000L, months[2].totalPaise)
    }

    @Test
    fun `monthly summary uses the phone's time zone`() {
        // 31 Oct 2026, 20:00 UTC is already 1 Nov 2026 in India (UTC+5:30).
        val lateNight = transaction(1, 10_000, savedAt = LocalDateTime.of(2026, 10, 31, 20, 0))
        val india = ZoneOffset.ofHoursMinutes(5, 30)

        assertEquals(YearMonth.of(2026, 10), SavingsCalculator.monthlySummary(listOf(lateNight), utc).single().month)
        assertEquals(YearMonth.of(2026, 11), SavingsCalculator.monthlySummary(listOf(lateNight), india).single().month)
    }

    @Test
    fun `monthly summary of nothing is empty`() {
        assertEquals(emptyList<MonthlySavings>(), SavingsCalculator.monthlySummary(emptyList(), utc))
    }
}
