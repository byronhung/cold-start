package com.byronhung.firstlight.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// Material only supplies the odd system widget (text selection, cursor). Sunrise draws the rest.
private val SunriseScheme = darkColorScheme(
    primary = Sun.Amber,
    onPrimary = Sun.OnAmber,
    background = Color(0xFF0B1030),
    surface = Color(0xFF1A1B46),
    onSurface = Color(0xFFFFF6EC),
)

/** [sky]: the theme every screen inside draws its sky from (Settings › Theme). */
@Composable
fun FirstLightTheme(sky: SkyTheme = SkyTheme.SUNRISE, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSkyTheme provides sky) {
        MaterialTheme(colorScheme = SunriseScheme, typography = AppTypography, content = content)
    }
}
