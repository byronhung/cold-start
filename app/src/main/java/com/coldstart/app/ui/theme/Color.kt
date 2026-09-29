package com.coldstart.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The palette from the design brief. Dark only, on purpose: the app is mostly seen at 6am in an
 * unlit room, and a white screen at that moment is hostile.
 *
 * Colour carries meaning, so each hue has one job:
 *   Accent (teal) → done, on, progress
 *   Danger (red)  → the give-up bar, and nothing else outside the puzzles
 *   Stroop / shape colours → puzzle content only, never UI chrome
 */
object ColdColors {
    // Surfaces, darkest to lightest
    val Ground = Color(0xFF0D1017)
    val Surface = Color(0xFF151A24)
    val Line = Color(0xFF232B39)

    // Text, most to least important
    val Ink = Color(0xFFE6EAF2)
    val InkDim = Color(0xFFA6B0C2)
    val InkMute = Color(0xFF77839A)

    val Accent = Color(0xFF3FCFB4)
    val Danger = Color(0xFFE5484D)

    // Puzzle colours. Red and blue double as the odd-one-out shape colours:
    // they stay distinct under red-green colour blindness.
    val PuzzleRed = Color(0xFFE5484D)
    val PuzzleBlue = Color(0xFF4C8DFF)
    val PuzzleGreen = Color(0xFF46C46A)
    val PuzzleYellow = Color(0xFFE0B429)
}
