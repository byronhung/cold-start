package com.coldstart.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
    val screen = 16.dp
}

object ColdShapes {
    /** Alarm cards, settings sections. */
    val card = RoundedCornerShape(26.dp)

    /** The puzzle card while ringing. */
    val puzzle = RoundedCornerShape(30.dp)

    /** Buttons, swatches, inputs. */
    val button = RoundedCornerShape(18.dp)

    /** Icon buttons, day pills, grid cells. */
    val small = RoundedCornerShape(14.dp)

    /** The add button. */
    val fab = RoundedCornerShape(24.dp)

    val pill = RoundedCornerShape(50)
}

/**
 * The prototype's motion, ported. Its CSS spring `cubic-bezier(.34,1.56,.64,1)` overshoots and
 * settles; in Compose that's a spring with a low damping ratio.
 */
object Motion {
    /** Toggles, presses, the sliding segment: bouncy. */
    fun <T> bouncy() = spring<T>(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)

    /** Card entrances: a gentle overshoot. */
    fun <T> settle() = spring<T>(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow)

    /** `cubic-bezier(.2,.9,.3,1)`: fast out, soft landing. For fills and fades. */
    val softOut = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1f)
}
