package com.savebox.app.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.savebox.app.SaveBoxApplication
import com.savebox.app.ui.dashboard.DashboardViewModel
import com.savebox.app.ui.denomination.DenominationViewModel
import com.savebox.app.ui.settings.SettingsViewModel
import com.savebox.app.ui.transactions.AddSavingsViewModel
import com.savebox.app.ui.transactions.HistoryViewModel
import com.savebox.app.ui.goals.GoalDetailsViewModel
import com.savebox.app.ui.goals.GoalEditorViewModel

/**
 * Tells Android HOW to create each ViewModel (which repositories to pass in).
 *
 * In a screen you write:
 *     viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory)
 *
 * TO ADD A NEW VIEWMODEL: add one more initializer { ... } block below.
 *
 * createSavedStateHandle() gives the ViewModel the screen's navigation arguments
 * (for example the goalId of GoalDetailsRoute).
 */
object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer {
            MainViewModel(saveBoxApplication().container.userPreferencesRepository)
        }
        initializer {
            DashboardViewModel(saveBoxApplication().container.savingsRepository)
        }
        initializer {
            GoalEditorViewModel(
                savedStateHandle = createSavedStateHandle(),
                savingsRepository = saveBoxApplication().container.savingsRepository,
            )
        }
        initializer {
            GoalDetailsViewModel(
                savedStateHandle = createSavedStateHandle(),
                savingsRepository = saveBoxApplication().container.savingsRepository,
            )
        }
        initializer {
            AddSavingsViewModel(
                savedStateHandle = createSavedStateHandle(),
                savingsRepository = saveBoxApplication().container.savingsRepository,
            )
        }
        initializer {
            DenominationViewModel(
                savedStateHandle = createSavedStateHandle(),
                savingsRepository = saveBoxApplication().container.savingsRepository,
            )
        }
        initializer {
            HistoryViewModel(saveBoxApplication().container.savingsRepository)
        }
        initializer {
            val application = saveBoxApplication()
            val container = application.container
            SettingsViewModel(
                preferencesRepository = container.userPreferencesRepository,
                savingsRepository = container.savingsRepository,
                reminderScheduler = container.reminderScheduler,
                backupManager = container.backupManager,
                appDataObserver = container.appDataObserver,
                contentResolver = application.contentResolver,
            )
        }
    }
}

/** Finds our SaveBoxApplication (and therefore the AppContainer) from inside a factory. */
private fun CreationExtras.saveBoxApplication(): SaveBoxApplication {
    return this[APPLICATION_KEY] as SaveBoxApplication
}
