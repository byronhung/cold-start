package com.coldstart.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import com.coldstart.app.ui.theme.Phase
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

/**
 * Monsoon, ported from the round-2 prototype in its units (300 wide). You're behind a rainy
 * window: three layers of low cloud, a forked bolt every 11 s (a new shape each time), flats in the
 * distance, rain at three depths, mist, then condensation and drops on the glass in front. Dragging
 * on open sky wipes the glass clear; it mists back over 4–7 s.
 */
internal fun DrawScope.monsoon(w: Float, h: Float, t: Float, phase: Phase, input: SceneInput, scroll: Float, k: Float) {
    val day = phase == Phase.DAY
    val dawn = phase == Phase.DAWN

    // Lightning: one strike per 11 s cycle. Flash, a dim gap, a second flash, then fade.
    val cyc = floor(t / 11f).toInt()
    val p = (t % 11f) / 11f
    var flash = 0f
    if (p > 0.9f && p < 0.95f) {
        flash = when {
            p < 0.905f -> 1f
            p < 0.915f -> 0.25f
            p < 0.925f -> 0.85f
            else -> 1 - (p - 0.925f) / 0.025f
        }
    }
    flash = flash.coerceIn(0f, 1f) * if (day) 0.25f else 1f
    if (flash > 0f) {
        drawRect(Color(200, 215, 240).copy(alpha = flash * 0.28f), size = Size(w, h))
        val bx = w * (0.2f + hash(cyc) * 0.6f)
        val pts = bolt(bx, h * 0.12f, h * 0.38f, cyc * 31, flash, 1.6f)
        val from = pts[5 + floor(hash(cyc + 7) * 4).toInt()]
        bolt(from.x, from.y, h * 0.16f, cyc * 31 + 99, flash * 0.7f, 1f)
    }

    // Low cloud, three layers drifting at different speeds.
    val cl = when (phase) {
        Phase.DAY -> listOf(0xFFAEB9C3, 0xFFBFC9D2, 0xFFD0D8DF)
        Phase.DAWN -> listOf(0xFF2A3442, 0xFF3A4656, 0xFF4C5A6B)
        Phase.NIGHT -> listOf(0xFF070C12, 0xFF0C131B, 0xFF131C27)
    }.map { Color(it) }
    cloudBand(w, h * 0.06f - scroll * 0.02f, h * 0.1f, t, 4f, cl[2].copy(alpha = 0.9f), 1f)
    cloudBand(w, h * 0.0f - scroll * 0.03f, h * 0.12f, t, 7f, cl[1].copy(alpha = 0.92f), 4f)
    cloudBand(w, h * -0.04f - scroll * 0.04f, h * 0.1f, t, 11f, cl[0].copy(alpha = 0.95f), 8f)
    if (flash > 0f) {
        drawRect(
            Brush.verticalGradient(listOf(Color(210, 225, 255).copy(alpha = flash * 0.25f), Color(210, 225, 255).copy(alpha = 0f)), startY = h * 0.05f, endY = h * 0.25f),
            size = Size(w, h * 0.25f),
        )
    }

    // Blocks of flats behind the rain, a few windows lit.
    val flats = when (phase) {
        Phase.DAY -> Color(120, 135, 150).copy(alpha = 0.35f)
        Phase.DAWN -> Color(20, 28, 40).copy(alpha = 0.6f)
        Phase.NIGHT -> Color(4, 8, 14).copy(alpha = 0.75f)
    }
    val bw = w / 9
    for (b in 0 until 9) {
        val top = h - h * (0.12f + hash(b + 50) * 0.14f) - scroll * 0.05f
        // Down to the bottom edge whatever the scroll, so a block never floats.
        drawRect(flats, Offset(b * bw, top), Size(bw - 3, h - top))
    }
    if (!day) {
        for (b2 in 0 until 60) {
            if (hash(b2 + 70) < 0.55f) continue
            val bi = b2 % 9
            val bh2 = h * (0.12f + hash(bi + 50) * 0.14f)
            drawRect(
                Color(255, 214, 150).copy(alpha = 0.25f + 0.2f * hash(b2)),
                Offset(bi * bw + 5 + hash(b2 + 3) * (bw - 14), h - bh2 + 8 + hash(b2 + 4) * (bh2 - 20) - scroll * 0.05f),
                Size(3f, 4f),
            )
        }
    }

    // Rain, far to near.
    for (layer in RAIN) {
        val color = if (day) Color(70, 86, 104).copy(alpha = layer.alpha * 1.3f) else Color(205, 222, 245).copy(alpha = layer.alpha)
        val dx = layer.len * 0.22f
        for (d in layer.drops) {
            val y = (d[1] * (h + 60) + t * layer.speed) % (h + 60) - 40
            val x = ((d[0] * w - y * 0.22f) % w + w) % w
            drawLine(color, Offset(x, y), Offset(x - dx, y + layer.len), strokeWidth = layer.width, cap = StrokeCap.Round)
        }
    }
    val mist = if (day) Color(0xFFF3F5F6) else Color(0xFF8FA3B8)
    drawRect(
        Brush.verticalGradient(listOf(mist.copy(alpha = 0f), mist.copy(alpha = if (day) 0.5f else 0.22f)), startY = h * 0.55f, endY = h),
        size = Size(w, h),
    )

    // Condensation, with clear patches where the glass was wiped. Drawn in its own layer so the
    // wipes (DstOut) cut through the mist only, not the rain behind it.
    val now = input.now
    input.wipes.removeAll { now - it.t >= 7f }
    drawIntoCanvas { it.saveLayer(Rect(0f, 0f, w, h), Paint()) }
    drawRect(if (day) Color.White.copy(alpha = 0.22f) else Color(170, 190, 210).copy(alpha = 0.10f), size = Size(w, h))
    for (q in input.wipes) {
        val a = wipeStrength(now - q.t)
        val c = Offset(q.x / k, q.y / k)
        drawCircle(
            Brush.radialGradient(0f to Color.Black.copy(alpha = a), 0.7f to Color.Black.copy(alpha = a * 0.9f), 1f to Color.Black.copy(alpha = 0f), center = c, radius = 26f),
            26f, c, blendMode = BlendMode.DstOut,
        )
    }
    drawIntoCanvas { it.restore() }

    // Drops on the glass: sit, then now and then run down leaving a trail.
    GLASS.forEachIndexed { i, d ->
        val cyc2 = floor((t + d.off) / d.period).toInt()
        val pp = ((t + d.off) % d.period) / d.period
        val x = (hash(i * 7 + cyc2) * 0.9f + 0.05f) * w
        val y0 = hash(i * 13 + cyc2) * h * 0.85f
        val slide = if (pp > 0.72f) ((pp - 0.72f) / 0.28f).pow(2) * h * 0.45f else 0f
        val y = y0 + slide
        val fade = when {
            pp < 0.06f -> pp / 0.06f
            pp > 0.96f -> (1 - pp) / 0.04f
            else -> 1f
        }
        var hidden = 0f
        for (q in input.wipes) {
            if (hypot(q.x / k - x, q.y / k - y) < 30f) hidden = max(hidden, wipeStrength(now - q.t))
        }
        val a = fade * (1 - hidden)
        if (a <= 0.02f) return@forEachIndexed
        if (slide > 0f) {
            drawLine(Color(220, 235, 255).copy(alpha = 0.12f * a), Offset(x, y0), Offset(x, y), strokeWidth = d.r * 0.5f)
        }
        val r = d.r * if (slide > 0f) 1.15f else 1f
        drawCircle(
            Brush.radialGradient(
                0f to if (day) Color.White.copy(alpha = 0.3f * a) else Color(200, 220, 245).copy(alpha = 0.18f * a),
                0.75f to if (day) Color.White.copy(alpha = 0.08f * a) else Color(160, 185, 215).copy(alpha = 0.06f * a),
                1f to Color.White.copy(alpha = (0.38f * a).coerceAtMost(1f)),
                center = Offset(x, y + r * 0.15f), radius = r,
            ),
            r, Offset(x, y),
        )
        // The dark crescent along the bottom edge, where the drop bends the light.
        val shade = Path().apply {
            arcTo(Rect(Offset(x + r * 0.15f, y + r * 0.2f), r * 0.9f), CRESCENT_START, CRESCENT_SWEEP, forceMoveTo = true)
            arcTo(Rect(Offset(x + r * 0.15f, y + r * 0.05f), r * 0.75f), CRESCENT_START + CRESCENT_SWEEP, -CRESCENT_SWEEP, forceMoveTo = false)
            close()
        }
        drawPath(shade, Color.Black.copy(alpha = 0.18f * a))
        drawCircle(Color.White.copy(alpha = (0.85f * a).coerceAtMost(1f)), max(0.6f, r * 0.18f), Offset(x - r * 0.35f, y - r * 0.35f))
    }
}

/** A wiped patch stays clear for 4 s, then mists over by 7 s. */
private fun wipeStrength(age: Float): Float = if (age < 4f) 1f else (1 - (age - 4f) / 3f).coerceIn(0f, 1f)

private val CRESCENT_START = Math.toDegrees(0.1).toFloat()
private val CRESCENT_SWEEP = Math.toDegrees(PI - 0.2).toFloat()

private class RainLayer(count: Int, val len: Float, val speed: Float, val alpha: Float, val width: Float, li: Int) {
    val drops = List(count) { i -> floatArrayOf(hash(i + li * 1000), hash(i + li * 1000 + 500)) }
}

/** Gentler than the prototype (Byron, 9 Oct): ~40% fewer drops, a third slower, fainter, shorter near streaks. */
private val RAIN = listOf(
    RainLayer(65, 8f, 340f, 0.09f, 0.7f, 0),
    RainLayer(42, 14f, 540f, 0.15f, 1f, 1),
    RainLayer(20, 24f, 820f, 0.2f, 1.3f, 2),
)

private class GlassDrop(val r: Float, val period: Float, val off: Float)

private val GLASS = List(70) { i -> GlassDrop(1.6f + hash(i + 2200).pow(2) * 5.5f, 9 + hash(i + 2300) * 16, hash(i + 2400) * 30) }

/** A low cloud layer: a wavy-bottomed band from the top of the screen, sliding sideways. */
private fun DrawScope.cloudBand(w: Float, y: Float, depth: Float, t: Float, speed: Float, color: Color, seed: Float) {
    val shift = (t * speed) % w
    val tau = (2 * PI).toFloat()
    val path = Path().apply {
        moveTo(-w, 0f)
        var x = -w
        while (x <= w * 2) {
            val n = (x + shift) / w
            lineTo(x, y + depth * (0.5f + 0.3f * sin(n * tau * 1.5f + seed) + 0.2f * sin(n * tau * 4.3f + seed * 3)))
            x += 6
        }
        lineTo(w * 2, 0f)
        close()
    }
    drawPath(path, color)
}

/** A jagged bolt down from (x, y): three strokes, wide and faint to thin and bright. Returns its points. */
private fun DrawScope.bolt(x: Float, y: Float, len: Float, seed: Int, a: Float, wide: Float): List<Offset> {
    val pts = ArrayList<Offset>(15)
    var cx = x
    var cy = y
    pts += Offset(cx, cy)
    for (i in 1..14) {
        cx += (hash(seed + i) - 0.5f) * 26
        cy += len / 14
        pts += Offset(cx, cy)
    }
    val path = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        for (j in 1 until pts.size) lineTo(pts[j].x, pts[j].y)
    }
    for ((width, alpha) in listOf(wide * 7 to 0.06f, wide * 3 to 0.18f, wide to 1f)) {
        drawPath(path, Color(235, 242, 255).copy(alpha = (a * alpha).coerceIn(0f, 1f)), style = Stroke(width, join = StrokeJoin.Round))
    }
    return pts
}
