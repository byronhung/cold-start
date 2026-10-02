package com.coldstart.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Sunrise: the background is the sky at the current hour, from the approved prototype
 * (claude.ai/artifact/RJ5hJG9ZUmeCjKF3fH7S6f, "The sky by hour"). Every colour a screen uses for text
 * and glass comes from the sky it sits on, so a pale midday sky gets dark ink and a night sky light.
 */
@Immutable
data class Sky(
    val top: Color,
    val mid: Color,
    val bottom: Color,
    val isLight: Boolean,
    /** How strongly the sun glows at the bottom of the screen. */
    val sunAlpha: Float,
) {
    val brush: Brush get() = Brush.verticalGradient(0f to top, 0.58f to mid, 1f to bottom)

    val ink: Color get() = if (isLight) Color(0xFF2A1D3A) else Color(0xFFFFF6EC)
    val dim: Color get() = ink.copy(alpha = if (isLight) 0.72f else 0.74f)
    val mute: Color get() = ink.copy(alpha = 0.55f)
    val card: Color get() = if (isLight) Color.White.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.09f)
    val cardEdge: Color get() = if (isLight) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.14f)
    val chip: Color get() = if (isLight) Color(0xFF2A1D3A).copy(alpha = 0.08f) else Color.White.copy(alpha = 0.12f)
    val track: Color get() = if (isLight) Color(0xFF2A1D3A).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.18f)

    /** Repeat-day letters that are on. */
    val sunInk: Color get() = if (isLight) Color(0xFFC8562E) else Color(0xFFFFC27A)
}

object Skies {
    private val stops = listOf(
        0 to Sky(Color(0xFF0A0E2A), Color(0xFF17183F), Color(0xFF2A1D52), false, 0.35f),
        5 to Sky(Color(0xFF151638), Color(0xFF3B2564), Color(0xFF8A4170), false, 0.6f),
        6 to Sky(Color(0xFF2C205E), Color(0xFFA8476F), Color(0xFFF49D5F), false, 1f),
        8 to Sky(Color(0xFFF3AE78), Color(0xFFF8CDA2), Color(0xFFFCE7CB), true, 0.8f),
        11 to Sky(Color(0xFFF4DABB), Color(0xFFF9E8D2), Color(0xFFFEF5E9), true, 0.5f),
        17 to Sky(Color(0xFF4A2C6B), Color(0xFFC4566C), Color(0xFFF4A35E), false, 0.9f),
        19 to Sky(Color(0xFF1D1B4B), Color(0xFF3D2664), Color(0xFF713668), false, 0.5f),
        21 to Sky(Color(0xFF0B1030), Color(0xFF1A1B46), Color(0xFF2D2058), false, 0.35f),
    )

    fun forHour(hour: Int): Sky = stops.last { it.first <= hour.coerceIn(0, 23) }.second

    /** The ringing screen is always dawn, whatever the clock says. */
    val Dawn = Sky(Color(0xFF2C205E), Color(0xFF7B3A72), Color(0xFFF7A35F), false, 1f)

    /** Wake checks: full morning, light. */
    val Morning = Sky(Color(0xFFF09A63), Color(0xFFF7C08F), Color(0xFFFCE3C4), true, 0.6f)

    /** Settings, history, setup: the late-evening sky you see when setting alarms. */
    val Night = forHour(22)
}

/** The sky the current screen is drawn on. Set by SkyBackground. */
val LocalSky = staticCompositionLocalOf { Skies.Night }

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
