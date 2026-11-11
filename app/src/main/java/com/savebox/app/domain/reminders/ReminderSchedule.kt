package com.savebox.app.domain.reminders

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * Pure time maths for reminders, kept separate from WorkManager so it can be unit tested.
 */
object ReminderSchedule {

    /**
     * How long to wait from [now] until the next reminder.
     *  - DAILY: next occurrence of [time] (today if still ahead, else tomorrow).
     *  - WEEKLY: next [dayOfWeek] at [time] (today if it's that day and the time is ahead).
     */
    fun delayUntilNext(
        now: ZonedDateTime,
        frequency: ReminderFrequency,
        time: LocalTime,
        dayOfWeek: DayOfWeek,
    ): Duration {
        var next = now.with(time).withSecond(0).withNano(0)

        when (frequency) {
            ReminderFrequency.DAILY -> {
                if (!next.isAfter(now)) next = next.plusDays(1)
            }
            ReminderFrequency.WEEKLY -> {
                next = next.with(TemporalAdjusters.nextOrSame(dayOfWeek))
                if (!next.isAfter(now)) next = next.plusWeeks(1)
            }
        }
        return Duration.between(now, next)
    }

    /** Time between two reminders. */
    fun repeatInterval(frequency: ReminderFrequency): Duration {
        return when (frequency) {
            ReminderFrequency.DAILY -> Duration.ofDays(1)
            ReminderFrequency.WEEKLY -> Duration.ofDays(7)
        }
    }

    /**
     * "Don't send unnecessary notifications": skip the reminder if the user already
     * saved recently enough.
     *  - DAILY: skip if something was saved since the start of today.
     *  - WEEKLY: skip if something was saved in the last 7 days.
     */
    fun shouldSkip(
        now: ZonedDateTime,
        frequency: ReminderFrequency,
        lastSavingAt: ZonedDateTime?,
    ): Boolean {
        if (lastSavingAt == null) return false
        val windowStart = when (frequency) {
            ReminderFrequency.DAILY -> now.toLocalDate().atStartOfDay(now.zone)
            ReminderFrequency.WEEKLY -> now.minusDays(7)
        }
        return !lastSavingAt.isBefore(windowStart)
    }
}
