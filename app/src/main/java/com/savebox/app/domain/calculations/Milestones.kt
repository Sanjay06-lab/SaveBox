package com.savebox.app.domain.calculations

/**
 * Progress milestones that earn a "well done" notification.
 * Change the list to celebrate different points (e.g. every 10%).
 */
object Milestones {

    val PERCENTAGES: List<Int> = listOf(25, 50, 75, 100)

    /** The highest milestone reached at [percent], or 0 if none yet. 60% -> 50. */
    fun highestReached(percent: Int): Int {
        return PERCENTAGES.lastOrNull { milestone -> percent >= milestone } ?: 0
    }

    /**
     * Decides whether to celebrate.
     * @param alreadyNotified the highest milestone already announced for this goal
     *                        (null = this goal has never been checked).
     * @return the milestone to announce, or null for "nothing new".
     *
     * Rules:
     *  - A goal seen for the first time is only recorded, never announced
     *    (so installing an update doesn't fire old milestones).
     *  - Falling back (e.g. unticking boxes) never re-announces a milestone.
     */
    fun milestoneToAnnounce(percent: Int, alreadyNotified: Int?): Int? {
        if (alreadyNotified == null) return null
        val reached = highestReached(percent)
        return if (reached > alreadyNotified) reached else null
    }

    fun message(goalName: String, milestone: Int): String {
        return when (milestone) {
            100 -> "🎉 You reached your $goalName goal! Amazing work."
            75 -> "75% of $goalName done. Your goal is within reach!"
            50 -> "Halfway to $goalName! You're getting closer to your goal."
            else -> "$milestone% of $goalName saved. Every small step counts."
        }
    }
}
