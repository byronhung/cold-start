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
import kotlin.math.sin
import kotlin.random.Random

/**
 * The Plus skies' moving layer, drawn between the gradient and the screen's content. Ported from
 * the mockup's CSS (claude.ai/artifact/RJ5hJG9ZUmeCjKF3fH7S6f, "Five skies"): same colours, sizes
 * as fractions of the mockup's 250 × 520 phone, same loop lengths.
 *
 * Everything runs off one clock in seconds, so each effect is a pure function of time and nothing
 * piles up across recompositions. Sunrise has no layer: its sun glow lives in [SkyBackground].
 */
@Composable
fun SceneLayer(sky: Sky, modifier: Modifier = Modifier) {
    if (sky.scene == Scene.SUN) return
    val seconds by produceState(0f) {
        val start = withFrameMillis { it }
        while (true) withFrameMillis { value = (it - start) / 1000f }
    }
    Canvas(modifier) {
        // How strongly night effects show: full at night, half at dawn, faint by day (mockup's fx).
        val fx = when (sky.phase) { Phase.NIGHT -> 0.9f; Phase.DAWN -> 0.55f; Phase.DAY -> 0.25f }
        when (sky.scene) {
            Scene.AURORA -> aurora(seconds, fx)
            Scene.MONSOON -> monsoon(seconds, fx, sky.isLight)
            Scene.NEON -> neon(seconds, sky.phase)
            Scene.COAST -> coast(seconds, sky.phase, fx)
            Scene.SUN -> Unit
        }
    }
}

/** 0..1..0 over [period] seconds, eased like CSS `alternate`. */
private fun swing(t: Float, period: Float, offset: Float = 0f): Float =
    ((1 - kotlin.math.cos(2 * PI * (t + offset) / period)) / 2).toFloat()

// ---------- aurora: two soft ribbons swaying across the top ----------

private fun DrawScope.aurora(t: Float, fx: Float) {
    ribbon(
        y = size.height * 0.10f, colors = listOf(Color(0xFF3DFFB0), Color(0xFF3DE0FF)),
        sway = swing(t, 22f), tilt = 1f, alpha = fx,
    )
    ribbon(
        y = size.height * 0.24f, colors = listOf(Color(0xFF9B7BFF), Color(0xFFFF7BD5)),
        sway = 1 - swing(t, 28f), tilt = -1f, alpha = fx * 0.7f,
    )
}

/**
 * A ribbon is a row of wide, soft blobs along a gentle wave: the mockup blurs a band with CSS,
 * which Compose can't do before Android 12, so the softness comes from radial gradients instead.
 */
private fun DrawScope.ribbon(y: Float, colors: List<Color>, sway: Float, tilt: Float, alpha: Float) {
    val n = 7
    val shiftX = (sway - 0.5f) * 0.24f * size.width
    for (i in 0 until n) {
        val f = i / (n - 1f)
        // Fade in and out at the ends, like the mockup's transparent gradient edges.
        val edge = sin(PI * f).toFloat()
        val c = lerp(colors[0], colors[1], f).copy(alpha = 0.55f * alpha * edge)
        val cx = -0.2f * size.width + f * 1.4f * size.width + shiftX
        val cy = y + tilt * (f - 0.5f) * 0.12f * size.height * (sway - 0.5f) * 2 + sin(f * 2 * PI).toFloat() * 0.02f * size.height
        val r = size.width * 0.16f
        withTransform({ scale(2.2f, 1f, Offset(cx, cy)) }) {
            drawCircle(Brush.radialGradient(listOf(c, Color.Transparent), center = Offset(cx, cy), radius = r), r, Offset(cx, cy))
        }
    }
}

private fun lerp(a: Color, b: Color, f: Float) = Color(
    a.red + (b.red - a.red) * f, a.green + (b.green - a.green) * f, a.blue + (b.blue - a.blue) * f, 1f,
)

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
