package com.savebox.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.savebox.app.data.database.SaveBoxDatabase
import com.savebox.app.data.repository.RoomSavingsRepository
import com.savebox.app.domain.model.GoalDraft
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Proves that data is written to a real FILE and is still there after the database
 * is closed and opened again - just like closing and reopening the app.
 *
 * Uses its own file name so it never touches the app's real data.
 */
@RunWith(AndroidJUnit4::class)
class DatabasePersistenceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val testDatabaseName = "savebox-persistence-test.db"

    @Before
    @After
    fun deleteTestDatabase() {
        context.deleteDatabase(testDatabaseName)
    }

    private fun openDatabase(): SaveBoxDatabase {
        return Room.databaseBuilder(context, SaveBoxDatabase::class.java, testDatabaseName).build()
    }

    @Test
    fun goalSurvivesClosingAndReopeningTheDatabase() = runTest {
        // 1. Open, save a goal with a starting amount, close.
        val firstSession = openDatabase()
        val goalId = RoomSavingsRepository(firstSession).createGoal(
            draft = GoalDraft(name = "Travel", emoji = "✈️", description = "", targetAmountPaise = 2_500_000, deadline = null),
            startingAmountPaise = 300_000,
        )
        firstSession.close()

        // 2. Open the same file again and read it back.
        val secondSession = openDatabase()
        val goal = RoomSavingsRepository(secondSession).getGoal(goalId)
        secondSession.close()

        assertEquals("Travel", goal?.name)
        assertEquals(2_500_000L, goal?.targetAmountPaise)
        assertEquals(300_000L, goal?.savedAmountPaise)
    }
}
