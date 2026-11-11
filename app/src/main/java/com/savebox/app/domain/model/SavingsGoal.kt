package com.savebox.app.domain.model

import java.time.LocalDate

/**
 * A savings goal as the UI sees it.
 *
 * This is a "domain model": a plain Kotlin class with no database or Android code.
 * Screens and calculations only ever use this class, so the way data is stored
 * (in memory now, Room database in Phase 3) can change without touching the UI.
 *
 * All money values are in PAISE (₹1 = 100 paise). See utils/MoneyFormatter.kt.
 */
data class SavingsGoal(
    val id: Long,
    val name: String,
    val emoji: String,
    val description: String,
    val targetAmountPaise: Long,
    /** Optional date the user wants to reach the goal by. */
    val deadline: LocalDate?,
    /** When the goal was created (milliseconds since 1970, the standard Java timestamp). */
    val createdAtMillis: Long,
    /**
     * Total of all savings recorded for this goal.
     * Calculated from the transactions - never typed in directly - so it can't get out of sync.
     */
    val savedAmountPaise: Long,
    /** Last time the goal or any of its savings changed. Used for "Recently updated". */
    val lastActivityAtMillis: Long,
)

/**
 * The information the user types when creating or editing a goal.
 * It has no id or saved amount yet - the repository adds those.
 */
data class GoalDraft(
    val name: String,
    val emoji: String,
    val description: String,
    val targetAmountPaise: Long,
    val deadline: LocalDate?,
)
