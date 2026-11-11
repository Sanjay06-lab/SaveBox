package com.savebox.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/*
 * Corner roundness for cards, buttons, dialogs, etc.
 * Increase the numbers for a "rounder" look, decrease for a sharper look.
 */
val SaveBoxShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),   // small chips, denomination boxes
    small = RoundedCornerShape(10.dp),       // text fields, small buttons
    medium = RoundedCornerShape(16.dp),      // cards
    large = RoundedCornerShape(24.dp),       // big hero cards, bottom sheets
    extraLarge = RoundedCornerShape(32.dp),  // dialogs
)
