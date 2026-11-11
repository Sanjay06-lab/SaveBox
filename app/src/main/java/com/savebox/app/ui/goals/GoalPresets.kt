package com.savebox.app.ui.goals

/**
 * Ideas shown on the "New goal" screen. Tapping one fills in the name and icon;
 * the user still chooses their own target amount (nothing is hardcoded).
 *
 * Add, remove or reorder entries freely.
 */
data class GoalPreset(val name: String, val emoji: String)

object GoalPresets {

    val examples: List<GoalPreset> = listOf(
        GoalPreset(name = "New phone", emoji = "📱"),
        GoalPreset(name = "Laptop", emoji = "💻"),
        GoalPreset(name = "PlayStation", emoji = "🎮"),
        GoalPreset(name = "Travel", emoji = "✈️"),
        GoalPreset(name = "Emergency fund", emoji = "🛟"),
        GoalPreset(name = "Personal savings", emoji = "💰"),
    )

    /** Icons the user can pick for any goal. */
    val emojiChoices: List<String> = listOf(
        "🎯", "💰", "📱", "💻", "🎮", "✈️", "🛟", "🏠",
        "🚗", "🏍️", "🎓", "💍", "🎁", "📷", "⌚", "👟",
        "🎸", "🏖️", "🩺", "🐶",
    )

    const val DEFAULT_EMOJI = "🎯"
}
