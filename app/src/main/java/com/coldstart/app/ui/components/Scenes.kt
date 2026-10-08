package com.coldstart.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.coldstart.app.ui.theme.Phase
import com.coldstart.app.ui.theme.Scene
import com.coldstart.app.ui.theme.Sky
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

/** One touch on open sky, in px and scene seconds. */
class Touch(val x: Float, val y: Float, val t: Float)

/**
 * What a scene knows about the screen it sits behind: how far the content has scrolled, and where
 * the sky was touched. Plain fields, written by [SkyBackground]'s input handlers and read while
 * drawing: the frame clock redraws every frame anyway, so none of this needs to be snapshot state.
 */
class SceneInput {
    /** Total scroll of whatever list sits on top, in px. Layers move by a fraction of it. */
    var scroll = 0f
    var down = false
    var x = 0f
    var y = 0f
    /** Scene seconds at the last frame, so touches are stamped on the same clock as the drawing. */
    var now = 0f
    val taps = ArrayList<Touch>()
    val wipes = ArrayList<Touch>()

    /** Aurora's pull toward the finger, eased in while held and out after release. */
    var pull = 0f
    var lastFrame = 0f

    fun press(at: Offset) {
        down = true
        x = at.x
        y = at.y
        taps += Touch(at.x, at.y, now)
        wipes += Touch(at.x, at.y, now)
    }

    fun move(at: Offset) {
        x = at.x
        y = at.y
        val last = wipes.lastOrNull()
        if (last == null || kotlin.math.hypot(last.x - at.x, last.y - at.y) > 8f) wipes += Touch(at.x, at.y, now)
    }
}

/**
 * The Plus skies' moving layer, drawn between the gradient and the screen's content.
 *
 * Ported scenes (Aurora so far) are drawn from the round-2 prototype
 * (claude.ai/artifact/3kQdAWeSjG49xjk6rHy92P) in its own units: a 300-wide phone, scaled up to the
 * real screen, so every number in the port matches the prototype's. The rest are still round 1.
 *
 * Everything runs off one clock in seconds, so each effect is a function of time and nothing piles
 * up across recompositions. Sunrise has no layer: its sun glow lives in [SkyBackground].
 */
@Composable
fun SceneLayer(sky: Sky, input: SceneInput, modifier: Modifier = Modifier) {
    if (sky.scene == Scene.SUN) return
    val seconds by produceState(0f) {
        val start = withFrameMillis { it }
        while (true) withFrameMillis { value = (it - start) / 1000f }
    }
    Canvas(modifier) {
        val t = seconds
        input.now = t
        if (sky.scene == Scene.AURORA) {
            proto(input) { w, h, scroll, k -> aurora(w, h, t, sky.phase, input, scroll, k) }
            return@Canvas
        }
        // Round-1 scenes, until each is ported.
        val fx = when (sky.phase) { Phase.NIGHT -> 0.9f; Phase.DAWN -> 0.55f; Phase.DAY -> 0.25f }
        when (sky.scene) {
            Scene.MONSOON -> monsoon(t, fx, sky.isLight)
            Scene.NEON -> neon(t, sky.phase)
            Scene.COAST -> coast(t, sky.phase, fx)
            else -> Unit
        }
    }
}

/** The prototype's phone was 300 px wide. */
private const val PROTO_W = 300f

/**
 * Draws in prototype units: [block] gets the width (300), the height in those units, the scroll
 * in those units, and k (prototype px → screen px) for converting touches.
 */
private inline fun DrawScope.proto(input: SceneInput, block: DrawScope.(w: Float, h: Float, scroll: Float, k: Float) -> Unit) {
    val k = size.width / PROTO_W
    withTransform({ scale(k, k, Offset.Zero) }) {
        block(PROTO_W, size.height / k, input.scroll / k, k)
    }
}

// ---------- shared by the ported scenes ----------

/** The prototype's hash: the same pseudo-random numbers, so stars and ridges land where they did there. */
internal fun hash(n: Double): Float {
    val s = sin(n * 12.9898 + 78.233) * 43758.5453
    return (s - floor(s)).toFloat()
}

internal fun hash(n: Int): Float = hash(n.toDouble())

/** Night effects at full strength, half at dawn, faint by day (the prototype's PH table). */
internal val Phase.fx: Float get() = when (this) { Phase.NIGHT -> 1f; Phase.DAWN -> 0.5f; Phase.DAY -> 0.14f }
internal val Phase.starAlpha: Float get() = when (this) { Phase.NIGHT -> 1f; Phase.DAWN -> 0.3f; Phase.DAY -> 0f }

private class Star(val x: Float, val y: Float, val r: Float, val phase: Float, val speed: Float)

private val STARS = List(160) { i ->
    Star(hash(i), hash(i + 300), 0.5f + hash(i + 600) * 1.3f, hash(i + 900) * 2 * PI.toFloat(), 0.6f + hash(i + 1200) * 2.4f)
}

/** Twinkling stars in the top [maxY] of the sky, nudged by [dy] for parallax. */
internal fun DrawScope.stars(w: Float, maxY: Float, t: Float, alpha: Float, dy: Float, count: Int = STARS.size) {
    if (alpha <= 0.01f) return
    for (i in 0 until count) {
        val s = STARS[i]
        val a = alpha * (0.45f + 0.55f * (0.5f + 0.5f * sin(t * s.speed + s.phase))) * (1 - s.y * 0.6f)
        drawCircle(Color(0xFFFFFAF0).copy(alpha = a.coerceIn(0f, 1f)), s.r * 0.75f, Offset(s.x * w, s.y * maxY + dy))
    }
}

// ---------- monsoon: slanted rain, a mist at the bottom, the odd distant flash ----------

/** Fixed drops, so the rain doesn't reshuffle on every frame. x, start offset, speed, length. */
private val drops = Random(7).let { r -> List(90) { floatArrayOf(r.nextFloat(), r.nextFloat(), 0.8f + r.nextFloat() * 0.5f, 0.6f + r.nextFloat() * 0.6f) } }

private fun DrawScope.monsoon(t: Float, fx: Float, light: Boolean) {
    drawRect(
        Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color(0x59F0F5FA)),
        size = size,
    )
    // On a pale day sky white rain vanishes, so it turns slate.
    val rain = if (light) Color(0x4D4A5A6C) else Color(0x38DCEBFF)
    val fall = size.height * 1.9f // per second: the mockup's 120 px every half second, scaled
    val len = size.height * 0.028f
    val dx = len * 0.27f // 105° slant
    for (d in drops) {
        val travel = (d[1] * size.height * 1.2f + t * fall * d[2]) % (size.height * 1.2f) - size.height * 0.1f
        // Drifts left as it falls (the 105° slant), wrapping round the sides.
        val x = d[0] * size.width - travel * 0.27f
        val wrapped = ((x % size.width) + size.width) % size.width
        drawLine(rain, Offset(wrapped, travel), Offset(wrapped - dx * d[3], travel + len * d[3]), strokeWidth = 1.5f, cap = StrokeCap.Round)
    }
    // A 9 s loop with a double flicker near the end: 92% .35, 93% .08, 94% .28.
    val p = (t % 9f) / 9f
    val flash = when {
        p < 0.91f || p > 0.95f -> 0f
        p < 0.92f -> (p - 0.91f) / 0.01f * 0.35f
        p < 0.93f -> 0.35f - (p - 0.92f) / 0.01f * 0.27f
        p < 0.94f -> 0.08f + (p - 0.93f) / 0.01f * 0.20f
        else -> 0.28f - (p - 0.94f) / 0.01f * 0.28f
    }
    if (flash > 0f) drawRect(Color(0xFFE8F0FF).copy(alpha = flash * fx), size = size)
}

// ---------- neon city: a skyline whose windows switch off as day comes ----------

private val buildingHeights = listOf(0.23f, 0.29f, 0.18f, 0.26f, 0.21f, 0.27f, 0.2f)

private fun DrawScope.neon(t: Float, phase: Phase) {
    val gap = size.width * 0.016f
    val n = buildingHeights.size
    val w = (size.width - gap * (n + 1)) / n
    val body = if (phase == Phase.DAY) Color(0xFFB98FA8) else Color(0xFF140829)
    buildingHeights.forEachIndexed { bi, hf ->
        val h = size.height * hf
        val left = gap + bi * (w + gap)
        val top = size.height - h
        drawRoundRect(body, Offset(left, top), Size(w, h + 8f), androidx.compose.ui.geometry.CornerRadius(8f))
        // Three columns of windows from the roof down.
        val pad = w * 0.12f
        val cols = 3
        val ww = (w - pad * 2 - pad * (cols - 1)) / cols
        val wh = size.height * 0.0115f
        val rows = ((h - pad * 2) / (wh + pad)).toInt()
        for (row in 0 until rows) for (c in 0 until cols) {
            val k = row * cols + c
            val on = when (phase) {
                Phase.NIGHT -> true
                Phase.DAWN -> (k + bi) % 3 == 0
                Phase.DAY -> false
            }
            // Each lit window goes dark for the last 30% of its own 6 s loop.
            val blinkOff = ((t + (k * 1.7f + bi * 2.3f)) % 6f) / 6f > 0.71f
            val o = Offset(left + pad + c * (ww + pad), top + pad + row * (wh + pad))
            if (on && !blinkOff) {
                drawRect(Color(0x55FF9AD5), o - Offset(2f, 2f), Size(ww + 4f, wh + 4f))
                drawRect(Color(0xFFFFD27A), o, Size(ww, wh))
            } else {
                drawRect(Color.White.copy(alpha = 0.08f), o, Size(ww, wh))
            }
        }
    }
}

// ---------- coast: a moon (or sun) over the sea, waves rolling in ----------

private fun DrawScope.coast(t: Float, phase: Phase, fx: Float) {
    val w = size.width
    val h = size.height
    val seaTop = h * 0.58f
    val r = w * 0.068f
    // Night: moon up and right. Dawn: the sun low over the water. Day: high and bright.
    val (orb, glow, centre) = when (phase) {
        Phase.NIGHT -> Triple(Color(0xFFFFF2D6), Color(0x99FFF2D6), Offset(w - w * 0.152f - r, h * 0.11f + r))
        Phase.DAWN -> Triple(Color(0xFFFFD08A), Color(0xE6FFBE78), Offset(w * 0.6f - r, seaTop - r * 1.4f))
        Phase.DAY -> Triple(Color(0xFFFFF6E0), Color(0xE6FFF0C8), Offset(w - w * 0.12f - r, h * 0.077f + r))
    }
    drawCircle(Brush.radialGradient(listOf(glow, Color.Transparent), centre, r * 2.6f), r * 2.6f, centre)
    drawCircle(orb, r, centre)

    val sea = when (phase) {
        Phase.NIGHT -> listOf(Color(0xFF0A2140), Color(0xFF06142A))
        Phase.DAWN -> listOf(Color(0xFFD98A5E), Color(0xFF6A4B7A))
        Phase.DAY -> listOf(Color(0xFF8FC8E0), Color(0xFF5FA6C8))
    }
    drawRect(Brush.verticalGradient(sea, startY = seaTop, endY = h), Offset(0f, seaTop), Size(w, h - seaTop))
    // The light on the water, under the orb.
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFFFFF0C8).copy(alpha = 0.5f * fx), Color.Transparent), startY = seaTop, endY = h * 0.92f),
        Offset(centre.x - w * 0.04f, seaTop), Size(w * 0.08f, h * 0.92f - seaTop),
    )
    wave(seaTop + h * 0.02f, t / 7f, Color.White.copy(alpha = 0.35f))
    wave(seaTop + h * 0.085f, t / 11f, Color.White.copy(alpha = 0.2f))
}

/** A gentle sine line across the screen, one wavelength (a quarter screen) per loop. */
private fun DrawScope.wave(y: Float, loops: Float, color: Color) {
    val wl = size.width / 4f
    val shift = (loops % 1f) * wl
    val path = Path()
    var x = -wl + shift
    path.moveTo(x, y)
    while (x < size.width + wl) {
        path.relativeQuadraticBezierTo(wl / 4, -size.height * 0.012f, wl / 2, 0f)
        path.relativeQuadraticBezierTo(wl / 4, size.height * 0.012f, wl / 2, 0f)
        x += wl
    }
    drawPath(path, color, style = Stroke(width = 4f, cap = StrokeCap.Round))
}
