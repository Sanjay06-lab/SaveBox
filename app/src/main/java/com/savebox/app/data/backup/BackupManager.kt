package com.savebox.app.data.backup

import android.database.SQLException
import androidx.room.withTransaction
import com.savebox.app.data.database.SaveBoxDatabase
import com.savebox.app.data.entities.DenominationEntity
import com.savebox.app.data.entities.DenominationEntryEntity
import com.savebox.app.data.entities.SavingsGoalEntity
import com.savebox.app.data.entities.SavingsTransactionEntity
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Creates and restores backup files.
 *
 * - A backup is ONE JSON file containing every goal, saving and box.
 * - The user chooses where to save it (phone storage, Google Drive app, a USB stick...).
 *   SaveBox itself never uploads anything.
 * - Restoring REPLACES all current data, inside one database transaction:
 *   if anything in the file is wrong, nothing changes.
 *
 * This class only converts between JSON text and the database. Reading/writing the
 * actual file is done in SettingsViewModel with Android's file picker.
 */
class BackupManager(
    private val database: SaveBoxDatabase,
    private val appVersion: String,
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() },
) {

    private val backupDao = database.backupDao()

    private val json = Json {
        prettyPrint = true          // readable if someone opens the file
        ignoreUnknownKeys = true    // files from newer versions with extra fields still load
        encodeDefaults = true
    }

    /** All data as JSON text. */
    suspend fun createBackupJson(): String {
        val backup = database.withTransaction {
            BackupFile(
                formatVersion = CURRENT_FORMAT_VERSION,
                appVersion = appVersion,
                exportedAtMillis = currentTimeMillis(),
                goals = backupDao.allGoals().map { it.toBackup() },
                transactions = backupDao.allTransactions().map { it.toBackup() },
                denominations = backupDao.allDenominations().map { it.toBackup() },
                denominationEntries = backupDao.allDenominationEntries().map { it.toBackup() },
            )
        }
        return json.encodeToString(BackupFile.serializer(), backup)
    }

    /**
     * Reads and checks a backup WITHOUT changing anything.
     * @throws BackupException with a message that can be shown to the user.
     */
    fun parseBackup(text: String): BackupFile {
        val backup = try {
            json.decodeFromString(BackupFile.serializer(), text)
        } catch (exception: SerializationException) {
            throw BackupException("This file is not a SaveBox backup.", exception)
        } catch (exception: IllegalArgumentException) {
            throw BackupException("This file is not a SaveBox backup.", exception)
        }

        if (backup.app != "SaveBox") throw BackupException("This file is not a SaveBox backup.")
        if (backup.formatVersion > CURRENT_FORMAT_VERSION) {
            throw BackupException("This backup was made by a newer version of SaveBox. Please update the app first.")
        }
        validate(backup)
        return backup
    }

    fun summarize(backup: BackupFile): BackupSummary {
        return BackupSummary(
            exportedAtMillis = backup.exportedAtMillis,
            goalCount = backup.goals.size,
            transactionCount = backup.transactions.size,
        )
    }

    /**
     * Replaces ALL current data with the backup's data. All or nothing.
     * @throws BackupException if the data can't be stored (nothing is changed in that case).
     */
    suspend fun restore(backup: BackupFile) {
        try {
            database.withTransaction {
                backupDao.deleteEverything()
                backupDao.insertGoals(backup.goals.map { it.toEntity() })
                backupDao.insertTransactions(backup.transactions.map { it.toEntity() })
                backupDao.insertDenominations(backup.denominations.map { it.toEntity() })
                backupDao.insertDenominationEntries(backup.denominationEntries.map { it.toEntity() })
            }
        } catch (exception: SQLException) {
            // Broken links between records (e.g. a saving for a goal that isn't in the file).
            throw BackupException("The backup file is damaged and could not be restored. Your data was not changed.", exception)
        }
    }

    /** Basic sanity checks so obviously broken files are rejected with a clear message. */
    private fun validate(backup: BackupFile) {
        val problem = when {
            backup.goals.any { it.name.isBlank() || it.targetAmountPaise <= 0 } -> "a goal has no name or target"
            backup.transactions.any { it.amountPaise <= 0 } -> "a saving has an invalid amount"
            backup.denominations.any { it.valuePaise <= 0 || it.quantity < 1 } -> "a box row is invalid"
            backup.goals.map { it.id }.toSet().size != backup.goals.size -> "two goals share an id"
            else -> null
        }
        if (problem != null) throw BackupException("The backup file is damaged ($problem). Your data was not changed.")
    }

    // ----- Conversions (entity <-> backup) -----

    private fun SavingsGoalEntity.toBackup() = GoalBackup(id, name, emoji, description, targetAmountPaise, deadlineEpochDay, createdAtMillis, updatedAtMillis)
    private fun GoalBackup.toEntity() = SavingsGoalEntity(id, name, emoji, description, targetAmountPaise, deadlineEpochDay, createdAtMillis, updatedAtMillis)

    private fun SavingsTransactionEntity.toBackup() = TransactionBackup(id, goalId, amountPaise, type, note, source, savedAtMillis, createdAtMillis)
    private fun TransactionBackup.toEntity() = SavingsTransactionEntity(id, goalId, amountPaise, type, note, source, savedAtMillis, createdAtMillis)

    private fun DenominationEntity.toBackup() = DenominationBackup(id, goalId, valuePaise, quantity, createdAtMillis)
    private fun DenominationBackup.toEntity() = DenominationEntity(id, goalId, valuePaise, quantity, createdAtMillis)

    private fun DenominationEntryEntity.toBackup() = DenominationEntryBackup(id, denominationId, boxIndex, transactionId, markedAtMillis)
    private fun DenominationEntryBackup.toEntity() = DenominationEntryEntity(id, denominationId, boxIndex, transactionId, markedAtMillis)
}

/** A backup problem with a message that is safe to show to the user. */
class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)
