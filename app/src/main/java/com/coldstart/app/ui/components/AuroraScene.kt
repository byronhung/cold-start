package com.coldstart.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.coldstart.app.ui.theme.Phase
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin

/**
 * Aurora, ported from the round-2 prototype in its units (300 wide). Real aurora is curtains of
 * vertical rays: a sharp bright lower edge fading up into violet. Each curtain is ~160 thin rays
 * along a drifting curve, added together (BlendMode.Plus). Mountains and a lake sit in front, and
 * the nearer layers move more when the list scrolls. Dragging on open sky bends the curtains
 * toward the finger and brightens them.
 */
internal fun DrawScope.aurora(w: Float, h: Float, t: Float, phase: Phase, input: SceneInput, scroll: Float, k: Float) {
    val fx = phase.fx
    stars(w, h * 0.7f, t, phase.starAlpha, -scroll * 0.03f)

    // The pull eases in while held and out after release. Per-frame rates from the prototype (60 fps),
    // made frame-rate independent so a 120 Hz screen doesn't double them.
    val dt = (t - input.lastFrame).coerceIn(0f, 0.1f)
    input.lastFrame = t
    val rate = if (input.down) 0.12f else 0.025f
    input.pull += ((if (input.down) 1f else 0f) - input.pull) * (1 - (1 - rate).pow(dt * 60))
    val touch = Pull(input.x / k, input.y / k, input.pull)

    // A faint haze under the curtains, so they sit in air.
    drawRect(
        Brush.verticalGradient(
            0f to Color(0xFF7B5CFF).copy(alpha = 0f),
            0.6f to Color(0xFF2DFFA8).copy(alpha = 0.07f * fx),
            1f to Color(0xFF2DFFA8).copy(alpha = 0f),
            startY = h * 0.12f, endY = h * 0.62f,
        ),
        size = Size(w, h), blendMode = BlendMode.Plus,
    )
    val dy = -scroll * 0.06f
    curtain(w, h, t, LOW, h * 0.42f + dy, fx, touch)
    curtain(w, h, t * 0.8f, HIGH, h * 0.30f + dy, fx, touch)

    val far = when (phase) { Phase.DAY -> 0xFFA9BDD3; Phase.DAWN -> 0xFF3B3768; Phase.NIGHT -> 0xFF0B1A33 }
    val mid = when (phase) { Phase.DAY -> 0xFF8199B2; Phase.DAWN -> 0xFF272350; Phase.NIGHT -> 0xFF081426 }
    val near = when (phase) { Phase.DAY -> 0xFF5F7790; Phase.DAWN -> 0xFF161334; Phase.NIGHT -> 0xFF030A16 }
    ridge(w, h, h * 0.74f - scroll * 0.1f, h * 0.05f, 1.3, Color(far), trees = false)
    ridge(w, h, h * 0.80f - scroll * 0.16f, h * 0.035f, 4.1, Color(mid), trees = false)

    // A still lake catching the lights.
    val ly = h * 0.82f - scroll * 0.16f
    val lakeTop = when (phase) { Phase.DAY -> 0xFFC9D8E8; Phase.DAWN -> 0xFF6B5B9A; Phase.NIGHT -> 0xFF0E2A3A }
    val lakeBottom = if (phase == Phase.DAY) 0xFF9FB2C8 else 0xFF050B1F
    drawRect(Brush.verticalGradient(listOf(Color(lakeTop), Color(lakeBottom)), startY = ly, endY = h), Offset(0f, ly), Size(w, h - ly))
    for (r in 0 until 26) {
        val yy = ly + 3 + r * 4.2f
        val a = fx * 0.22f * (1 - r / 26f) * (0.6f + 0.4f * sin(t * 1.3f + r))
        val x0 = hash(r) * 0.6f * w + sin(t * 0.4f + r) * 8
        drawLine(
            Color(0xFF3DFFA8).copy(alpha = a.coerceIn(0f, 1f)),
            Offset(x0, yy), Offset(x0 + w * (0.2f + hash(r + 9) * 0.4f), yy),
            strokeWidth = 1.2f, blendMode = BlendMode.Plus,
        )
    }
    ridge(w, h, h * 0.9f - scroll * 0.24f, h * 0.03f, 7.7, Color(near), trees = true)
}

private class Pull(val x: Float, val y: Float, val s: Float)

private class Curtain(val len: Float, val off: Float, val gain: Float, colors: List<Long>) {
    /**
     * One ray's colour from its bright foot (0) to its tip (-1). Drawn once per curtain and
     * stretched to each ray's height, so a frame allocates no brushes.
     */
    val ray = Brush.verticalGradient(
        0f to Color(colors[0]).copy(alpha = 0f),
        0.03f to Color(colors[0]).copy(alpha = 0.95f),
        0.12f to Color(colors[1]).copy(alpha = 0.7f),
        0.45f to Color(colors[2]).copy(alpha = 0.3f),
        0.8f to Color(colors[3]).copy(alpha = 0.12f),
        1f to Color(colors[3]).copy(alpha = 0f),
        startY = 0f, endY = -1f,
    )
}

private val LOW = Curtain(0.34f, 0f, 0.55f, listOf(0xFFC9FFE4, 0xFF3DFFA8, 0xFF22C3B0, 0xFFB05CFF))
private val HIGH = Curtain(0.26f, 2.4f, 0.32f, listOf(0xFFB9FFF0, 0xFF3DE8C8, 0xFF5C7BFF, 0xFFFF5CC8))

private fun DrawScope.curtain(w: Float, h: Float, t: Float, c: Curtain, y: Float, fx: Float, touch: Pull) {
    val sigma2 = 2 * (w * 0.16f).pow(2)
    var x = -10f
    while (x <= w + 10) {
        val n = x / w
        var base = y + h * (0.06f * sin(n * 2.6f + t * 0.11f + c.off) + 0.03f * sin(n * 6.9f - t * 0.19f + c.off * 2))
        var boost = 0f
        if (touch.s > 0.001f) {
            val g = exp(-(x - touch.x).pow(2) / sigma2)
            base += (touch.y - base) * 0.45f * g * touch.s
            boost = g * touch.s * 0.9f
        }
        var fold = 0.5f + 0.5f * sin(n * 9 + t * 0.32f + c.off)
        fold *= fold
        val streak = 0.6f + 0.4f * sin(x * 0.61f + t * 1.9f + c.off * 3) * sin(x * 0.17f - t * 0.7f)
        val a = ((0.18f + 0.82f * fold) * streak * fx + boost) * c.gain
        if (a >= 0.01f) {
            val len = c.len * h * (0.55f + 0.45f * sin(n * 4.3f - t * 0.23f + c.off)) * (0.75f + 0.35f * fold + boost * 0.4f)
            val foot = base + 4
            withTransform({
                translate(x, foot)
                scale(1f, len + 4, Offset.Zero)
            }) {
                drawLine(c.ray, Offset.Zero, Offset(0f, -1f), strokeWidth = 2.6f, alpha = a.coerceIn(0f, 1f), blendMode = BlendMode.Plus)
            }
        }
        x += 2
    }
}

/** A mountain ridge across the screen, filled to the bottom; [trees] roughens it into pines. */
private fun DrawScope.ridge(w: Float, h: Float, y0: Float, amp: Float, seed: Double, color: Color, trees: Boolean) {
    val path = Path().apply {
        moveTo(0f, h)
        var x = 0f
        while (x <= w + 4) {
            val n = x / w
            var y = y0 - amp * (0.55f * sin(n * 4.1f + seed.toFloat()) + 0.3f * sin(n * 9.7f + seed.toFloat() * 2.3f) + 0.15f * sin(n * 23 + seed.toFloat() * 5))
            if (trees) {
                val kk = hash(floor(x / 4.0) + seed * 100)
                if (kk > 0.55f) y -= (kk - 0.55f) * 26
            }
            lineTo(x, y)
            x += 4
        }
        lineTo(w, h)
        close()
    }
    drawPath(path, color)
}
