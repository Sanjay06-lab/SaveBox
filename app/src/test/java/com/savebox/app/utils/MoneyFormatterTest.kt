package com.savebox.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for MoneyFormatter. Run them with:  gradlew test
 * (or right-click this file in Android Studio -> Run).
 */
class MoneyFormatterTest {

    @Test
    fun `formats zero`() {
        assertEquals("₹0", MoneyFormatter.format(0))
    }

    @Test
    fun `formats whole rupees without decimals`() {
        assertEquals("₹500", MoneyFormatter.format(50_000))
        assertEquals("₹70,000", MoneyFormatter.format(7_000_000))
    }

    @Test
    fun `uses Indian lakh and crore grouping`() {
        assertEquals("₹1,00,000", MoneyFormatter.format(10_000_000))
        assertEquals("₹12,34,567", MoneyFormatter.format(123_456_700))
        assertEquals("₹1,00,00,000", MoneyFormatter.format(1_000_000_000))
    }

    @Test
    fun `shows paise only when present`() {
        assertEquals("₹1,500.50", MoneyFormatter.format(150_050))
        assertEquals("₹0.05", MoneyFormatter.format(5))
    }

    @Test
    fun `formats negative amounts`() {
        assertEquals("-₹250", MoneyFormatter.format(-25_000))
    }

    @Test
    fun `supports a custom currency symbol`() {
        assertEquals("$1,000", MoneyFormatter.format(100_000, Currency.USD))
        assertEquals("$100,000.50", MoneyFormatter.format(10_000_050, Currency.USD))
        assertEquals("€1,234,567", MoneyFormatter.format(123_456_700, Currency.EUR))
    }

    @Test
    fun `parses valid user input`() {
        assertEquals(150_000L, MoneyFormatter.parseToPaise("1500"))
        assertEquals(150_050L, MoneyFormatter.parseToPaise("1,500.5"))
        assertEquals(25_075L, MoneyFormatter.parseToPaise("₹ 250.75"))
        assertEquals(10L, MoneyFormatter.parseToPaise("0.1"))
        assertEquals(0L, MoneyFormatter.parseToPaise("0"))
    }

    @Test
    fun `rejects invalid user input`() {
        assertNull(MoneyFormatter.parseToPaise(""))
        assertNull(MoneyFormatter.parseToPaise("abc"))
        assertNull(MoneyFormatter.parseToPaise("12.345"))
        assertNull(MoneyFormatter.parseToPaise("1.2.3"))
        assertNull(MoneyFormatter.parseToPaise("99999999999999999999"))
    }

    @Test
    fun `compact labels for denomination boxes`() {
        assertEquals("500", MoneyFormatter.compact(50_000))
        assertEquals("2K", MoneyFormatter.compact(200_000))
        assertEquals("2.5K", MoneyFormatter.compact(250_000))
        assertEquals("1L", MoneyFormatter.compact(10_000_000))
        assertEquals("0.5", MoneyFormatter.compact(50))
        assertEquals("10", MoneyFormatter.compact(1_000))
    }

    @Test
    fun `converts paise back to input text`() {
        assertEquals("1500", MoneyFormatter.paiseToInputText(150_000))
        assertEquals("1500.50", MoneyFormatter.paiseToInputText(150_050))
    }
}
