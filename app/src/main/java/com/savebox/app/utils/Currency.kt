package com.savebox.app.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Currencies the user can choose in Settings.
 *
 * Amounts are always stored in the currency's smallest unit (paise for ₹, cents for $ ...),
 * so changing the currency only changes how numbers are SHOWN - no amounts are converted.
 *
 * To add a currency, add an entry here. Never rename an existing `code`: it is saved in settings.
 */
enum class Currency(
    val code: String,
    val symbol: String,
    val displayName: String,
    /** true = 1,00,000 (Indian lakh/crore grouping), false = 100,000 */
    val usesIndianGrouping: Boolean,
) {
    INR(code = "INR", symbol = "₹", displayName = "Indian Rupee", usesIndianGrouping = true),
    USD(code = "USD", symbol = "$", displayName = "US Dollar", usesIndianGrouping = false),
    EUR(code = "EUR", symbol = "€", displayName = "Euro", usesIndianGrouping = false),
    GBP(code = "GBP", symbol = "£", displayName = "British Pound", usesIndianGrouping = false),
    AED(code = "AED", symbol = "AED ", displayName = "UAE Dirham", usesIndianGrouping = false),
    ;

    companion object {
        val DEFAULT = INR

        /** Reads a saved code safely; unknown or missing codes fall back to ₹. */
        fun fromCode(code: String?): Currency = entries.firstOrNull { currency -> currency.code == code } ?: DEFAULT
    }
}

/**
 * The currency currently chosen in Settings, readable from anywhere.
 *
 * It is stored in Compose "state", so every screen that shows money redraws automatically
 * when the user changes the currency. It is set at app start and whenever the setting
 * changes (see AppDataObserver).
 */
object AppCurrency {
    var current: Currency by mutableStateOf(Currency.DEFAULT)
}
