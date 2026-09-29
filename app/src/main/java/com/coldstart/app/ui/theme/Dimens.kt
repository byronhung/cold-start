package com.coldstart.app.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** One spacing scale. Every gap and padding in the app is one of these. */
object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** Side margin of every screen. */
    val screen = 20.dp
}

object ColdShapes {
    /** Stroop swatches, puzzle grid cells. */
    val small = RoundedCornerShape(10.dp)

    /** The add-alarm button. */
    val medium = RoundedCornerShape(16.dp)

    /** Progress pips, the give-up bar. */
    val pill = CircleShape
}
