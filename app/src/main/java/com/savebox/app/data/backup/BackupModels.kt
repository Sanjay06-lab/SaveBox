package com.savebox.app.data.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * The shape of a SaveBox backup file (JSON).
 *
 * These classes are deliberately SEPARATE from the Room entities: the database can change
 * between app versions, but old backup files must still be readable. If you change the
 * backup format, increase CURRENT_FORMAT_VERSION and keep reading the old version.
 */

const val CURRENT_FORMAT_VERSION = 1

@Serializable
data class BackupFile(
    @SerialName("app") val app: String = "SaveBox",
    @SerialName("format_version") val formatVersion: Int,
    @SerialName("app_version") val appVersion: String,
    @SerialName("exported_at") val exportedAtMillis: Long,
    @SerialName("goals") val goals: List<GoalBackup>,
    @SerialName("transactions") val transactions: List<TransactionBackup>,
    @SerialName("denominations") val denominations: List<DenominationBackup>,
    @SerialName("denomination_entries") val denominationEntries: List<DenominationEntryBackup>,
)

@Serializable
data class GoalBackup(
    val id: Long,
    val name: String,
    val emoji: String,
    val description: String,
    @SerialName("target_amount_paise") val targetAmountPaise: Long,
    @SerialName("deadline_epoch_day") val deadlineEpochDay: Long? = null,
    @SerialName("created_at") val createdAtMillis: Long,
    @SerialName("updated_at") val updatedAtMillis: Long,
)

@Serializable
data class TransactionBackup(
    val id: Long,
    @SerialName("goal_id") val goalId: Long,
    @SerialName("amount_paise") val amountPaise: Long,
    val type: String,
    val note: String = "",
    val source: String = "",
    @SerialName("saved_at") val savedAtMillis: Long,
    @SerialName("created_at") val createdAtMillis: Long,
)

@Serializable
data class DenominationBackup(
    val id: Long,
    @SerialName("goal_id") val goalId: Long,
    @SerialName("value_paise") val valuePaise: Long,
    val quantity: Int,
    @SerialName("created_at") val createdAtMillis: Long,
)

@Serializable
data class DenominationEntryBackup(
    val id: Long,
    @SerialName("denomination_id") val denominationId: Long,
    @SerialName("box_index") val boxIndex: Int,
    @SerialName("transaction_id") val transactionId: Long,
    @SerialName("marked_at") val markedAtMillis: Long,
)

/** Shown to the user before restoring, so they know what they're about to load. */
data class BackupSummary(
    val exportedAtMillis: Long,
    val goalCount: Int,
    val transactionCount: Int,
)
