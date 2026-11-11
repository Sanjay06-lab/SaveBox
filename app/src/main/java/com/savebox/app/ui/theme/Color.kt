package com.savebox.app.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * SaveBox colour palette.
 *
 * HOW TO CHANGE THE APP'S COLOURS:
 * 1. Change the raw colour values below (e.g. Emerald600).
 * 2. The light and dark colour schemes in Theme.kt use these values,
 *    so every screen updates automatically.
 *
 * Screens should NEVER use these raw colours directly. They should use
 * MaterialTheme.colorScheme.primary, .surface, etc. That way dark mode works for free.
 */

// Brand: emerald green (primary)
val Emerald50 = Color(0xFFECFDF5)
val Emerald100 = Color(0xFFD1FAE5)
val Emerald200 = Color(0xFFA7F3D0)
val Emerald300 = Color(0xFF6EE7B7)
val Emerald400 = Color(0xFF34D399)
val Emerald500 = Color(0xFF10B981)
val Emerald600 = Color(0xFF059669)
val Emerald700 = Color(0xFF047857)
val Emerald800 = Color(0xFF065F46)
val Emerald900 = Color(0xFF064E3B)

// Brand: dark navy (secondary)
val Navy50 = Color(0xFFEEF2F8)
val Navy100 = Color(0xFFD9E1EE)
val Navy200 = Color(0xFFB4C2DA)
val Navy400 = Color(0xFF5F7598)
val Navy600 = Color(0xFF2B3D5C)
val Navy800 = Color(0xFF16233A)
val Navy900 = Color(0xFF0F1B2D)
val Navy950 = Color(0xFF0A1220)

// Neutrals
val NeutralBackgroundLight = Color(0xFFF5F7F6)
val NeutralSurfaceLight = Color(0xFFFFFFFF)
val NeutralSurfaceVariantLight = Color(0xFFEEF1F0)
val NeutralOutlineLight = Color(0xFFD5DBD9)
val NeutralTextLight = Color(0xFF111827)
val NeutralTextSecondaryLight = Color(0xFF5B6472)

val NeutralBackgroundDark = Color(0xFF0B121C)
val NeutralSurfaceDark = Color(0xFF131C28)
val NeutralSurfaceVariantDark = Color(0xFF1C2735)
val NeutralOutlineDark = Color(0xFF2E3A4A)
val NeutralTextDark = Color(0xFFE8ECEF)
val NeutralTextSecondaryDark = Color(0xFF9AA5B4)

// Status colours
val ErrorLight = Color(0xFFDC2626)
val ErrorContainerLight = Color(0xFFFEE2E2)
val ErrorDark = Color(0xFFF87171)
val ErrorContainerDark = Color(0xFF4C1D1D)

// Used for "goal completed" highlights and warnings.
val Gold500 = Color(0xFFF59E0B)
