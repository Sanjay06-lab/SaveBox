package com.savebox.app.navigation

import android.content.Context
import android.content.Intent
import com.savebox.app.MainActivity

/**
 * Builds Intents that open SaveBox, optionally at a specific goal.
 * Used by notifications and the home-screen widget.
 */
object DeepLinks {

    /** Intent extra holding the goal id to open (a Long). */
    const val EXTRA_GOAL_ID = "com.savebox.app.extra.GOAL_ID"

    fun openApp(context: Context, goalId: Long? = null): Intent {
        return Intent(context, MainActivity::class.java).apply {
            // Reuse the running app instead of starting a second copy.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (goalId != null) putExtra(EXTRA_GOAL_ID, goalId)
        }
    }

    /** Reads the goal id from an Intent, or null if there isn't one. */
    fun goalIdFrom(intent: Intent?): Long? {
        val goalId = intent?.getLongExtra(EXTRA_GOAL_ID, -1L) ?: -1L
        return goalId.takeIf { id -> id > 0 }
    }
}
