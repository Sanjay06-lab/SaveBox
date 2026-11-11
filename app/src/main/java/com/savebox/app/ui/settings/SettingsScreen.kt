package com.savebox.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.savebox.app.BuildConfig
import com.savebox.app.data.preferences.ThemeMode
import com.savebox.app.data.preferences.UserPreferences
import com.savebox.app.domain.reminders.ReminderFrequency
import com.savebox.app.ui.AppViewModelProvider
import com.savebox.app.ui.components.ConfirmDialog
import com.savebox.app.ui.components.LocalAppSnackbar
import com.savebox.app.ui.components.ScreenHeader
import com.savebox.app.ui.theme.spacing
import com.savebox.app.utils.Currency
import com.savebox.app.utils.DateFormatter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Which dialog is open on the Settings screen. */
private enum class SettingsDialog { CURRENCY, REMINDER_TIME, REMINDER_DAY, WIDGET_GOAL, ABOUT, PRIVACY }

private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalAppSnackbar.current
    val context = LocalContext.current
    var openDialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }
    val preferences = uiState.preferences
    val showMessage: (String) -> Unit = { message -> snackbar.showMessage(message) }

    // ----- System file pickers (no storage permission needed) -----
    val createBackupFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.writeBackup(uri, showMessage)
    }
    val openBackupFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.readBackupForRestore(uri, showMessage)
    }
    val createCsvFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) viewModel.exportCsv(uri, showMessage)
    }

    // ----- Notification permission (Android 13+) -----
    // The setting is saved either way; if the user says no, we explain how to allow it later.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) showMessage("Notifications are off. You can allow them in Android Settings › Apps › SaveBox.")
    }
    val askForNotificationsIfNeeded = {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    if (uiState.isLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = MaterialTheme.spacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
    ) {
        item { ScreenHeader(title = "Settings") }

        // ----- Appearance -----
        item {
            SettingsSection(title = "Appearance") {
                SettingsRow(title = "Theme")
                ThemeSelector(
                    selected = preferences.themeMode,
                    onSelect = viewModel::setThemeMode,
                    modifier = Modifier.padding(
                        start = MaterialTheme.spacing.medium,
                        end = MaterialTheme.spacing.medium,
                        bottom = MaterialTheme.spacing.medium,
                    ),
                )
                SettingsDivider()
                SettingsRow(
                    title = "Currency",
                    subtitle = "${preferences.currency.symbol.trim()}  ${preferences.currency.displayName}",
                    onClick = { openDialog = SettingsDialog.CURRENCY },
                )
            }
        }

        // ----- Reminders -----
        item {
            SettingsSection(title = "Reminders") {
                SettingsSwitchRow(
                    title = "Savings reminders",
                    subtitle = reminderSummary(preferences),
                    checked = preferences.remindersEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) askForNotificationsIfNeeded()
                        viewModel.updateReminder(enabled = enabled)
                    },
                )
                if (preferences.remindersEnabled) {
                    SettingsDivider()
                    FrequencySelector(
                        selected = preferences.reminderFrequency,
                        onSelect = { frequency -> viewModel.updateReminder(frequency = frequency) },
                        modifier = Modifier.padding(MaterialTheme.spacing.medium),
                    )
                    if (preferences.reminderFrequency == ReminderFrequency.WEEKLY) {
                        SettingsRow(
                            title = "Day",
                            subtitle = dayName(preferences.reminderDayOfWeek),
                            onClick = { openDialog = SettingsDialog.REMINDER_DAY },
                        )
                    }
                    SettingsRow(
                        title = "Time",
                        subtitle = LocalTime.of(preferences.reminderHour, preferences.reminderMinute).format(timeFormat),
                        onClick = { openDialog = SettingsDialog.REMINDER_TIME },
                    )
                    Text(
                        text = "No reminder is sent if you've already saved today (or this week).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
                    )
                }
                SettingsDivider()
                SettingsSwitchRow(
                    title = "Milestone celebrations",
                    subtitle = "A notification at 25%, 50%, 75% and 100% of a goal",
                    checked = preferences.milestoneNotificationsEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) askForNotificationsIfNeeded()
                        viewModel.setMilestoneNotifications(enabled)
                    },
                )
            }
        }

        // ----- Widget -----
        item {
            SettingsSection(title = "Home-screen widget") {
                val widgetGoal = uiState.goals.firstOrNull { goal -> goal.id == preferences.widgetGoalId }
                SettingsRow(
                    title = "Goal shown on the widget",
                    subtitle = widgetGoal?.let { goal -> "${goal.emoji} ${goal.name}" } ?: "Automatic (most recently updated goal)",
                    onClick = { openDialog = SettingsDialog.WIDGET_GOAL },
                )
                Text(
                    text = "To add the widget, long-press your home screen, choose Widgets and find SaveBox.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
                )
            }
        }

        // ----- Data -----
        item {
            SettingsSection(title = "Your data") {
                SettingsRow(
                    title = "Back up data",
                    subtitle = "Save all goals and savings to a file you choose",
                    enabled = !viewModel.isWorking,
                    onClick = { createBackupFile.launch("savebox-backup-${LocalDate.now()}.json") },
                )
                SettingsDivider()
                SettingsRow(
                    title = "Restore from backup",
                    subtitle = "Replace all current data with a backup file",
                    enabled = !viewModel.isWorking,
                    onClick = { openBackupFile.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                )
                SettingsDivider()
                SettingsRow(
                    title = "Export savings history",
                    subtitle = "A CSV file for Excel or Google Sheets",
                    enabled = !viewModel.isWorking,
                    onClick = { createCsvFile.launch("savebox-history-${LocalDate.now()}.csv") },
                )
                Text(
                    text = "🔒 Files are saved only where you choose. SaveBox never uploads your data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.small),
                )
            }
        }

        // ----- About -----
        item {
            SettingsSection(title = "About") {
                SettingsRow(title = "About SaveBox", onClick = { openDialog = SettingsDialog.ABOUT })
                SettingsDivider()
                SettingsRow(title = "Privacy", onClick = { openDialog = SettingsDialog.PRIVACY })
                SettingsDivider()
                SettingsRow(title = "Version", subtitle = BuildConfig.VERSION_NAME)
            }
        }

        item {
            Text(
                text = "SaveBox records the amounts you enter. It does not hold, move or verify real money.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.screenPadding),
            )
        }
    }

    // ----- Dialogs -----
    when (openDialog) {
        SettingsDialog.CURRENCY -> ChoiceDialog(
            title = "Currency",
            options = Currency.entries,
            selected = preferences.currency,
            optionLabel = { currency -> "${currency.symbol.trim()}  ${currency.displayName}" },
            onSelect = { currency ->
                viewModel.setCurrency(currency)
                openDialog = null
            },
            onDismiss = { openDialog = null },
        )
        SettingsDialog.REMINDER_DAY -> ChoiceDialog(
            title = "Reminder day",
            options = DayOfWeek.entries,
            selected = preferences.reminderDayOfWeek,
            optionLabel = ::dayName,
            onSelect = { day ->
                viewModel.updateReminder(dayOfWeek = day)
                openDialog = null
            },
            onDismiss = { openDialog = null },
        )
        SettingsDialog.REMINDER_TIME -> ReminderTimeDialog(
            hour = preferences.reminderHour,
            minute = preferences.reminderMinute,
            onConfirm = { hour, minute ->
                viewModel.updateReminder(hour = hour, minute = minute)
                openDialog = null
            },
            onDismiss = { openDialog = null },
        )
        SettingsDialog.WIDGET_GOAL -> ChoiceDialog(
            title = "Widget goal",
            // null = automatic
            options = listOf<Long?>(null) + uiState.goals.map { goal -> goal.id },
            selected = preferences.widgetGoalId,
            optionLabel = { goalId ->
                uiState.goals.firstOrNull { goal -> goal.id == goalId }?.let { goal -> "${goal.emoji} ${goal.name}" }
                    ?: "Automatic"
            },
            onSelect = { goalId ->
                viewModel.setWidgetGoal(goalId)
                openDialog = null
            },
            onDismiss = { openDialog = null },
        )
        SettingsDialog.ABOUT -> InfoDialog(title = "About SaveBox", message = ABOUT_TEXT, onDismiss = { openDialog = null })
        SettingsDialog.PRIVACY -> InfoDialog(title = "Privacy", message = PRIVACY_TEXT, onDismiss = { openDialog = null })
        null -> Unit
    }

    // Restore confirmation (shown after the chosen file was read and checked).
    viewModel.pendingRestore?.let { pending ->
        ConfirmDialog(
            title = "Replace all data?",
            message = "This backup is from ${DateFormatter.formatDateTime(pending.summary.exportedAtMillis)} and contains " +
                "${countText(pending.summary.goalCount, "goal")} and " +
                "${countText(pending.summary.transactionCount, "savings entry", "savings entries")}.\n\n" +
                "Everything currently in SaveBox will be replaced. Tip: back up your current data first.",
            confirmLabel = "Replace",
            isDestructive = true,
            onConfirm = { viewModel.confirmRestore(showMessage) },
            onDismiss = viewModel::cancelRestore,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    val options = ThemeMode.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) { Text(mode.label) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencySelector(selected: ReminderFrequency, onSelect: (ReminderFrequency) -> Unit, modifier: Modifier = Modifier) {
    val options = ReminderFrequency.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, frequency ->
            SegmentedButton(
                selected = frequency == selected,
                onClick = { onSelect(frequency) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) { Text(frequency.label) }
        }
    }
}

/** "1 goal", "3 goals" */
internal fun countText(count: Int, singular: String, plural: String = singular + "s"): String {
    return "$count ${if (count == 1) singular else plural}"
}

private fun dayName(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, Locale.ENGLISH)

private fun reminderSummary(preferences: UserPreferences): String {
    if (!preferences.remindersEnabled) return "Off"
    val time = LocalTime.of(preferences.reminderHour, preferences.reminderMinute).format(timeFormat)
    return when (preferences.reminderFrequency) {
        ReminderFrequency.DAILY -> "Every day at $time"
        ReminderFrequency.WEEKLY -> "Every ${dayName(preferences.reminderDayOfWeek)} at $time"
    }
}

private const val ABOUT_TEXT =
    "SaveBox - Small savings. Bigger possibilities.\n\n" +
        "SaveBox helps you save for the things that matter by tracking small, regular savings. " +
        "Create goals, fill denomination boxes or add any amount, and watch your progress grow.\n\n" +
        "SaveBox is free, works offline and needs no account.\n\n" +
        "Important: SaveBox only records the amounts you enter. It does not hold, transfer or verify " +
        "money, and it never connects to a bank."

private const val PRIVACY_TEXT =
    "Your data stays on your phone.\n\n" +
        "• No account or registration.\n" +
        "• No internet permission: SaveBox cannot send your data anywhere.\n" +
        "• No ads, analytics or tracking.\n" +
        "• Backups and exports are saved only to the place you pick, and only when you ask.\n" +
        "• Android's own device backup (if you turned it on) may include SaveBox data, " +
        "like any other app. You control this in Android settings.\n\n" +
        "Uninstalling SaveBox deletes all of its data from the phone, so make a backup first if you want to keep it."
