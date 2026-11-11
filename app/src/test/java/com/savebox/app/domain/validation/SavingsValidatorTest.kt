package com.savebox.app.domain.validation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SavingsValidatorTest {

    private val today = LocalDate.of(2026, 11, 11)

    private fun validate(
        goalId: Long? = 1,
        amount: String = "250",
        note: String = "",
        source: String = "",
        date: LocalDate? = today,
    ) = SavingsValidator.validateEntry(goalId, amount, note, source, date, today)

    @Test
    fun `valid entry`() {
        assertFalse(validate().hasErrors)
        assertFalse(validate(amount = "0.50", date = today.minusYears(1)).hasErrors)
    }

    @Test
    fun `goal is required`() {
        assertNotNull(validate(goalId = null).goalError)
    }

    @Test
    fun `amount must be positive and valid`() {
        assertNotNull(validate(amount = "").amountError)
        assertNotNull(validate(amount = "0").amountError)
        assertNotNull(validate(amount = "abc").amountError)
        assertNotNull(validate(amount = "1.234").amountError)
        assertNotNull(validate(amount = "9999999999").amountError) // above ₹10 crore
    }

    @Test
    fun `date can't be in the future`() {
        assertNotNull(validate(date = today.plusDays(1)).dateError)
        assertNotNull(validate(date = null).dateError)
    }

    @Test
    fun `note and source have length limits`() {
        assertNotNull(validate(note = "x".repeat(SavingsValidator.MAX_NOTE_LENGTH + 1)).noteError)
        assertNotNull(validate(source = "x".repeat(SavingsValidator.MAX_SOURCE_LENGTH + 1)).sourceError)
    }

    @Test
    fun `denomination value and quantity`() {
        assertFalse(SavingsValidator.validateDenomination("500", "110").hasErrors)
        assertNotNull(SavingsValidator.validateDenomination("", "10").valueError)
        assertNotNull(SavingsValidator.validateDenomination("0", "10").valueError)
        assertNotNull(SavingsValidator.validateDenomination("200000", "10").valueError) // above ₹1,00,000
        assertNotNull(SavingsValidator.validateDenomination("500", "").quantityError)
        assertNotNull(SavingsValidator.validateDenomination("500", "0").quantityError)
        assertNotNull(SavingsValidator.validateDenomination("500", "1001").quantityError)
        assertNull(SavingsValidator.validateDenomination("500", "1000").quantityError)
    }
}
