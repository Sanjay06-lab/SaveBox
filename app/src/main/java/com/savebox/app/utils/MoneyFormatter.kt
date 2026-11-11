package com.savebox.app.utils

import kotlin.math.abs

/**
 * Converts money between how we STORE it and how we SHOW it.
 *
 * WHY PAISE?
 * Computers cannot store numbers like 0.1 exactly as Double/Float, so adding
 * 0.1 + 0.2 gives 0.30000000000000004. For money this causes wrong totals.
 * SaveBox therefore stores every amount as a whole number of paise in a Long:
 *     ₹1      = 100 paise
 *     ₹70,000 = 7_000_000 paise
 * Whole numbers add and subtract exactly. We only convert to "₹70,000" for display.
 *
 * We format the number ourselves (instead of java.text.NumberFormat) so the
 * Indian grouping (1,00,000) is identical on every phone and in unit tests.
 *
 * Other currencies (chosen in Settings) work the same way: "paise" then simply means
 * the smallest unit (cents for $), and grouping becomes 100,000.
 */
object MoneyFormatter {

    const val PAISE_PER_RUPEE = 100L

    /**
     * Format paise for display.
     *
     *   format(7_000_000)                 -> "₹70,000"
     *   format(10_000_050)                -> "₹1,00,000.50"
     *   format(-25_000)                   -> "-₹250"
     *   format(10_000_000, Currency.USD)  -> "$100,000"
     *
     * Paise are shown only when the amount is not a whole number of rupees.
     * @param currency defaults to the currency chosen in Settings.
     */
    fun format(amountInPaise: Long, currency: Currency = AppCurrency.current): String {
        val isNegative = amountInPaise < 0
        val absolutePaise = abs(amountInPaise)

        val rupees = absolutePaise / PAISE_PER_RUPEE
        val paise = absolutePaise % PAISE_PER_RUPEE

        val rupeesText = if (currency.usesIndianGrouping) groupIndian(rupees) else groupInternational(rupees)
        val paiseText = if (paise == 0L) "" else "." + paise.toString().padStart(2, '0')
        val sign = if (isNegative) "-" else ""

        return "$sign${currency.symbol}$rupeesText$paiseText"
    }

    /** Groups of 3 digits: 1234567 -> "1,234,567" */
    fun groupInternational(number: Long): String {
        return number.toString()
            .reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()
    }

    /**
     * Inserts commas the Indian way: the last 3 digits form one group,
     * then every 2 digits form a group.  1234567 -> "12,34,567"
     */
    fun groupIndian(number: Long): String {
        val digits = number.toString()
        if (digits.length <= 3) return digits

        val lastThree = digits.takeLast(3)
        var remaining = digits.dropLast(3)
        val groups = mutableListOf<String>()

        // Take 2 digits at a time from the right.
        while (remaining.length > 2) {
            groups.add(0, remaining.takeLast(2))
            remaining = remaining.dropLast(2)
        }
        if (remaining.isNotEmpty()) {
            groups.add(0, remaining)
        }

        return groups.joinToString(",") + "," + lastThree
    }

    /**
     * Turn what the user typed into paise. Returns null if the text is not a valid amount.
     *
     *   parseToPaise("1500")      -> 150000
     *   parseToPaise("1,500.5")   -> 150050
     *   parseToPaise("₹ 250.75")  -> 25075
     *   parseToPaise("12.345")    -> null  (more than 2 decimal places)
     *   parseToPaise("abc")       -> null
     *
     * This function does not decide whether zero or negative values are allowed:
     * screens check that separately so they can show a clear error message.
     */
    fun parseToPaise(input: String): Long? {
        val cleaned = input
            .replace(AppCurrency.current.symbol.trim(), "")
            .replace("₹", "")
            .replace(",", "")
            .trim()

        // Optional minus, digits, optional "." followed by 1-2 digits.
        val validAmount = Regex("""^-?\d+(\.\d{1,2})?$""")
        if (!validAmount.matches(cleaned)) return null

        // BigDecimal does exact decimal maths, so "0.1" stays exactly 0.1.
        return try {
            cleaned.toBigDecimal()
                .movePointRight(2)
                .longValueExact()
        } catch (exception: ArithmeticException) {
            null // number too large to fit in a Long
        }
    }

    /**
     * Very short label for small spaces such as a denomination box (no ₹ symbol).
     *   compact(50_000)     -> "500"
     *   compact(200_000)    -> "2K"
     *   compact(250_000)    -> "2.5K"
     *   compact(10_000_000) -> "1L"     (1 lakh)
     *   compact(50)         -> "0.5"
     */
    fun compact(amountInPaise: Long): String {
        val rupees = amountInPaise.toBigDecimal().movePointLeft(2)
        val (value, suffix) = when {
            rupees >= 1_00_000.toBigDecimal() -> rupees.movePointLeft(5) to "L"
            rupees >= 1_000.toBigDecimal() -> rupees.movePointLeft(3) to "K"
            else -> rupees to ""
        }
        // Keep at most one decimal place and drop trailing zeros: 2.50 -> 2.5, 3.0 -> 3
        val rounded = value.setScale(1, java.math.RoundingMode.DOWN).stripTrailingZeros().toPlainString()
        return rounded + suffix
    }

    /** Rupees (whole number) to paise. 500 -> 50000 */
    fun rupeesToPaise(rupees: Long): Long = rupees * PAISE_PER_RUPEE

    /**
     * Paise to the plain text shown inside an input field when editing.
     * 150050 -> "1500.50", 150000 -> "1500"
     */
    fun paiseToInputText(amountInPaise: Long): String {
        val rupees = amountInPaise / PAISE_PER_RUPEE
        val paise = abs(amountInPaise % PAISE_PER_RUPEE)
        return if (paise == 0L) rupees.toString() else "$rupees." + paise.toString().padStart(2, '0')
    }
}
