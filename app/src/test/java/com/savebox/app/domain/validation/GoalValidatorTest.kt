package com.savebox.app.domain.validation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalValidatorTest {

    private val today = LocalDate.of(2026, 6, 1)

    private fun validate(
        name: String = "PlayStation 5",
        target: String = "70000",
        starting: String = "",
        description: String = "",
        deadline: LocalDate? = null,
        isNewGoal: Boolean = true,
    ) = GoalValidator.validate(name, target, starting, description, deadline, today, isNewGoal)

    @Test
    fun `valid goal has no errors`() {
        assertFalse(validate().hasErrors)
        assertFalse(validate(starting = "5000", deadline = today.plusMonths(6)).hasErrors)
    }

    @Test
    fun `name is required and limited`() {
        assertNotNull(validate(name = "   ").nameError)
        assertNotNull(validate(name = "x".repeat(GoalValidator.MAX_NAME_LENGTH + 1)).nameError)
    }

    @Test
    fun `target must be a positive valid amount`() {
        assertNotNull(validate(target = "").targetAmountError)
        assertNotNull(validate(target = "0").targetAmountError)
        assertNotNull(validate(target = "abc").targetAmountError)
        assertNotNull(validate(target = "1000000000").targetAmountError) // above ₹10 crore
        assertNull(validate(target = "99.50").targetAmountError)
    }

    @Test
    fun `starting amount is optional but must be valid`() {
        assertNull(validate(starting = "").startingAmountError)
        assertNull(validate(starting = "0").startingAmountError)
        assertNotNull(validate(starting = "12.345").startingAmountError)
    }

    @Test
    fun `starting amount above target is allowed`() {
        // Extra savings are kept, so this is not an error.
        assertNull(validate(target = "1000", starting = "1500").startingAmountError)
    }

    @Test
    fun `new goals cannot have a past deadline`() {
        assertNotNull(validate(deadline = today.minusDays(1)).deadlineError)
        assertNull(validate(deadline = today).deadlineError)
    }

    @Test
    fun `edited goals may keep an old deadline`() {
        assertNull(validate(deadline = today.minusDays(30), isNewGoal = false).deadlineError)
    }

    @Test
    fun `long description is rejected`() {
        assertTrue(validate(description = "a".repeat(GoalValidator.MAX_DESCRIPTION_LENGTH + 1)).hasErrors)
    }
}
