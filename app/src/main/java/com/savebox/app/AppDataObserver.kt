package com.savebox.app

import android.content.Context
import com.savebox.app.data.preferences.UserPreferencesRepository
import com.savebox.app.data.repository.SavingsRepository
import com.savebox.app.domain.calculations.Milestones
import com.savebox.app.domain.calculations.SavingsCalculator
import com.savebox.app.domain.model.SavingsGoal
import com.savebox.app.notifications.SaveBoxNotifications
import com.savebox.app.utils.AppCurrency
import com.savebox.app.widget.SaveBoxWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Background helpers that react to data changes for the WHOLE app, no matter which
 * screen is open. Started once in SaveBoxApplication.
 *
 *  1. Keeps AppCurrency in sync with the currency setting.
 *  2. Sends a milestone notification when a goal passes 25 / 50 / 75 / 100%.
 *  3. Refreshes the home-screen widget when savings or widget settings change.
 */
class AppDataObserver(
    private val context: Context,
    private val scope: CoroutineScope,
    private val savingsRepository: SavingsRepository,
    private val preferencesRepository: UserPreferencesRepository,
) {

    /**
     * Highest milestone already announced per goal. Loaded from settings on first use,
     * then kept here so two quick changes can't announce the same milestone twice.
     */
    private var announcedMilestones: MutableMap<Long, Int>? = null
    private val milestoneMutex = Mutex()

    fun start() {
        scope.launch { syncCurrency() }
        scope.launch { watchMilestones() }
        scope.launch { refreshWidgetOnChanges() }
    }

    private suspend fun syncCurrency() {
        preferencesRepository.userPreferences
            .map { preferences -> preferences.currency }
            .distinctUntilChanged()
            .collect { currency ->
                // Compose state is updated on the main thread.
                withContext(Dispatchers.Main) { AppCurrency.current = currency }
            }
    }

    private suspend fun watchMilestones() {
        savingsRepository.observeGoals().collect { goals -> checkMilestones(goals) }
    }

    private suspend fun checkMilestones(goals: List<SavingsGoal>) = milestoneMutex.withLock {
        val preferences = preferencesRepository.userPreferences.first()
        val announced = announcedMilestones ?: preferences.notifiedMilestones.toMutableMap().also { announcedMilestones = it }
        val before = announced.toMap()

        for (goal in goals) {
            val percent = SavingsCalculator.progressPercent(goal)
            val alreadyAnnounced = announced[goal.id]
            if (alreadyAnnounced == null) {
                // First time we see this goal: just remember where it is.
                announced[goal.id] = Milestones.highestReached(percent)
                continue
            }
            val milestone = Milestones.milestoneToAnnounce(percent, alreadyAnnounced) ?: continue
            announced[goal.id] = milestone
            if (preferences.milestoneNotificationsEnabled) {
                SaveBoxNotifications.showMilestone(context, goal.id, Milestones.message(goal.name, milestone))
            }
        }

        // Forget goals that were deleted.
        val existingIds = goals.map { goal -> goal.id }.toSet()
        announced.keys.retainAll(existingIds)

        if (announced != before) {
            preferencesRepository.setNotifiedMilestones(announced.toMap())
        }
    }

    /** Called before a backup is restored, so restored goals are treated as new (no old milestones fire). */
    suspend fun resetMilestones() = milestoneMutex.withLock {
        announcedMilestones = mutableMapOf()
        preferencesRepository.setNotifiedMilestones(emptyMap())
    }

    private suspend fun refreshWidgetOnChanges() {
        combine(
            savingsRepository.observeGoals(),
            preferencesRepository.userPreferences.map { preferences -> preferences.widgetGoalId to preferences.currency },
        ) { goals, widgetSettings -> goals to widgetSettings }
            .distinctUntilChanged()
            .collectLatest {
                // Wait a moment so several quick changes (e.g. ticking boxes) cause one refresh.
                delay(WIDGET_REFRESH_DELAY_MILLIS)
                try {
                    SaveBoxWidget.refreshAll(context)
                } catch (exception: Exception) {
                    // A widget problem must never break the app.
                    android.util.Log.w("SaveBox", "Widget refresh failed", exception)
                }
            }
    }

    private companion object {
        const val WIDGET_REFRESH_DELAY_MILLIS = 500L
    }
}
