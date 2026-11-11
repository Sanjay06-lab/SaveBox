package com.savebox.app.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.savebox.app.data.preferences.UserPreferences
import com.savebox.app.domain.reminders.ReminderSchedule
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Turns the reminder settings into a WorkManager job.
 *
 * WorkManager is Android's official tool for background tasks that must run even
 * if the app is closed or the phone restarts. It needs no internet and no server.
 */
class ReminderScheduler(context: Context) {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    /** Schedule, reschedule or cancel the reminder to match the settings. */
    fun apply(preferences: UserPreferences) {
        if (!preferences.remindersEnabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val initialDelay = ReminderSchedule.delayUntilNext(
            now = ZonedDateTime.now(),
            frequency = preferences.reminderFrequency,
            time = LocalTime.of(preferences.reminderHour, preferences.reminderMinute),
            dayOfWeek = preferences.reminderDayOfWeek,
        )
        val interval = ReminderSchedule.repeatInterval(preferences.reminderFrequency)

        val request = PeriodicWorkRequestBuilder<ReminderWorker>(interval.toMinutes(), TimeUnit.MINUTES)
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .build()

        // "Unique" work: there is only ever one reminder job. Changing the settings replaces it.
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request,
        )
    }

    private companion object {
        const val WORK_NAME = "savebox_savings_reminder"
    }
}
