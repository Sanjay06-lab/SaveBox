package com.savebox.app.domain.reminders

/** How often savings reminders are sent. Saved in settings by name - don't rename entries. */
enum class ReminderFrequency(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
}
