package com.savebox.app.ui.settings

import android.content.ContentResolver
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.savebox.app.AppDataObserver
import com.savebox.app.data.backup.BackupException
import com.savebox.app.data.backup.BackupFile
import com.savebox.app.data.backup.BackupManager
import com.savebox.app.data.backup.BackupSummary
import com.savebox.app.data.backup.CsvExporter
import com.savebox.app.data.preferences.ThemeMode
import com.savebox.app.data.preferences.UserPreferences
import com.savebox.app.data.preferences.UserPreferencesRepository
import com.savebox.app.data.repository.SavingsRepository
import com.savebox.app.domain.model.SavingsGoal
import com.savebox.app.domain.reminders.ReminderFrequency
import com.savebox.app.notifications.ReminderScheduler
import com.savebox.app.utils.Currency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.DayOfWeek

data class SettingsUiState(
    val isLoading: Boolean = true,
    val preferences: UserPreferences = UserPreferences(),
    val goals: List<SavingsGoal> = emptyList(),
)

/** A backup file that was read and checked, waiting for the user to confirm "Replace my data". */
data class PendingRestore(val backup: BackupFile, val summary: BackupSummary)

/**
 * Settings: theme, currency, reminders, widget goal, backup/restore and export.
 *
 * @param contentResolver Android's way to read/write files the user picked
 *                        (with the system file picker) - no storage permission needed.
 */
class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val savingsRepository: SavingsRepository,
    private val reminderScheduler: ReminderScheduler,
    private val backupManager: BackupManager,
    private val appDataObserver: AppDataObserver,
    private val contentResolver: ContentResolver,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.userPreferences,
        savingsRepository.observeGoals(),
    ) { preferences, goals ->
        SettingsUiState(isLoading = false, preferences = preferences, goals = goals)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    var pendingRestore by mutableStateOf<PendingRestore?>(null)
        private set

    /** True while a backup/restore/export is running (buttons are disabled). */
    var isWorking by mutableStateOf(false)
        private set

    // ----- Appearance -----

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setCurrency(currency: Currency) {
        viewModelScope.launch { preferencesRepository.setCurrency(currency) }
    }

    // ----- Reminders -----

    /** Saves the reminder settings and (re)schedules or cancels the background job. */
    fun updateReminder(
        enabled: Boolean = uiState.value.preferences.remindersEnabled,
        frequency: ReminderFrequency = uiState.value.preferences.reminderFrequency,
        hour: Int = uiState.value.preferences.reminderHour,
        minute: Int = uiState.value.preferences.reminderMinute,
        dayOfWeek: DayOfWeek = uiState.value.preferences.reminderDayOfWeek,
    ) {
        viewModelScope.launch {
            preferencesRepository.setReminder(enabled, frequency, hour, minute, dayOfWeek)
            reminderScheduler.apply(preferencesRepository.userPreferences.first())
        }
    }

    fun setMilestoneNotifications(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setMilestoneNotificationsEnabled(enabled) }
    }

    // ----- Widget -----

    fun setWidgetGoal(goalId: Long?) {
        viewModelScope.launch { preferencesRepository.setWidgetGoalId(goalId) }
    }

    // ----- Backup, restore, export -----

    fun writeBackup(uri: Uri, onResult: (String) -> Unit) = runFileTask(onResult) {
        val json = backupManager.createBackupJson()
        writeText(uri, json)
        "Backup saved. Keep the file somewhere safe."
    }

    fun exportCsv(uri: Uri, onResult: (String) -> Unit) = runFileTask(onResult) {
        val entries = savingsRepository.observeAllTransactions().first()
        val currency = preferencesRepository.userPreferences.first().currency
        writeText(uri, CsvExporter.buildCsv(entries, currency))
        "Exported ${countText(entries.size, "savings entry", "savings entries")}."
    }

    /** Step 1 of restoring: read and check the file, then ask the user to confirm. */
    fun readBackupForRestore(uri: Uri, onResult: (String) -> Unit) = runFileTask(onResult) {
        val text = readText(uri)
        val backup = backupManager.parseBackup(text)
        pendingRestore = PendingRestore(backup, backupManager.summarize(backup))
        null // no message yet - the confirmation dialog opens instead
    }

    /** Step 2 of restoring: the user confirmed. Replace everything. */
    fun confirmRestore(onResult: (String) -> Unit) {
        val restore = pendingRestore ?: return
        pendingRestore = null
        runFileTask(onResult) {
            appDataObserver.resetMilestones()
            backupManager.restore(restore.backup)
            "Backup restored: ${countText(restore.summary.goalCount, "goal")}, " +
                "${countText(restore.summary.transactionCount, "savings entry", "savings entries")}."
        }
    }

    fun cancelRestore() {
        pendingRestore = null
    }

    /**
     * Runs a file task with clear error messages. [task] returns the success message
     * (or null for no message).
     */
    private fun runFileTask(onResult: (String) -> Unit, task: suspend () -> String?) {
        if (isWorking) return
        isWorking = true
        viewModelScope.launch {
            val message = try {
                task()
            } catch (exception: BackupException) {
                exception.message
            } catch (exception: IOException) {
                "Couldn't read or write that file. Please try another location."
            } catch (exception: SecurityException) {
                "SaveBox isn't allowed to use that file. Please pick it again."
            }
            isWorking = false
            if (message != null) onResult(message)
        }
    }

    private suspend fun writeText(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        // "wt" = write and truncate (replace the file's old contents).
        val stream = contentResolver.openOutputStream(uri, "wt") ?: throw IOException("No output stream")
        stream.use { output -> output.write(text.toByteArray(Charsets.UTF_8)) }
    }

    private suspend fun readText(uri: Uri): String = withContext(Dispatchers.IO) {
        val stream = contentResolver.openInputStream(uri) ?: throw IOException("No input stream")
        stream.use { input ->
            // Refuse unreasonably large files (a real backup is far smaller).
            val bytes = input.readBytes()
            if (bytes.size > MAX_BACKUP_BYTES) throw BackupException("This file is too large to be a SaveBox backup.")
            bytes.toString(Charsets.UTF_8)
        }
    }

    private companion object {
        const val MAX_BACKUP_BYTES = 20 * 1024 * 1024 // 20 MB
    }
}
