package com.savebox.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savebox.app.data.preferences.ThemeMode
import com.savebox.app.navigation.DeepLinks
import com.savebox.app.ui.AppViewModelProvider
import com.savebox.app.ui.MainViewModel
import com.savebox.app.ui.SaveBoxApp
import com.savebox.app.ui.theme.SaveBoxTheme

/**
 * The single Activity of SaveBox. All screens are Compose screens inside it.
 */
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels { AppViewModelProvider.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate() to show the branded splash screen.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep the splash visible until we know the first screen and the theme.
        splashScreen.setKeepOnScreenCondition {
            mainViewModel.startDestination == null || mainViewModel.preferences.value == null
        }

        // Opened from the widget or a notification? (Only on a fresh start, not after rotation.)
        if (savedInstanceState == null) {
            mainViewModel.openGoal(DeepLinks.goalIdFrom(intent))
        }

        setContent {
            val preferences by mainViewModel.preferences.collectAsStateWithLifecycle()
            val themeMode = preferences?.themeMode ?: ThemeMode.SYSTEM
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Draw behind the status/navigation bars, with icon colours that match the theme.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
                )
                onDispose {}
            }

            SaveBoxTheme(darkTheme = darkTheme) {
                val startDestination = mainViewModel.startDestination
                if (startDestination != null && preferences != null) {
                    SaveBoxApp(
                        startDestination = startDestination,
                        onOnboardingFinished = mainViewModel::completeOnboarding,
                        pendingGoalId = mainViewModel.pendingGoalId,
                        onGoalOpened = mainViewModel::onGoalOpened,
                    )
                }
            }
        }
    }

    /** Called when the app is already open and the widget/notification is tapped. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        mainViewModel.openGoal(DeepLinks.goalIdFrom(intent))
    }

    private companion object {
        // The same light/dark navigation-bar scrims Android uses by default.
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
