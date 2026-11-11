package com.savebox.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.savebox.app.data.backup.BackupManager
import com.savebox.app.data.database.SaveBoxDatabase
import com.savebox.app.data.preferences.UserPreferencesRepository
import com.savebox.app.data.repository.RoomSavingsRepository
import com.savebox.app.data.repository.SavingsRepository
import com.savebox.app.notifications.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Creates the single DataStore file "user_preferences" for the whole app. */
private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences",
)

/**
 * The AppContainer builds the app's long-lived objects ONCE and shares them.
 * (This is called "manual dependency injection".)
 *
 * ViewModels get their objects from here - see ui/AppViewModelProvider.kt.
 *
 * To swap an implementation (for example, the repository), you only change ONE line here.
 * "by lazy" means each object is created the first time someone uses it, not at app start.
 */
class AppContainer(context: Context) {

    private val applicationContext = context.applicationContext

    /**
     * A coroutine scope that lives as long as the app process. Used for work that must
     * not stop when a screen closes (widget refresh, milestone checks).
     * SupervisorJob = one failing task doesn't cancel the others.
     */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** The single database instance for the whole app. Opened the first time it is needed. */
    val database: SaveBoxDatabase by lazy {
        SaveBoxDatabase.create(applicationContext)
    }

    /** Goals, savings and denomination boxes, stored in the Room database on the phone. */
    val savingsRepository: SavingsRepository by lazy {
        RoomSavingsRepository(database)
    }

    /** Small settings (theme, currency, reminders...). Saved on the device. */
    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(applicationContext.userPreferencesDataStore)
    }

    /** Schedules savings reminders with WorkManager. */
    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(applicationContext)
    }

    /** Creates and restores backup files. */
    val backupManager: BackupManager by lazy {
        BackupManager(database, appVersion = BuildConfig.VERSION_NAME)
    }

    /** Milestone notifications, currency sync and widget refresh. */
    val appDataObserver: AppDataObserver by lazy {
        AppDataObserver(applicationContext, applicationScope, savingsRepository, userPreferencesRepository)
    }
}
