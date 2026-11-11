package com.savebox.app.domain.model

/**
 * Short encouraging messages chosen by how far along a goal is.
 * Add or edit messages here; the screens pick them up automatically.
 */
object MotivationMessages {

    fun forProgress(percent: Int): String {
        return when {
            percent <= 0 -> "Every small step counts. Start with any amount."
            percent < 25 -> "Great start! Every small step counts."
            percent < 50 -> "You're building a habit. Keep going!"
            percent < 75 -> "Past halfway soon. You're getting closer to your goal!"
            percent < 100 -> "Keep going. Your goal is within reach."
            else -> "Goal reached! You did it. 🎉"
        }
    }

    /** Messages for savings reminders. One is picked at random each time. */
    val reminderMessages: List<String> = listOf(
        "Every small step counts. Have you saved something today?",
        "You're getting closer to your goal! Add today's savings.",
        "Keep going. Your goal is within reach.",
        "A little today, a lot tomorrow. Time to fill a box!",
    )
}
