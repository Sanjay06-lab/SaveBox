package com.savebox.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.savebox.app.data.dao.BackupDao
import com.savebox.app.data.dao.DenominationDao
import com.savebox.app.data.dao.GoalDao
import com.savebox.app.data.dao.TransactionDao
import com.savebox.app.data.entities.DenominationEntity
import com.savebox.app.data.entities.DenominationEntryEntity
import com.savebox.app.data.entities.SavingsGoalEntity
import com.savebox.app.data.entities.SavingsTransactionEntity

/**
 * The SaveBox database (a single SQLite file, "savebox.db", private to the app).
 *
 * Tables:
 *   savings_goals          1 ──< savings_transactions
 *   savings_goals          1 ──< denominations 1 ──< denomination_entries >── 1 savings_transactions
 *
 * HOW TO CHANGE THE DATABASE SAFELY (e.g. add a column):
 *   1. Change the entity class.
 *   2. Increase `version` below (1 -> 2).
 *   3. Write a Migration in Migrations.kt and add it to SaveBoxMigrations.ALL.
 *   4. Build: Room writes app/schemas/.../2.json. Commit it to git.
 * Never use fallbackToDestructiveMigration(): it would DELETE users' savings on update.
 */
@Database(
    entities = [
        SavingsGoalEntity::class,
        SavingsTransactionEntity::class,
        DenominationEntity::class,
        DenominationEntryEntity::class,
    ],
    version = 1,
    exportSchema = true, // writes the schema JSON to app/schemas (configured in app/build.gradle.kts)
)
abstract class SaveBoxDatabase : RoomDatabase() {

    abstract fun goalDao(): GoalDao
    abstract fun transactionDao(): TransactionDao
    abstract fun denominationDao(): DenominationDao
    abstract fun backupDao(): BackupDao

    companion object {
        const val DATABASE_NAME = "savebox.db"

        /**
         * Opens (or creates) the database file. Call this ONCE - AppContainer does it -
         * because opening several copies of the same database is slow and unsafe.
         */
        fun create(context: Context): SaveBoxDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SaveBoxDatabase::class.java,
                DATABASE_NAME,
            )
                .addMigrations(*SaveBoxMigrations.ALL)
                .build()
        }
    }
}
