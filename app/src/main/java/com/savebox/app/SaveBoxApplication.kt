package com.savebox.app

import android.app.Application
import com.savebox.app.notifications.SaveBoxNotifications

/**
 * Runs once when the app process starts, before any screen.
 *
 * It creates the AppContainer: the single place that builds the repositories
 * and hands them to ViewModels. (We use this simple "manual dependency injection"
 * instead of a library like Hilt, so there is no hidden magic to learn.)
 */
class SaveBoxApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Notification channels must exist before any notification is shown.
        SaveBoxNotifications.createChannels(this)

        // Start app-wide background helpers (currency, milestones, widget refresh).
        container.appDataObserver.start()
    }
}
