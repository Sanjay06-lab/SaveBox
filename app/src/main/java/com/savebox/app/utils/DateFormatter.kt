package com.savebox.app.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Turns dates into readable text, e.g. "12 Mar 2027" or "12 Mar 2027, 4:30 pm".
 * Change the patterns here to change how dates look everywhere in the app.
 */
object DateFormatter {

    private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
    private val dateTimeFormat = DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", Locale.ENGLISH)

    fun formatDate(date: LocalDate): String = date.format(dateFormat)

    /** Formats a timestamp in the phone's own time zone. */
    fun formatDateTime(epochMillis: Long): String {
        val localDateTime = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        return localDateTime.format(dateTimeFormat)
    }

    fun formatDate(epochMillis: Long): String {
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormat)
    }

    /**
     * Material date pickers return the chosen day as UTC milliseconds.
     * This converts that value to a plain LocalDate (no time zone surprises).
     */
    fun pickerMillisToLocalDate(utcMillis: Long): LocalDate {
        return Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    }

    /** The reverse of [pickerMillisToLocalDate], used to pre-select a date in the picker. */
    fun localDateToPickerMillis(date: LocalDate): Long {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

    /** "in 12 days", "due today", "3 days overdue". */
    fun describeDaysLeft(daysLeft: Long): String {
        return when {
            daysLeft > 1 -> "$daysLeft days left"
            daysLeft == 1L -> "1 day left"
            daysLeft == 0L -> "Due today"
            daysLeft == -1L -> "1 day overdue"
            else -> "${-daysLeft} days overdue"
        }
    }
}
