package com.savebox.app.data.backup

import com.savebox.app.domain.model.TransactionWithGoal
import com.savebox.app.utils.Currency
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Turns the savings history into a CSV file (opens in Excel, Google Sheets, LibreOffice).
 *
 * Columns: Date, Time, Goal, Type, Amount, Source, Note
 * Amounts are plain numbers like 1500.50 (no ₹ or commas) so spreadsheets can add them up.
 */
object CsvExporter {

    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    fun buildCsv(
        entries: List<TransactionWithGoal>,
        currency: Currency,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val lines = mutableListOf(
            listOf("Date", "Time", "Goal", "Type", "Amount (${currency.code})", "Source", "Note").toCsvLine(),
        )
        for (entry in entries) {
            val savedAt = Instant.ofEpochMilli(entry.transaction.savedAtMillis).atZone(zone)
            lines += listOf(
                savedAt.format(dateFormat),
                savedAt.format(timeFormat),
                entry.goalName,
                entry.transaction.type.label,
                plainAmount(entry.transaction.amountPaise),
                entry.transaction.source,
                entry.transaction.note,
            ).toCsvLine()
        }
        // \r\n line endings are what spreadsheet programs expect.
        return lines.joinToString(separator = "\r\n", postfix = "\r\n")
    }

    /** 150050 -> "1500.50" */
    fun plainAmount(amountPaise: Long): String {
        return amountPaise.toBigDecimal().movePointLeft(2).setScale(2).toPlainString()
    }

    /**
     * CSV rules: a value containing a comma, quote or line break is wrapped in quotes,
     * and any quote inside it is doubled. 'He said "hi", ok' -> '"He said ""hi"", ok"'
     */
    fun escape(value: String): String {
        val needsQuotes = value.any { character -> character == ',' || character == '"' || character == '\n' || character == '\r' }
        return if (needsQuotes) "\"" + value.replace("\"", "\"\"") + "\"" else value
    }

    private fun List<String>.toCsvLine(): String = joinToString(",") { value -> escape(value) }
}
