package com.savebox.app

import com.savebox.app.data.backup.CsvExporter
import com.savebox.app.domain.calculations.Milestones
import com.savebox.app.domain.model.SavingsGoal
import com.savebox.app.domain.model.SavingsTransaction
import com.savebox.app.domain.model.TransactionType
import com.savebox.app.domain.model.TransactionWithGoal
import com.savebox.app.domain.reminders.ReminderFrequency
import com.savebox.app.domain.reminders.ReminderSchedule
import com.savebox.app.utils.Currency
import com.savebox.app.widget.WidgetDataBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

/** Unit tests for the Phase 5 logic: milestones, reminder timing, CSV export, widget choice. */
class Phase5LogicTest {

    private val india = ZoneOffset.ofHoursMinutes(5, 30)

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): ZonedDateTime =
        ZonedDateTime.of(LocalDateTime.of(year, month, day, hour, minute), india)

    // ----- Milestones -----

    @Test
    fun `highest milestone reached`() {
        assertEquals(0, Milestones.highestReached(0))
        assertEquals(0, Milestones.highestReached(24))
        assertEquals(25, Milestones.highestReached(25))
        assertEquals(50, Milestones.highestReached(74))
        assertEquals(100, Milestones.highestReached(130))
    }

    @Test
    fun `milestones are announced once and only when going up`() {
        assertNull("new goal is only recorded", Milestones.milestoneToAnnounce(60, alreadyNotified = null))
        assertEquals(50, Milestones.milestoneToAnnounce(60, alreadyNotified = 25))
        assertNull("already announced", Milestones.milestoneToAnnounce(60, alreadyNotified = 50))
        assertNull("unticking never re-announces", Milestones.milestoneToAnnounce(30, alreadyNotified = 50))
        assertEquals(100, Milestones.milestoneToAnnounce(100, alreadyNotified = 75))
    }

    // ----- Reminder timing -----

    @Test
    fun `daily reminder later today`() {
        val delay = ReminderSchedule.delayUntilNext(at(2026, 11, 11, 18), ReminderFrequency.DAILY, LocalTime.of(20, 0), DayOfWeek.SUNDAY)
        assertEquals(Duration.ofHours(2), delay)
    }

    @Test
    fun `daily reminder already passed moves to tomorrow`() {
        val delay = ReminderSchedule.delayUntilNext(at(2026, 11, 11, 21), ReminderFrequency.DAILY, LocalTime.of(20, 0), DayOfWeek.SUNDAY)
        assertEquals(Duration.ofHours(23), delay)
    }

    @Test
    fun `weekly reminder on the chosen day`() {
        // 11 Nov 2026 is a Wednesday. Next Sunday 8 pm is 15 Nov.
        val delay = ReminderSchedule.delayUntilNext(at(2026, 11, 11, 20), ReminderFrequency.WEEKLY, LocalTime.of(20, 0), DayOfWeek.SUNDAY)
        assertEquals(Duration.ofDays(4), delay)
    }

    @Test
    fun `weekly reminder on the same day but time passed waits a week`() {
        val delay = ReminderSchedule.delayUntilNext(at(2026, 11, 11, 21), ReminderFrequency.WEEKLY, LocalTime.of(20, 0), DayOfWeek.WEDNESDAY)
        assertEquals(Duration.ofDays(7).minusHours(1), delay)
    }

    @Test
    fun `reminder is skipped if the user already saved`() {
        val now = at(2026, 11, 11, 20)
        assertTrue(ReminderSchedule.shouldSkip(now, ReminderFrequency.DAILY, lastSavingAt = at(2026, 11, 11, 9)))
        assertFalse(ReminderSchedule.shouldSkip(now, ReminderFrequency.DAILY, lastSavingAt = at(2026, 11, 10, 23)))
        assertTrue(ReminderSchedule.shouldSkip(now, ReminderFrequency.WEEKLY, lastSavingAt = at(2026, 11, 6, 9)))
        assertFalse(ReminderSchedule.shouldSkip(now, ReminderFrequency.WEEKLY, lastSavingAt = at(2026, 11, 1, 9)))
        assertFalse(ReminderSchedule.shouldSkip(now, ReminderFrequency.DAILY, lastSavingAt = null))
    }

    // ----- CSV -----

    @Test
    fun `csv escapes commas and quotes`() {
        assertEquals("plain", CsvExporter.escape("plain"))
        assertEquals("\"a, b\"", CsvExporter.escape("a, b"))
        assertEquals("\"He said \"\"hi\"\"\"", CsvExporter.escape("He said \"hi\""))
        assertEquals("\"line1\nline2\"", CsvExporter.escape("line1\nline2"))
    }

    @Test
    fun `csv has a header and plain amounts`() {
        val entry = TransactionWithGoal(
            transaction = SavingsTransaction(
                id = 1, goalId = 1, amountPaise = 150_050, type = TransactionType.CUSTOM,
                note = "Birthday, from Gran", source = "Gift",
                savedAtMillis = at(2026, 11, 11, 9, 30).toInstant().toEpochMilli(), createdAtMillis = 0,
            ),
            goalName = "Laptop",
            goalEmoji = "💻",
        )
        val lines = CsvExporter.buildCsv(listOf(entry), Currency.INR, india).trimEnd().split("\r\n")

        assertEquals("Date,Time,Goal,Type,Amount (INR),Source,Note", lines[0])
        assertEquals("2026-11-11,09:30,Laptop,Custom savings,1500.50,Gift,\"Birthday, from Gran\"", lines[1])
    }

    // ----- Widget goal choice -----

    private fun goal(id: Long, saved: Long, target: Long, lastActivity: Long) = SavingsGoal(
        id = id, name = "Goal $id", emoji = "🎯", description = "", targetAmountPaise = target,
        deadline = null, createdAtMillis = 0, savedAmountPaise = saved, lastActivityAtMillis = lastActivity,
    )

    @Test
    fun `widget shows the chosen goal, else the latest unfinished goal`() {
        val finishedRecent = goal(1, saved = 100, target = 100, lastActivity = 300)
        val activeOlder = goal(2, saved = 10, target = 100, lastActivity = 200)
        val activeOldest = goal(3, saved = 0, target = 100, lastActivity = 100)
        val goals = listOf(finishedRecent, activeOlder, activeOldest)

        assertEquals(3L, WidgetDataBuilder.chooseGoal(goals, preferredGoalId = 3)?.id)
        assertEquals(2L, WidgetDataBuilder.chooseGoal(goals, preferredGoalId = null)?.id)
        assertEquals(2L, WidgetDataBuilder.chooseGoal(goals, preferredGoalId = 999)?.id) // deleted goal
        assertEquals(1L, WidgetDataBuilder.chooseGoal(listOf(finishedRecent), preferredGoalId = null)?.id)
        assertNull(WidgetDataBuilder.chooseGoal(emptyList(), preferredGoalId = null))
    }

    @Test
    fun `widget data is formatted in the chosen currency`() {
        val data = WidgetDataBuilder.build(listOf(goal(1, saved = 1_500_000, target = 7_000_000, lastActivity = 1)), null, Currency.INR)
        assertEquals("₹15,000", data.totalSavedText)
        assertEquals("₹55,000", data.goal?.remainingText)
        assertEquals(21, data.goal?.percent)
    }
}
