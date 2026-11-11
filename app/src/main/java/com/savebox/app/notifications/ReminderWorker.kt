package com.savebox.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.savebox.app.SaveBoxApplication
import com.savebox.app.domain.calculations.SavingsCalculator
import com.savebox.app.domain.model.MotivationMessages
import com.savebox.app.domain.reminders.ReminderSchedule
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Runs in the background at the reminder time and decides whether to show a reminder.
 *
 * It stays quiet (no unnecessary notifications) when:
 *  - reminders were switched off,
 *  - there are no active goals to save for,
 *  - the user already saved today (daily) or this week (weekly),
 *  - notifications are not allowed.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as SaveBoxApplication).container
        val preferences = container.userPreferencesRepository.userPreferences.first()
        if (!preferences.remindersEnabled) return Result.success()

        val goals = container.savingsRepository.observeGoals().first()
        val hasActiveGoal = goals.any { goal -> !SavingsCalculator.isCompleted(goal) }
        if (!hasActiveGoal) return Result.success()

        val zone = ZoneId.systemDefault()
        val lastSaving = container.savingsRepository.getLatestSavingCreatedAt()
            ?.let { millis -> Instant.ofEpochMilli(millis).atZone(zone) }
        if (ReminderSchedule.shouldSkip(ZonedDateTime.now(zone), preferences.reminderFrequency, lastSaving)) {
            return Result.success()
        }

        SaveBoxNotifications.showReminder(applicationContext, MotivationMessages.reminderMessages.random())
        return Result.success()
    }
}
