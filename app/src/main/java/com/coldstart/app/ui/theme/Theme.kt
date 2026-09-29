package com.coldstart.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ColdScheme = darkColorScheme(
    primary = ColdColors.Accent,
    onPrimary = ColdColors.Ground,
    background = ColdColors.Ground,
    onBackground = ColdColors.Ink,
    surface = ColdColors.Surface,
    onSurface = ColdColors.Ink,
    onSurfaceVariant = ColdColors.InkDim,
    outline = ColdColors.Line,
    error = ColdColors.Danger,
)

/** Always dark: there is no light theme, whatever the phone's setting. */
@Composable
fun ColdStartTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColdScheme,
        typography = ColdTypography,
        content = content,
    )
}
