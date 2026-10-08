package com.byronhung.firstlight.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * The background is the sky at the current hour, from the chosen [SkyTheme]. Every colour a screen
 * uses for text and glass comes from the sky it sits on, so a pale midday sky gets dark ink and a
 * night sky light.
 */
@Immutable
data class Sky(
    val top: Color,
    val mid: Color,
    val bottom: Color,
    val isLight: Boolean,
    /** How strongly the sun glows at the bottom of the screen. Only Sunrise has the glow. */
    val sunAlpha: Float,
    val phase: Phase = if (isLight) Phase.DAY else Phase.NIGHT,
    val scene: Scene = Scene.SUN,
) {
    val brush: Brush get() = Brush.verticalGradient(0f to top, 0.58f to mid, 1f to bottom)

    val ink: Color get() = if (isLight) Color(0xFF2A1D3A) else Color(0xFFFFF6EC)
    val dim: Color get() = ink.copy(alpha = if (isLight) 0.72f else 0.74f)
    val mute: Color get() = ink.copy(alpha = 0.55f)
    /**
     * Glass for cards. Sunrise's sky is smooth, so clear glass reads fine. Plus skies have scenes
     * (lit windows, rain, glints) that fight with text, so their glass is frosted: the sky's own
     * darkest colour (white by day) at 65%, which keeps the scene visible but softened.
     */
    val card: Color get() = when {
        scene == Scene.SUN -> if (isLight) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.09f)
        isLight -> Color.White.copy(alpha = 0.65f)
        else -> top.copy(alpha = 0.65f)
    }
    val cardEdge: Color get() = if (isLight) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.14f)
    val chip: Color get() = if (isLight) Color(0xFF2A1D3A).copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f)
    val track: Color get() = if (isLight) Color(0xFF2A1D3A).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.18f)

    /** Repeat-day letters that are on. */
    val sunInk: Color get() = if (isLight) Color(0xFFC8562E) else Color(0xFFFFC27A)
}

/** The sky the current screen is drawn on. Set by SkyBackground. */
val LocalSky = staticCompositionLocalOf { SkyTheme.SUNRISE.night }

/** Fixed colours that don't depend on the sky. */
object Sun {
    val AmberLight = Color(0xFFFFD08A)
    val Amber = Color(0xFFF2845A)
    val ToggleLight = Color(0xFFFFC67E)
    val OnAmber = Color(0xFF3A1F2E)
    val Glow = Color(0xFFFFE1B0)

    /** The give-up ring. Rose, and only there. */
    val Rose = Color(0xFFFF8F9C)

    /** The dark glass the puzzles sit on, so coloured words stay readable on a bright dawn. */
    val PuzzleGlass = Color(0xFF160C2A).copy(alpha = 0.55f)
    val OnGlass = Color(0xFFFFF6EC)

    val PuzzleRed = Color(0xFFFF6B7A)
    val PuzzleBlue = Color(0xFF6EA8FF)
    val PuzzleGreen = Color(0xFF5FD08A)
    val PuzzleYellow = Color(0xFFFFD166)

    val amberBrush: Brush get() = Brush.linearGradient(listOf(AmberLight, Amber))
    val toggleBrush: Brush get() = Brush.linearGradient(listOf(ToggleLight, Amber))
}
