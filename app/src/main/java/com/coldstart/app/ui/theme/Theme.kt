package com.coldstart.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Material only supplies the odd system widget (text selection, cursor). Sunrise draws the rest.
private val SunriseScheme = darkColorScheme(
    primary = Sun.Amber,
    onPrimary = Sun.OnAmber,
    background = Color(0xFF0B1030),
    surface = Color(0xFF1A1B46),
    onSurface = Color(0xFFFFF6EC),
)

@Composable
fun ColdStartTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SunriseScheme, typography = ColdTypography, content = content)
}
