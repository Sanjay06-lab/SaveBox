package com.savebox.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.savebox.app.data.backup.BackupException
import com.savebox.app.data.backup.BackupManager
import com.savebox.app.data.database.SaveBoxDatabase
import com.savebox.app.data.repository.RoomSavingsRepository
import com.savebox.app.domain.model.AddDenominationResult
import com.savebox.app.domain.model.GoalDraft
import com.savebox.app.domain.model.SavingsEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** Backup -> restore must give back exactly the same data, and bad files must change nothing. */
@RunWith(AndroidJUnit4::class)
class BackupManagerTest {

    private lateinit var source: SaveBoxDatabase
    private lateinit var target: SaveBoxDatabase

    private fun newDatabase() = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        SaveBoxDatabase::class.java,
    ).build()

    @Before
    fun setUp() {
        source = newDatabase()
        target = newDatabase()
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    @Test
    fun backupAndRestore_reproducesEverything() = runTest {
        val repository = RoomSavingsRepository(source)
        val goalId = repository.createGoal(
            GoalDraft("PS5", "🎮", "Birthday \"gift\"", 7_000_000, LocalDate.of(2027, 1, 13)),
            startingAmountPaise = 1_000_000,
        )
        repository.addSavings(SavingsEntry(goalId, 25_050, note = "Diwali, from Gran", source = "Gift", savedAtMillis = 123L))
        val rowId = (repository.addDenomination(goalId, 50_000, 110) as AddDenominationResult.Added).denominationId
        repository.toggleBox(rowId, 0)
        repository.toggleBox(rowId, 5)

        val json = BackupManager(source, "test").createBackupJson()

        // Put some OTHER data in the target first, to prove restore replaces it.
        RoomSavingsRepository(target).createGoal(GoalDraft("Old", "🚗", "", 100, null), 0)

        val manager = BackupManager(target, "test")
        manager.restore(manager.parseBackup(json))

        val restored = RoomSavingsRepository(target)
        val goals = restored.observeGoals().first()
        assertEquals(1, goals.size)
        val goal = goals.single()
        assertEquals("PS5", goal.name)
        assertEquals("Birthday \"gift\"", goal.description)
        assertEquals(LocalDate.of(2027, 1, 13), goal.deadline)
        assertEquals(1_000_000L + 25_050L + 2 * 50_000L, goal.savedAmountPaise)

        val rows = restored.observeDenominations(goal.id).first()
        assertEquals(setOf(0, 5), rows.single().markedBoxIndices)
        assertEquals(
            repository.observeAllTransactions().first().map { it.transaction },
            restored.observeAllTransactions().first().map { it.transaction },
        )
    }

    @Test
    fun notABackup_isRejectedWithAClearMessage() = runTest {
        val manager = BackupManager(target, "test")
        for (badText in listOf("", "hello", "{\"some\":\"json\"}", "[1,2,3]")) {
            try {
                manager.parseBackup(badText)
                fail("Should have rejected: $badText")
            } catch (exception: BackupException) {
                assertTrue(exception.message!!.isNotBlank())
            }
        }
    }

    @Test
    fun newerFormatVersion_isRejected() = runTest {
        val json = BackupManager(source, "test").createBackupJson().replace("\"format_version\": 1", "\"format_version\": 99")
        try {
            BackupManager(target, "test").parseBackup(json)
            fail("Should have rejected a newer format")
        } catch (exception: BackupException) {
            assertTrue(exception.message!!.contains("newer version"))
        }
    }

    @Test
    fun brokenLinks_changeNothing() = runTest {
        // Existing data in the target must survive a failed restore.
        RoomSavingsRepository(target).createGoal(GoalDraft("Keep me", "🛟", "", 100, null), 0)

        val repository = RoomSavingsRepository(source)
        val goalId = repository.createGoal(GoalDraft("PS5", "🎮", "", 7_000_000, null), 500)
        val json = BackupManager(source, "test").createBackupJson()
            // Make the saving point at a goal that isn't in the file.
            .replace("\"goal_id\": $goalId", "\"goal_id\": 9999")

        val manager = BackupManager(target, "test")
        try {
            manager.restore(manager.parseBackup(json))
            fail("Should have failed")
        } catch (exception: BackupException) {
            // expected
        }
        assertEquals(listOf("Keep me"), RoomSavingsRepository(target).observeGoals().first().map { it.name })
    }
}
