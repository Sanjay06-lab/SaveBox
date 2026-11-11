package com.savebox.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Light colour scheme. Each Material 3 "role" (primary, surface, ...) is mapped
 * to one of the SaveBox colours from Color.kt.
 */
internal val LightColors: ColorScheme = lightColorScheme(
    primary = Emerald600,
    onPrimary = NeutralSurfaceLight,
    primaryContainer = Emerald100,
    onPrimaryContainer = Emerald900,

    secondary = Navy900,
    onSecondary = NeutralSurfaceLight,
    secondaryContainer = Navy50,
    onSecondaryContainer = Navy900,

    tertiary = Gold500,
    onTertiary = Navy900,
    // Used by a few Material parts, e.g. the AM/PM switch of the time picker.
    tertiaryContainer = Emerald100,
    onTertiaryContainer = Emerald900,

    background = NeutralBackgroundLight,
    onBackground = NeutralTextLight,
    surface = NeutralSurfaceLight,
    onSurface = NeutralTextLight,
    surfaceVariant = NeutralSurfaceVariantLight,
    onSurfaceVariant = NeutralTextSecondaryLight,
    surfaceContainerLowest = NeutralSurfaceLight,
    surfaceContainerLow = NeutralSurfaceLight,
    surfaceContainer = NeutralSurfaceLight,
    surfaceContainerHigh = NeutralSurfaceVariantLight,
    surfaceContainerHighest = NeutralSurfaceVariantLight,
    outline = NeutralOutlineLight,
    outlineVariant = NeutralSurfaceVariantLight,

    // "Inverse" colours are used by snackbars (dark bar with a coloured action like "Undo").
    inverseSurface = Navy900,
    inverseOnSurface = NeutralSurfaceLight,
    inversePrimary = Emerald300,

    error = ErrorLight,
    onError = NeutralSurfaceLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = ErrorLight,
)

/** Dark colour scheme: same roles, darker surfaces and lighter brand colours. */
internal val DarkColors: ColorScheme = darkColorScheme(
    primary = Emerald400,
    onPrimary = Emerald900,
    primaryContainer = Emerald800,
    onPrimaryContainer = Emerald100,

    secondary = Navy200,
    onSecondary = Navy900,
    secondaryContainer = Navy600,
    onSecondaryContainer = Navy50,

    tertiary = Gold500,
    onTertiary = Navy900,
    tertiaryContainer = Emerald800,
    onTertiaryContainer = Emerald100,

    background = NeutralBackgroundDark,
    onBackground = NeutralTextDark,
    surface = NeutralSurfaceDark,
    onSurface = NeutralTextDark,
    surfaceVariant = NeutralSurfaceVariantDark,
    onSurfaceVariant = NeutralTextSecondaryDark,
    surfaceContainerLowest = NeutralBackgroundDark,
    surfaceContainerLow = NeutralSurfaceDark,
    surfaceContainer = NeutralSurfaceDark,
    surfaceContainerHigh = NeutralSurfaceVariantDark,
    surfaceContainerHighest = NeutralSurfaceVariantDark,
    outline = NeutralOutlineDark,
    outlineVariant = NeutralSurfaceVariantDark,

    inverseSurface = NeutralTextDark,
    inverseOnSurface = Navy900,
    inversePrimary = Emerald700,

    error = ErrorDark,
    onError = Navy950,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorDark,
)

/**
 * Wrap every screen in SaveBoxTheme so it gets SaveBox colours, fonts, shapes and spacing.
 *
 * @param darkTheme true = dark colours. Defaults to the phone's system setting.
 *                  (The Settings screen will let the user override this in Phase 5.)
 *
 * Note: we intentionally do NOT use Android 12 "dynamic colour" (wallpaper-based colours)
 * so that SaveBox always looks like SaveBox.
 */
@Composable
fun SaveBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    CompositionLocalProvider(LocalSpacing provides Spacing()) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SaveBoxTypography,
            shapes = SaveBoxShapes,
            content = content,
        )
    }
}
