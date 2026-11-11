package com.savebox.app.data.database

import androidx.room.migration.Migration

/**
 * Database migrations: instructions to upgrade an existing user's database
 * from one version to the next WITHOUT losing their savings.
 *
 * Version 1 is the first release, so there is nothing to migrate yet.
 *
 * EXAMPLE - adding a "colour" column to goals in version 2:
 *
 *     val MIGRATION_1_2 = object : Migration(1, 2) {
 *         override fun migrate(db: SupportSQLiteDatabase) {
 *             db.execSQL("ALTER TABLE savings_goals ADD COLUMN colour TEXT NOT NULL DEFAULT ''")
 *         }
 *     }
 *
 * Then add it below:  val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
 * and test it with MigrationTestHelper (see DEVELOPMENT_GUIDE.md).
 */
object SaveBoxMigrations {

    /** Every migration, oldest first. Passed to Room in SaveBoxDatabase.create(). */
    val ALL: Array<Migration> = arrayOf()
}
