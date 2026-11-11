package com.savebox.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.savebox.app.data.database.SaveBoxDatabase
import com.savebox.app.data.repository.RoomSavingsRepository
import com.savebox.app.domain.model.AddDenominationResult
import com.savebox.app.domain.model.BoxToggleResult
import com.savebox.app.domain.model.GoalDraft
import com.savebox.app.domain.model.SavingsEntry
import com.savebox.app.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 4 rules, tested against the real database:
 * adding, editing, deleting and restoring savings, and ticking/unticking boxes.
 * After every change we check that the goal's total is still correct.
 */
@RunWith(AndroidJUnit4::class)
class SavingsOperationsTest {

    private lateinit var database: SaveBoxDatabase
    private lateinit var repository: RoomSavingsRepository
    private var goalId = 0L

    @Before
    fun setUp() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SaveBoxDatabase::class.java,
        ).build()
        repository = RoomSavingsRepository(database, currentTimeMillis = { 1_000L })
        goalId = repository.createGoal(
            GoalDraft(name = "PS5", emoji = "🎮", description = "", targetAmountPaise = 7_000_000, deadline = null),
            startingAmountPaise = 0,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun saved(): Long = repository.getGoal(goalId)!!.savedAmountPaise

    private fun entry(amountPaise: Long, note: String = "") = SavingsEntry(
        goalId = goalId,
        amountPaise = amountPaise,
        note = note,
        source = "Pocket money",
        savedAtMillis = 500L,
    )

    // ----- Custom savings -----

    @Test
    fun addSavings_increasesTotal_andAppearsInHistoryWithGoalName() = runTest {
        repository.addSavings(entry(25_000))
        repository.addSavings(entry(15_050))

        assertEquals(40_050L, saved())
        val history = repository.observeAllTransactions().first()
        assertEquals(2, history.size)
        assertTrue(history.all { it.goalName == "PS5" && it.goalEmoji == "🎮" })
        assertEquals("Pocket money", history.first().transaction.source)
    }

    @Test
    fun editSavings_changesTotalByTheDifference() = runTest {
        val id = repository.addSavings(entry(25_000))
        repository.addSavings(entry(10_000))

        assertTrue(repository.updateSavings(id, entry(40_000, note = "Corrected")))

        assertEquals(50_000L, saved())
        assertEquals("Corrected", repository.getTransaction(id)!!.note)
    }

    @Test
    fun editSavings_canMoveEntryToAnotherGoal() = runTest {
        val otherGoalId = repository.createGoal(
            GoalDraft("Bike", "🚲", "", 1_000_000, null),
            startingAmountPaise = 0,
        )
        val id = repository.addSavings(entry(25_000))

        repository.updateSavings(id, entry(25_000).copy(goalId = otherGoalId))

        assertEquals(0L, saved())
        assertEquals(25_000L, repository.getGoal(otherGoalId)!!.savedAmountPaise)
    }

    @Test
    fun deleteThenUndo_restoresTotalAndOriginalRecord() = runTest {
        val id = repository.addSavings(entry(25_000, note = "Birthday"))
        repository.addSavings(entry(5_000))

        val deleted = repository.deleteTransaction(id)
        assertNotNull(deleted)
        assertEquals(5_000L, saved())

        assertTrue(repository.restoreTransaction(deleted!!))
        assertEquals(30_000L, saved())
        val restored = repository.getTransaction(id)!!
        assertEquals("Birthday", restored.note)
        assertEquals(500L, restored.savedAtMillis)
    }

    @Test
    fun restoreAfterGoalDeleted_doesNothing() = runTest {
        val id = repository.addSavings(entry(25_000))
        val deleted = repository.deleteTransaction(id)!!
        repository.deleteGoal(goalId)

        assertFalse(repository.restoreTransaction(deleted))
        assertTrue(repository.observeAllTransactions().first().isEmpty())
    }

    @Test
    fun editOrDeleteMissingEntry_isHandledSafely() = runTest {
        assertFalse(repository.updateSavings(999, entry(100)))
        assertNull(repository.deleteTransaction(999))
    }

    // ----- Denomination grid -----

    private suspend fun addRow(valueRupees: Long, quantity: Int): Long {
        val result = repository.addDenomination(goalId, valueRupees * 100, quantity)
        return (result as AddDenominationResult.Added).denominationId
    }

    @Test
    fun ps5Grid_tickAndUntick_keepsTotalsCorrect() = runTest {
        val fiveHundred = addRow(500, 110)
        addRow(200, 60)
        addRow(100, 30)

        // Tick 3 × ₹500
        repeat(3) { boxIndex ->
            assertTrue(repository.toggleBox(fiveHundred, boxIndex) is BoxToggleResult.Marked)
        }
        assertEquals(150_000L, saved())

        // Untick the middle one
        val result = repository.toggleBox(fiveHundred, 1)
        assertTrue(result is BoxToggleResult.Unmarked)
        assertEquals(100_000L, saved())

        val rows = repository.observeDenominations(goalId).first()
        assertEquals(listOf(50_000L, 20_000L, 10_000L), rows.map { it.valuePaise }) // largest first
        assertEquals(setOf(0, 2), rows.first().markedBoxIndices)

        // Every ticked box has exactly one DENOMINATION transaction.
        val history = repository.observeTransactionsForGoal(goalId).first()
        assertEquals(2, history.count { it.type == TransactionType.DENOMINATION })
    }

    @Test
    fun untickThenUndo_ticksTheSameBoxAgain() = runTest {
        val row = addRow(500, 10)
        repository.toggleBox(row, 4)
        val unmarked = repository.toggleBox(row, 4) as BoxToggleResult.Unmarked

        assertTrue(repository.restoreTransaction(unmarked.deleted))

        assertEquals(setOf(4), repository.observeDenominations(goalId).first().single().markedBoxIndices)
        assertEquals(50_000L, saved())
    }

    @Test
    fun deletingBoxSavingFromHistory_unticksTheBox() = runTest {
        val row = addRow(200, 5)
        val marked = repository.toggleBox(row, 0) as BoxToggleResult.Marked

        repository.deleteTransaction(marked.transactionId)

        assertTrue(repository.observeDenominations(goalId).first().single().markedBoxIndices.isEmpty())
        assertEquals(0L, saved())
    }

    @Test
    fun editingBoxSaving_keepsAmountAndGoal_butUpdatesNote() = runTest {
        val row = addRow(500, 5)
        val marked = repository.toggleBox(row, 0) as BoxToggleResult.Marked

        repository.updateSavings(marked.transactionId, entry(999_999, note = "From grandma"))

        val transaction = repository.getTransaction(marked.transactionId)!!
        assertEquals(50_000L, transaction.amountPaise)
        assertEquals("From grandma", transaction.note)
        assertEquals(50_000L, saved())
    }

    @Test
    fun boxOutsideRow_isRejected() = runTest {
        val row = addRow(100, 3)
        assertEquals(BoxToggleResult.NotFound, repository.toggleBox(row, 3))
        assertEquals(BoxToggleResult.NotFound, repository.toggleBox(row, -1))
        assertEquals(BoxToggleResult.NotFound, repository.toggleBox(9_999, 0))
    }

    @Test
    fun duplicateDenominationValue_isRefused() = runTest {
        addRow(500, 10)
        assertEquals(AddDenominationResult.DuplicateValue, repository.addDenomination(goalId, 50_000, 5))
    }

    @Test
    fun reducingQuantityBelowATickedBox_isRefused() = runTest {
        val row = addRow(100, 10)
        repository.toggleBox(row, 7)

        assertFalse(repository.updateDenominationQuantity(row, 7)) // would remove box #8
        assertTrue(repository.updateDenominationQuantity(row, 8))
        assertTrue(repository.updateDenominationQuantity(row, 20))
        assertEquals(20, repository.observeDenominations(goalId).first().single().quantity)
    }

    @Test
    fun deletingARow_removesItsSavingsButNotOthers() = runTest {
        val fiveHundred = addRow(500, 10)
        val hundred = addRow(100, 10)
        repository.toggleBox(fiveHundred, 0)
        repository.toggleBox(fiveHundred, 1)
        repository.toggleBox(hundred, 0)
        repository.addSavings(entry(25_000))

        repository.deleteDenomination(fiveHundred)

        assertEquals(35_000L, saved()) // ₹100 box + ₹250 custom
        assertEquals(1, repository.observeDenominations(goalId).first().size)
    }
}
