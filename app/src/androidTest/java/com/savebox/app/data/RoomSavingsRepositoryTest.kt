package com.savebox.app.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.savebox.app.data.database.SaveBoxDatabase
import com.savebox.app.data.entities.DenominationEntity
import com.savebox.app.data.entities.DenominationEntryEntity
import com.savebox.app.data.entities.SavingsTransactionEntity
import com.savebox.app.data.repository.RoomSavingsRepository
import com.savebox.app.domain.model.GoalDraft
import com.savebox.app.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Tests the real Room database (in memory, so each test starts empty).
 *
 * These are INSTRUMENTED tests: they run on a phone or emulator.
 * Run with:  gradlew connectedDebugAndroidTest
 * or right-click this file in Android Studio -> Run.
 */
@RunWith(AndroidJUnit4::class)
class RoomSavingsRepositoryTest {

    private lateinit var database: SaveBoxDatabase
    private lateinit var repository: RoomSavingsRepository
    private var fakeNow = 1_000L

    private val laptopDraft = GoalDraft(
        name = "Laptop",
        emoji = "💻",
        description = "For college",
        targetAmountPaise = 6_000_000,
        deadline = LocalDate.of(2027, 3, 31),
    )

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SaveBoxDatabase::class.java,
        ).build()
        repository = RoomSavingsRepository(database, currentTimeMillis = { fakeNow })
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun saveAndReadGoal_allFieldsSurvive() = runTest {
        val goalId = repository.createGoal(laptopDraft, startingAmountPaise = 0)

        val goal = repository.getGoal(goalId)!!
        assertEquals("Laptop", goal.name)
        assertEquals("💻", goal.emoji)
        assertEquals("For college", goal.description)
        assertEquals(6_000_000L, goal.targetAmountPaise)
        assertEquals(LocalDate.of(2027, 3, 31), goal.deadline)
        assertEquals(0L, goal.savedAmountPaise)
        assertEquals(1_000L, goal.createdAtMillis)
    }

    @Test
    fun goalWithoutDeadline_readsBackAsNull() = runTest {
        val goalId = repository.createGoal(laptopDraft.copy(deadline = null), startingAmountPaise = 0)
        assertNull(repository.getGoal(goalId)!!.deadline)
    }

    @Test
    fun startingAmount_isStoredAsTransaction_andCountsTowardsTotal() = runTest {
        val goalId = repository.createGoal(laptopDraft, startingAmountPaise = 500_000)

        val transactions = repository.observeTransactionsForGoal(goalId).first()
        assertEquals(1, transactions.size)
        assertEquals(TransactionType.STARTING_AMOUNT, transactions.single().type)
        assertEquals(500_000L, repository.getGoal(goalId)!!.savedAmountPaise)
    }

    @Test
    fun savedTotal_isSumOfTransactions() = runTest {
        val goalId = repository.createGoal(laptopDraft, startingAmountPaise = 100_000)
        insertTransaction(goalId, amountPaise = 25_050)
        insertTransaction(goalId, amountPaise = 50_000)

        assertEquals(175_050L, repository.getGoal(goalId)!!.savedAmountPaise)
    }

    @Test
    fun observeGoals_newestFirst_withIndependentTotals() = runTest {
        val firstId = repository.createGoal(laptopDraft, startingAmountPaise = 100_000)
        fakeNow = 2_000L
        val secondId = repository.createGoal(laptopDraft.copy(name = "Bike"), startingAmountPaise = 0)

        val goals = repository.observeGoals().first()
        assertEquals(listOf(secondId, firstId), goals.map { goal -> goal.id })
        assertEquals(0L, goals[0].savedAmountPaise)
        assertEquals(100_000L, goals[1].savedAmountPaise)
    }

    @Test
    fun updateGoal_changesDetails_butKeepsSavingsAndCreationTime() = runTest {
        val goalId = repository.createGoal(laptopDraft, startingAmountPaise = 500_000)
        fakeNow = 5_000L

        repository.updateGoal(goalId, laptopDraft.copy(name = "Gaming laptop", targetAmountPaise = 9_000_000, deadline = null))

        val goal = repository.getGoal(goalId)!!
        assertEquals("Gaming laptop", goal.name)
        assertEquals(9_000_000L, goal.targetAmountPaise)
        assertNull(goal.deadline)
        assertEquals(500_000L, goal.savedAmountPaise)
        assertEquals(1_000L, goal.createdAtMillis)
        assertEquals(5_000L, goal.lastActivityAtMillis)
    }

    @Test
    fun deleteGoal_cascadesToTransactionsDenominationsAndBoxes() = runTest {
        val goalId = repository.createGoal(laptopDraft, startingAmountPaise = 500_000)
        val otherGoalId = repository.createGoal(laptopDraft.copy(name = "Bike"), startingAmountPaise = 100_000)
        val denominationId = database.denominationDao().insertDenomination(
            DenominationEntity(goalId = goalId, valuePaise = 50_000, quantity = 10, createdAtMillis = fakeNow),
        )
        val boxTransactionId = insertTransaction(goalId, amountPaise = 50_000, type = TransactionType.DENOMINATION)
        database.denominationDao().insertEntry(
            DenominationEntryEntity(denominationId = denominationId, boxIndex = 0, transactionId = boxTransactionId, markedAtMillis = fakeNow),
        )

        repository.deleteGoal(goalId)

        assertNull(repository.getGoal(goalId))
        assertEquals(0, database.transactionDao().countForGoal(goalId))
        assertEquals(0, database.denominationDao().countDenominationsForGoal(goalId))
        assertEquals(0, database.denominationDao().countEntriesForDenomination(denominationId))
        // The other goal is untouched.
        assertEquals(100_000L, repository.getGoal(otherGoalId)!!.savedAmountPaise)
    }

    @Test
    fun sameBoxCannotBeTickedTwice() = runTest {
        val goalId = repository.createGoal(laptopDraft, startingAmountPaise = 0)
        val denominationId = database.denominationDao().insertDenomination(
            DenominationEntity(goalId = goalId, valuePaise = 50_000, quantity = 10, createdAtMillis = fakeNow),
        )
        val firstTransaction = insertTransaction(goalId, 50_000, TransactionType.DENOMINATION)
        val secondTransaction = insertTransaction(goalId, 50_000, TransactionType.DENOMINATION)
        database.denominationDao().insertEntry(DenominationEntryEntity(denominationId = denominationId, boxIndex = 3, transactionId = firstTransaction, markedAtMillis = 0))

        val duplicateRejected = try {
            database.denominationDao().insertEntry(DenominationEntryEntity(denominationId = denominationId, boxIndex = 3, transactionId = secondTransaction, markedAtMillis = 0))
            false
        } catch (exception: SQLiteConstraintException) {
            true
        }
        assertTrue("A second tick on the same box must be rejected", duplicateRejected)
    }

    @Test
    fun sameDenominationValueCannotBeAddedTwiceToOneGoal() = runTest {
        val goalId = repository.createGoal(laptopDraft, startingAmountPaise = 0)
        val fiveHundred = DenominationEntity(goalId = goalId, valuePaise = 50_000, quantity = 10, createdAtMillis = 0)
        database.denominationDao().insertDenomination(fiveHundred)

        val duplicateRejected = try {
            database.denominationDao().insertDenomination(fiveHundred.copy(quantity = 5))
            false
        } catch (exception: SQLiteConstraintException) {
            true
        }
        assertTrue(duplicateRejected)
    }

    @Test
    fun transactionForMissingGoal_isRejected() = runTest {
        val rejected = try {
            insertTransaction(goalId = 999, amountPaise = 100)
            false
        } catch (exception: SQLiteConstraintException) {
            true
        }
        assertTrue("Foreign keys must stop savings that belong to no goal", rejected)
    }

    @Test
    fun unknownTransactionType_isReadAsCustom() = runTest {
        val goalId = repository.createGoal(laptopDraft, startingAmountPaise = 0)
        database.transactionDao().insert(
            SavingsTransactionEntity(goalId = goalId, amountPaise = 100, type = "SOMETHING_NEW", note = "", source = "", savedAtMillis = 0, createdAtMillis = 0),
        )
        val transaction = repository.observeTransactionsForGoal(goalId).first().single()
        assertEquals(TransactionType.CUSTOM, transaction.type)
    }

    private suspend fun insertTransaction(
        goalId: Long,
        amountPaise: Long,
        type: TransactionType = TransactionType.CUSTOM,
    ): Long {
        return database.transactionDao().insert(
            SavingsTransactionEntity(
                goalId = goalId,
                amountPaise = amountPaise,
                type = type.name,
                note = "",
                source = "",
                savedAtMillis = fakeNow,
                createdAtMillis = fakeNow,
            ),
        )
    }
}
