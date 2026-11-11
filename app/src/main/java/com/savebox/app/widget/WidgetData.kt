package com.savebox.app.widget

import com.savebox.app.domain.calculations.SavingsCalculator
import com.savebox.app.domain.model.SavingsGoal
import com.savebox.app.utils.Currency
import com.savebox.app.utils.MoneyFormatter

/** Everything the widget shows, already formatted as text. */
data class WidgetData(
    val totalSavedText: String,
    val goal: WidgetGoal?,
)

data class WidgetGoal(
    val id: Long,
    val title: String,
    val percent: Int,
    /** 0.0..1.0 for the progress bar. */
    val progress: Float,
    val remainingText: String,
    val isCompleted: Boolean,
)

object WidgetDataBuilder {

    /**
     * Which goal the widget shows:
     *  1. the goal chosen in Settings, if it still exists;
     *  2. otherwise the most recently updated goal that isn't finished;
     *  3. otherwise the most recently updated goal.
     */
    fun chooseGoal(goals: List<SavingsGoal>, preferredGoalId: Long?): SavingsGoal? {
        goals.firstOrNull { goal -> goal.id == preferredGoalId }?.let { return it }
        val byRecent = goals.sortedByDescending { goal -> goal.lastActivityAtMillis }
        return byRecent.firstOrNull { goal -> !SavingsCalculator.isCompleted(goal) } ?: byRecent.firstOrNull()
    }

    fun build(goals: List<SavingsGoal>, preferredGoalId: Long?, currency: Currency): WidgetData {
        val summary = SavingsCalculator.summarize(goals)
        val goal = chooseGoal(goals, preferredGoalId)
        return WidgetData(
            totalSavedText = MoneyFormatter.format(summary.totalSavedPaise, currency),
            goal = goal?.let {
                WidgetGoal(
                    id = it.id,
                    title = "${it.emoji} ${it.name}",
                    percent = SavingsCalculator.progressPercent(it),
                    progress = SavingsCalculator.progressFraction(it),
                    remainingText = MoneyFormatter.format(SavingsCalculator.remainingAmount(it), currency),
                    isCompleted = SavingsCalculator.isCompleted(it),
                )
            },
        )
    }
}
