package com.savebox.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.savebox.app.domain.reminders.ReminderFrequency
import com.savebox.app.utils.Currency
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.DayOfWeek

/**
 * Small app settings saved on the device with Jetpack DataStore
 * (a modern replacement for SharedPreferences).
 *
 * Why not Room? Settings are a handful of single values (a switch, a theme choice),
 * not lists of records, and DataStore is designed exactly for that.
 *
 * TO ADD A SETTING: add a key in Keys, a field in UserPreferences (with a default),
 * read it in the map { } block, and add a setter function.
 */
class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) {

    /** Names of the saved values. Never rename a key, or users lose that setting. */
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val REMINDER_FREQUENCY = stringPreferencesKey("reminder_frequency")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val REMINDER_DAY_OF_WEEK = intPreferencesKey("reminder_day_of_week")
        val MILESTONE_NOTIFICATIONS = booleanPreferencesKey("milestone_notifications")
        val WIDGET_GOAL_ID = longPreferencesKey("widget_goal_id")

        /** Entries like "12:50" = goal 12 has already been congratulated for 50%. */
        val NOTIFIED_MILESTONES = stringSetPreferencesKey("notified_milestones")
    }

    val userPreferences: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            // If the file can't be read, use default settings instead of crashing.
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val defaults = UserPreferences()
            UserPreferences(
                onboardingCompleted = preferences[Keys.ONBOARDING_COMPLETED] ?: defaults.onboardingCompleted,
                themeMode = enumOrDefault(preferences[Keys.THEME_MODE], defaults.themeMode),
                currency = Currency.fromCode(preferences[Keys.CURRENCY_CODE]),
                remindersEnabled = preferences[Keys.REMINDERS_ENABLED] ?: defaults.remindersEnabled,
                reminderFrequency = enumOrDefault(preferences[Keys.REMINDER_FREQUENCY], defaults.reminderFrequency),
                reminderHour = preferences[Keys.REMINDER_HOUR] ?: defaults.reminderHour,
                reminderMinute = preferences[Keys.REMINDER_MINUTE] ?: defaults.reminderMinute,
                reminderDayOfWeek = preferences[Keys.REMINDER_DAY_OF_WEEK]
                    ?.let { day -> DayOfWeek.of(day.coerceIn(1, 7)) }
                    ?: defaults.reminderDayOfWeek,
                milestoneNotificationsEnabled = preferences[Keys.MILESTONE_NOTIFICATIONS] ?: defaults.milestoneNotificationsEnabled,
                widgetGoalId = preferences[Keys.WIDGET_GOAL_ID]?.takeIf { id -> id > 0 },
                notifiedMilestones = parseMilestones(preferences[Keys.NOTIFIED_MILESTONES].orEmpty()),
            )
        }

    suspend fun setOnboardingCompleted() {
        dataStore.edit { preferences -> preferences[Keys.ONBOARDING_COMPLETED] = true }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences -> preferences[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setCurrency(currency: Currency) {
        dataStore.edit { preferences -> preferences[Keys.CURRENCY_CODE] = currency.code }
    }

    suspend fun setReminder(
        enabled: Boolean,
        frequency: ReminderFrequency,
        hour: Int,
        minute: Int,
        dayOfWeek: DayOfWeek,
    ) {
        dataStore.edit { preferences ->
            preferences[Keys.REMINDERS_ENABLED] = enabled
            preferences[Keys.REMINDER_FREQUENCY] = frequency.name
            preferences[Keys.REMINDER_HOUR] = hour.coerceIn(0, 23)
            preferences[Keys.REMINDER_MINUTE] = minute.coerceIn(0, 59)
            preferences[Keys.REMINDER_DAY_OF_WEEK] = dayOfWeek.value
        }
    }

    suspend fun setMilestoneNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.MILESTONE_NOTIFICATIONS] = enabled }
    }

    /** null = "automatic" (the most recently updated active goal). */
    suspend fun setWidgetGoalId(goalId: Long?) {
        dataStore.edit { preferences ->
            if (goalId == null) preferences.remove(Keys.WIDGET_GOAL_ID) else preferences[Keys.WIDGET_GOAL_ID] = goalId
        }
    }

    /** Remember the highest milestone (25/50/75/100) already announced for each goal. */
    suspend fun setNotifiedMilestones(milestones: Map<Long, Int>) {
        dataStore.edit { preferences ->
            preferences[Keys.NOTIFIED_MILESTONES] = milestones
                .map { (goalId, percent) -> "$goalId:$percent" }
                .toSet()
        }
    }

    private fun parseMilestones(entries: Set<String>): Map<Long, Int> {
        return entries.mapNotNull { entry ->
            val parts = entry.split(":")
            val goalId = parts.getOrNull(0)?.toLongOrNull()
            val percent = parts.getOrNull(1)?.toIntOrNull()
            if (goalId != null && percent != null) goalId to percent else null
        }.toMap()
    }

    /** Reads an enum saved by name; unknown names fall back to the default. */
    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T {
        return enumValues<T>().firstOrNull { value -> value.name == name } ?: default
    }
}

/** All user settings in one object. Every field has a sensible default. */
data class UserPreferences(
    val onboardingCompleted: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val currency: Currency = Currency.DEFAULT,
    val remindersEnabled: Boolean = false,
    val reminderFrequency: ReminderFrequency = ReminderFrequency.DAILY,
    val reminderHour: Int = 20, // 8 pm
    val reminderMinute: Int = 0,
    val reminderDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
    val milestoneNotificationsEnabled: Boolean = true,
    /** Goal shown on the home-screen widget; null = automatic. */
    val widgetGoalId: Long? = null,
    val notifiedMilestones: Map<Long, Int> = emptyMap(),
)

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}
