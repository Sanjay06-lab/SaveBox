package com.savebox.app.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * One snackbar for the whole app (shown by SaveBoxApp above the bottom bar).
 *
 * Any screen can show a message with:
 *     val snackbar = LocalAppSnackbar.current
 *     snackbar.showMessage("Saved")
 *     snackbar.showUndo("Entry deleted") { viewModel.restore(deleted) }
 *
 * Because it lives at the app level, a message survives navigation: e.g. the
 * "₹500 saved · Undo" message stays visible after the Add Savings screen closes.
 */
@Stable
class AppSnackbarState(
    val hostState: SnackbarHostState,
    private val scope: CoroutineScope,
) {

    fun showMessage(message: String) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss() // replace any older message
            hostState.showSnackbar(message = message, withDismissAction = true, duration = SnackbarDuration.Short)
        }
    }

    /**
     * Shows [message] with an "Undo" button. [onUndo] runs only if the user taps Undo.
     * It runs in this app-level scope, so it still works after the screen that asked has closed.
     */
    fun showUndo(message: String, onUndo: suspend () -> Unit) {
        scope.launch {
            hostState.currentSnackbarData?.dismiss()
            val result = hostState.showSnackbar(
                message = message,
                actionLabel = "Undo",
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                onUndo()
            }
        }
    }
}

@Composable
fun rememberAppSnackbarState(): AppSnackbarState {
    val hostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    return remember(hostState, scope) { AppSnackbarState(hostState, scope) }
}

/** Gives every screen access to the app snackbar. Provided in SaveBoxApp. */
val LocalAppSnackbar = staticCompositionLocalOf<AppSnackbarState> {
    error("AppSnackbarState not provided. Wrap your content in SaveBoxApp.")
}
