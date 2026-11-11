package com.savebox.app.data.repository

import androidx.room.withTransaction
import com.savebox.app.data.database.SaveBoxDatabase
import com.savebox.app.data.entities.DenominationEntity
import com.savebox.app.data.entities.DenominationEntryEntity
import com.savebox.app.data.entities.SavingsGoalEntity
import com.savebox.app.data.entities.SavingsTransactionEntity
import com.savebox.app.domain.model.AddDenominationResult
import com.savebox.app.domain.model.BoxLink
import com.savebox.app.domain.model.BoxToggleResult
import com.savebox.app.domain.model.DeletedTransaction
import com.savebox.app.domain.model.Denomination
import com.savebox.app.domain.model.GoalDraft
import com.savebox.app.domain.model.SavingsEntry
import com.savebox.app.domain.model.SavingsGoal
import com.savebox.app.domain.model.SavingsTransaction
import com.savebox.app.domain.model.TransactionType
import com.savebox.app.domain.model.TransactionWithGoal
import com.savebox.app.utils.MoneyFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The real SavingsRepository: everything is saved in the Room database on the phone,
 * so it is still there after the app (or the phone) restarts.
 *
 * Every change that touches more than one row runs inside `database.withTransaction { }`.
 * That means "all or nothing": if anything fails half-way, the database is left exactly
 * as it was, so totals, history and the box grid can never disagree.
 *
 * @param currentTimeMillis where "now" comes from. Tests can pass a fixed time.
 */
class RoomSavingsRepository(
    private val database: SaveBoxDatabase,
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() },
) : SavingsRepository {

    private val goalDao = database.goalDao()
    private val transactionDao = database.transactionDao()
    private val denominationDao = database.denominationDao()

    // =====================================================================
    // Goals
    // =====================================================================

    override fun observeGoals(): Flow<List<SavingsGoal>> {
        return goalDao.observeGoalsWithTotals()
            .map { rows -> rows.map { row -> row.toDomain() } }
            // Room re-runs the query when ANY table it reads changes; skip identical results.
            .distinctUntilChanged()
    }

    override fun observeGoal(goalId: Long): Flow<SavingsGoal?> {
        return goalDao.observeGoalWithTotals(goalId)
            .map { row -> row?.toDomain() }
            .distinctUntilChanged()
    }

    override suspend fun getGoal(goalId: Long): SavingsGoal? {
        return goalDao.getGoalWithTotals(goalId)?.toDomain()
    }

    override suspend fun createGoal(draft: GoalDraft, startingAmountPaise: Long): Long {
        val now = currentTimeMillis()

        // If saving the starting amount failed, the goal is not saved either.
        return database.withTransaction {
            val goalId = goalDao.insert(
                SavingsGoalEntity(
                    name = draft.name,
                    emoji = draft.emoji,
                    description = draft.description,
                    targetAmountPaise = draft.targetAmountPaise,
                    deadlineEpochDay = draft.deadline?.toEpochDay(),
                    createdAtMillis = now,
                    updatedAtMillis = now,
                ),
            )

            if (startingAmountPaise > 0) {
                transactionDao.insert(
                    SavingsTransactionEntity(
                        goalId = goalId,
                        amountPaise = startingAmountPaise,
                        type = TransactionType.STARTING_AMOUNT.name,
                        note = "",
                        source = "",
                        savedAtMillis = now,
                        createdAtMillis = now,
                    ),
                )
            }
            goalId
        }
    }

    override suspend fun updateGoal(goalId: Long, draft: GoalDraft) {
        goalDao.updateDetails(
            goalId = goalId,
            name = draft.name,
            emoji = draft.emoji,
            description = draft.description,
            targetAmountPaise = draft.targetAmountPaise,
            deadlineEpochDay = draft.deadline?.toEpochDay(),
            updatedAtMillis = currentTimeMillis(),
        )
    }

    override suspend fun deleteGoal(goalId: Long) {
        // One statement is enough: foreign keys with CASCADE remove the goal's
        // transactions, denominations and ticked boxes automatically.
        goalDao.deleteById(goalId)
    }

    // =====================================================================
    // Savings records
    // =====================================================================

    override fun observeTransactionsForGoal(goalId: Long): Flow<List<SavingsTransaction>> {
        return transactionDao.observeForGoal(goalId)
            .map { rows -> rows.map { row -> row.toDomain() } }
            .distinctUntilChanged()
    }

    override fun observeAllTransactions(): Flow<List<TransactionWithGoal>> {
        return transactionDao.observeAllWithGoal()
            .map { rows -> rows.map { row -> row.toDomain() } }
            .distinctUntilChanged()
    }

    override suspend fun getTransaction(transactionId: Long): SavingsTransaction? {
        return transactionDao.getById(transactionId)?.toDomain()
    }

    override suspend fun getLatestSavingCreatedAt(): Long? {
        return transactionDao.latestCreatedAt()
    }

    override suspend fun addSavings(entry: SavingsEntry): Long {
        return transactionDao.insert(
            SavingsTransactionEntity(
                goalId = entry.goalId,
                amountPaise = entry.amountPaise,
                type = TransactionType.CUSTOM.name,
                note = entry.note,
                source = entry.source,
                savedAtMillis = entry.savedAtMillis,
                createdAtMillis = currentTimeMillis(),
            ),
        )
    }

    override suspend fun updateSavings(transactionId: Long, entry: SavingsEntry): Boolean {
        return database.withTransaction {
            val existing = transactionDao.getById(transactionId) ?: return@withTransaction false

            // A box's amount must always equal the box value, and the box belongs to one goal.
            val isBoxSaving = existing.type == TransactionType.DENOMINATION.name

            val updated = existing.copy(
                goalId = if (isBoxSaving) existing.goalId else entry.goalId,
                amountPaise = if (isBoxSaving) existing.amountPaise else entry.amountPaise,
                note = entry.note,
                source = entry.source,
                savedAtMillis = entry.savedAtMillis,
            )
            transactionDao.update(updated) > 0
        }
    }

    override suspend fun deleteTransaction(transactionId: Long): DeletedTransaction? {
        return database.withTransaction {
            val existing = transactionDao.getById(transactionId) ?: return@withTransaction null
            // Remember the box (if any) BEFORE deleting, so undo can tick it again.
            val entry = denominationDao.getEntryForTransaction(transactionId)

            transactionDao.deleteById(transactionId) // CASCADE also removes the box entry

            DeletedTransaction(
                transaction = existing.toDomain(),
                boxLink = entry?.let { BoxLink(denominationId = it.denominationId, boxIndex = it.boxIndex) },
            )
        }
    }

    override suspend fun restoreTransaction(deleted: DeletedTransaction): Boolean {
        return database.withTransaction {
            if (!goalDao.exists(deleted.transaction.goalId)) return@withTransaction false

            // Room uses AUTOINCREMENT, so a deleted id is never handed out again:
            // re-inserting with the original id is safe.
            transactionDao.insert(deleted.transaction.toEntity())

            val boxLink = deleted.boxLink
            if (boxLink != null) {
                val denomination = denominationDao.getDenomination(boxLink.denominationId)
                val boxStillExists = denomination != null && boxLink.boxIndex < denomination.quantity
                val boxIsFree = denominationDao.getEntry(boxLink.denominationId, boxLink.boxIndex) == null
                if (boxStillExists && boxIsFree) {
                    denominationDao.insertEntry(
                        DenominationEntryEntity(
                            denominationId = boxLink.denominationId,
                            boxIndex = boxLink.boxIndex,
                            transactionId = deleted.transaction.id,
                            markedAtMillis = currentTimeMillis(),
                        ),
                    )
                }
            }
            true
        }
    }

    // =====================================================================
    // Denomination grid
    // =====================================================================

    override fun observeDenominations(goalId: Long): Flow<List<Denomination>> {
        // Two queries (rows + ticked boxes) combined into one list of Denomination.
        return combine(
            denominationDao.observeDenominationsForGoal(goalId),
            denominationDao.observeEntriesForGoal(goalId),
        ) { rows, entries ->
            val entriesByDenomination = entries.groupBy { entry -> entry.denominationId }
            rows.map { row -> row.toDomain(entriesByDenomination) }
        }.distinctUntilChanged()
    }

    override suspend fun addDenomination(goalId: Long, valuePaise: Long, quantity: Int): AddDenominationResult {
        return database.withTransaction {
            if (denominationDao.findByValue(goalId, valuePaise) != null) {
                return@withTransaction AddDenominationResult.DuplicateValue
            }
            val id = denominationDao.insertDenomination(
                DenominationEntity(
                    goalId = goalId,
                    valuePaise = valuePaise,
                    quantity = quantity,
                    createdAtMillis = currentTimeMillis(),
                ),
            )
            AddDenominationResult.Added(id)
        }
    }

    override suspend fun updateDenominationQuantity(denominationId: Long, quantity: Int): Boolean {
        return database.withTransaction {
            // Box numbers start at 0, so with a ticked box #9 we need at least 10 boxes.
            val highestTickedBox = denominationDao.maxMarkedBoxIndex(denominationId) ?: -1
            if (quantity <= highestTickedBox) return@withTransaction false
            denominationDao.updateQuantity(denominationId, quantity) > 0
        }
    }

    override suspend fun deleteDenomination(denominationId: Long) {
        database.withTransaction {
            // Remove the savings that this row's ticked boxes created, then the row itself.
            val transactionIds = denominationDao.transactionIdsForDenomination(denominationId)
            if (transactionIds.isNotEmpty()) {
                transactionDao.deleteByIds(transactionIds)
            }
            denominationDao.deleteDenomination(denominationId)
        }
    }

    override suspend fun toggleBox(denominationId: Long, boxIndex: Int): BoxToggleResult {
        return database.withTransaction {
            val denomination = denominationDao.getDenomination(denominationId)
                ?: return@withTransaction BoxToggleResult.NotFound
            if (boxIndex !in 0 until denomination.quantity) {
                return@withTransaction BoxToggleResult.NotFound
            }

            val existingEntry = denominationDao.getEntry(denominationId, boxIndex)
            if (existingEntry != null) {
                // UN-TICK: delete the box's transaction (CASCADE removes the entry).
                val transaction = transactionDao.getById(existingEntry.transactionId)
                    ?: return@withTransaction BoxToggleResult.NotFound
                transactionDao.deleteById(transaction.id)
                BoxToggleResult.Unmarked(
                    DeletedTransaction(
                        transaction = transaction.toDomain(),
                        boxLink = BoxLink(denominationId, boxIndex),
                    ),
                )
            } else {
                // TICK: record the saving and link it to the box, together.
                val now = currentTimeMillis()
                val transactionId = transactionDao.insert(
                    SavingsTransactionEntity(
                        goalId = denomination.goalId,
                        amountPaise = denomination.valuePaise,
                        type = TransactionType.DENOMINATION.name,
                        note = "${MoneyFormatter.format(denomination.valuePaise)} box #${boxIndex + 1}",
                        source = "",
                        savedAtMillis = now,
                        createdAtMillis = now,
                    ),
                )
                denominationDao.insertEntry(
                    DenominationEntryEntity(
                        denominationId = denominationId,
                        boxIndex = boxIndex,
                        transactionId = transactionId,
                        markedAtMillis = now,
                    ),
                )
                BoxToggleResult.Marked(transactionId)
            }
        }
    }
}
