package com.savebox.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.savebox.app.R
import com.savebox.app.navigation.DeepLinks

/**
 * Everything about showing notifications in one place.
 *
 * Android groups notifications into "channels" so users can turn each kind on or off
 * in system settings. SaveBox has two: reminders and milestones.
 */
object SaveBoxNotifications {

    private const val CHANNEL_REMINDERS = "savings_reminders"
    private const val CHANNEL_MILESTONES = "goal_milestones"

    private const val REMINDER_NOTIFICATION_ID = 1001
    private const val MILESTONE_NOTIFICATION_ID_BASE = 2000

    /** Call once at app start. Creating an existing channel again does nothing. */
    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Savings reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Daily or weekly nudges to record your savings"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_MILESTONES, "Goal milestones", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Celebrations when a goal reaches 25%, 50%, 75% and 100%"
            },
        )
    }

    /**
     * True if SaveBox may show notifications right now.
     * Android 13+ needs the user's permission; on any version the user can block them.
     */
    fun canPostNotifications(context: Context): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showReminder(context: Context, message: String) {
        show(
            context = context,
            channelId = CHANNEL_REMINDERS,
            notificationId = REMINDER_NOTIFICATION_ID,
            title = "Time to save 💰",
            message = message,
            goalId = null,
        )
    }

    fun showMilestone(context: Context, goalId: Long, message: String) {
        show(
            context = context,
            channelId = CHANNEL_MILESTONES,
            // One notification per goal: a newer milestone replaces the older one.
            notificationId = MILESTONE_NOTIFICATION_ID_BASE + (goalId % 100_000).toInt(),
            title = "Milestone reached!",
            message = message,
            goalId = goalId,
        )
    }

    private fun show(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        goalId: Long?,
    ) {
        if (!canPostNotifications(context)) return

        // Tapping the notification opens SaveBox (at the goal, if there is one).
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            DeepLinks.openApp(context, goalId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.savebox_emerald))
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (exception: SecurityException) {
            // Permission was removed between the check and now - nothing to do.
        }
    }
}
