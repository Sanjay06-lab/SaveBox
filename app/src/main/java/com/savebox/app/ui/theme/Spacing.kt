package com.savebox.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One place for all spacing values, so the whole app has consistent gaps.
 *
 * Usage in a screen:  Modifier.padding(MaterialTheme.spacing.medium)
 */
data class Spacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,

    /** Horizontal padding at the left/right edge of every screen. */
    val screenPadding: Dp = 20.dp,

    /** Minimum size for anything the user taps (accessibility guideline: 48dp). */
    val minTouchTarget: Dp = 48.dp,
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }

/** Lets you write MaterialTheme.spacing.medium anywhere inside SaveBoxTheme. */
val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
