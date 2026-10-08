package com.coldstart.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.coldstart.app.ui.theme.Phase
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Neon city, ported from the round-2 prototype in its units (300 wide). Above: stars, the city's
 * glow, two searchlights, low cloud lit pink from below, a plane blinking past. Below: three depths
 * of skyline (nearer moves more on scroll), windows switching on and off on their own slow clocks,
 * antenna lights and vertical neon signs. A tap on open sky lights every window in a ring spreading
 * out from the finger for 6 s, and makes the signs buzz.
 */
internal fun DrawScope.neon(w: Float, h: Float, t: Float, phase: Phase, input: SceneInput, scroll: Float, k: Float) {
    val fx = phase.fx
    val day = phase == Phase.DAY
    val dawn = phase == Phase.DAWN
    val pink = Color(0xFFFF4FB8)

    stars(w, h * 0.4f, t, phase.starAlpha * 0.5f, -scroll * 0.02f, count = 60)

    // The city's glow on the low sky.
    val glowTop = if (day) Color(0xFFFFB89A) else pink
    drawRect(
        Brush.verticalGradient(
            listOf(pink.copy(alpha = 0f), glowTop.copy(alpha = if (day) 0.25f else 0.32f * if (dawn) 0.6f else 1f)),
            startY = h * 0.25f, endY = h * 0.7f,
        ),
        size = Size(w, h),
    )

    // Searchlights sweeping from behind the skyline.
    if (!day) {
        for ((at, period, off) in SEARCHLIGHTS) {
            val ang = -PI.toFloat() / 2 + 0.42f * sin(t * TAU / period + off)
            val ox = at * w
            val oy = h * 0.72f - scroll * 0.06f
            val len = h * 0.75f
            val spread = 0.07f
            val tip = Offset(ox + cos(ang) * len, oy + sin(ang) * len)
            val beam = Path().apply {
                moveTo(ox, oy)
                lineTo(ox + cos(ang - spread) * len, oy + sin(ang - spread) * len)
                lineTo(ox + cos(ang + spread) * len, oy + sin(ang + spread) * len)
                close()
            }
            drawPath(
                beam,
                Brush.linearGradient(listOf(Color(200, 215, 255).copy(alpha = 0.22f * fx), Color(200, 215, 255).copy(alpha = 0f)), start = Offset(ox, oy), end = tip),
                blendMode = BlendMode.Plus,
            )
        }
    }

    // Low cloud lit pink from below (white by day).
    CLOUDS.forEachIndexed { ki, c ->
        val y = h * c.y - scroll * (0.03f + ki * 0.01f)
        val depth = h * c.depth
        val shift = (t * c.speed) % (w * 2)
        val brush = Brush.verticalGradient(
            0f to (if (day) Color.White else Color(0xFF2A0B4F)).copy(alpha = 0f),
            0.7f to (if (day) Color.White else Color(0xFF5A1A6E)).copy(alpha = if (day) 0.45f else 0.55f),
            1f to (if (day) Color(0xFFFFE4D6) else Color(0xFFFF6FC2)).copy(alpha = if (day) 0.4f else 0.35f * if (dawn) 0.6f else 1f),
            startY = y - depth, endY = y + depth,
        )
        val band = Path().apply {
            moveTo(-w, y - depth)
            var x = -w
            while (x <= w * 2) {
                val n = (x + shift) / w
                lineTo(x, y + depth * (0.4f * sin(n * 7 + c.seed) + 0.3f * sin(n * 17 + c.seed * 2)) * (0.5f + 0.5f * sin(n * 2.3f + c.seed)))
                x += 6
            }
            lineTo(w * 2, y - depth)
            close()
        }
        drawPath(band, brush)
    }

    // A plane crossing, nav lights blinking.
    val pp = ((t + 6) % 38f) / 38f
    val plx = -20 + pp * (w + 40)
    val ply = h * 0.16f - pp * h * 0.04f - scroll * 0.03f
    if (sin(t * 6) > 0.6f) drawCircle(Color(255, 90, 90).copy(alpha = 0.95f), 1.6f, Offset(plx, ply))
    if (sin(t * 6 + 2) > 0.85f) drawCircle(Color.White.copy(alpha = 0.95f), 1.3f, Offset(plx - 5, ply + 0.5f))

    // The skyline, far to near.
    val now = input.now
    input.taps.removeAll { now - it.t >= 8f }
    val taps = input.taps.map { Offset(it.x / k, it.y / k) to (now - it.t) }
    val buzz = taps.any { it.second < 1.2f }
    LAYERS.forEachIndexed { li, layer ->
        val off = -scroll * layer.parallax
        val body = Color(when (phase) { Phase.DAY -> layer.day; Phase.DAWN -> layer.dawn; Phase.NIGHT -> layer.night })
        for (b in layer.buildings) {
            val bh = h * b.h
            val top = h - bh + off
            // Down to the bottom edge whatever the scroll, so a building never floats.
            drawRect(body, Offset(b.x, top), Size(b.w, h - top + 10))
            if (li == 2) drawRect(if (day) Color.White.copy(alpha = 0.12f) else Color(255, 120, 200).copy(alpha = 0.10f), Offset(b.x, top), Size(1.5f, bh))
            if (b.antenna) {
                val ax = b.x + b.w / 2
                val ay = top - 16 - li * 4
                drawLine(body, Offset(ax, top), Offset(ax, ay), strokeWidth = 1.5f)
                if (!day && sin(t * 2.4f + b.i) > 0.2f) {
                    drawCircle(Brush.radialGradient(listOf(Color(255, 70, 70).copy(alpha = 0.9f), Color(255, 70, 70).copy(alpha = 0f)), Offset(ax, ay), 7f), 7f, Offset(ax, ay))
                }
            }
            // Windows, each on its own slow on/off clock.
            val cols = max(2, floor(b.w / (layer.wh * 3)).toInt())
            val cw = b.w / cols
            val rows = floor(bh / (layer.wh * 3.2f)).toInt()
            for (r in 1 until rows) for (cc in 0 until cols) {
                val id = b.i * 997 + r * 31 + cc + li * 100000
                val x = b.x + cc * cw + cw * 0.3f
                val y = top + r * layer.wh * 3.2f
                val base = hash(id)
                val slow = hash(id + floor(t / (6 + base * 8)) * 3.1)
                var lit = when (phase) { Phase.NIGHT -> slow < 0.55f; Phase.DAWN -> slow < 0.18f; Phase.DAY -> false }
                if (!lit) for ((at, age) in taps) {
                    if (age < 6f && hypot(at.x - x, at.y - y) < age * 340) {
                        lit = true
                        break
                    }
                }
                if (lit) {
                    val hue = hash(id + 7)
                    val col = if (hue < 0.68f) WARM else if (hue < 0.9f) COOL else ROSE
                    drawRect(col.copy(alpha = (0.55f + 0.45f * layer.win) * if (day) 0.6f else 1f), Offset(x, y), Size(cw * 0.45f, layer.wh))
                } else if (li == 2) {
                    drawRect(Color.White.copy(alpha = if (day) 0.22f else 0.04f), Offset(x, y), Size(cw * 0.45f, layer.wh))
                }
            }
            // Vertical neon signs on the near buildings.
            if (li == 2 && b.sign) {
                val sx = b.x + b.w - 9
                val sy = top + bh * 0.12f
                val sh = min(70f, bh * 0.35f)
                val col = if (hash(b.i + 3) > 0.5f) Color(0xFFFF4FD8) else Color(0xFF3DF2FF)
                val on = !day && (sin(t * 0.7f + b.i * 5) > -0.92f || buzz) && !(buzz && sin(now * 60) > 0.3f)
                val a = if (on) (if (dawn) 0.6f else 1f) else 0.15f
                for ((width, alpha) in SIGN_GLOW) {
                    drawRect(col.copy(alpha = a * alpha), Offset(sx, sy), Size(7f, sh), style = Stroke(width))
                }
                for (gl in 0 until 4) {
                    drawRect(col.copy(alpha = a * 0.9f), Offset(sx + 2, sy + 6 + gl * (sh - 10) / 4), Size(3f, (sh - 10) / 4 - 4))
                }
            }
        }
    }

    // Street-level haze.
    val haze = if (day) Color(0xFFFFE9DD) else pink
    drawRect(
        Brush.verticalGradient(listOf(haze.copy(alpha = 0f), haze.copy(alpha = if (day) 0.35f else 0.18f)), startY = h * 0.8f, endY = h),
        size = Size(w, h),
    )
}

private val TAU = (2 * PI).toFloat()
private val WARM = Color(0xFFFFD27A)
private val COOL = Color(0xFFCDEBFF)
private val ROSE = Color(0xFFFF9AD5)
private val SIGN_GLOW = listOf(9f to 0.06f, 5f to 0.16f, 1.6f to 0.95f)

/** Where along the bottom, sweep period (s), phase. */
private val SEARCHLIGHTS = listOf(Triple(0.22f, 9f, 0f), Triple(0.74f, 13f, 2f))

private class Cloud(val y: Float, val depth: Float, val speed: Float, val seed: Float)

private val CLOUDS = listOf(Cloud(0.1f, 0.05f, 5f, 1f), Cloud(0.19f, 0.06f, 9f, 3f), Cloud(0.29f, 0.05f, 14f, 6f))

private class Building(val x: Float, val w: Float, val h: Float, val i: Int, val antenna: Boolean, val sign: Boolean)

/** A row of buildings across the prototype's 300-wide phone, from the same hashes as the prototype. */
private fun skyline(seed: Int, minH: Float, maxH: Float, wMin: Float, wMax: Float): List<Building> {
    val out = ArrayList<Building>()
    var x = -10f
    var i = 0
    while (x < PROTO_WIDTH + 10) {
        val bw = wMin + hash(seed + i) * (wMax - wMin)
        out += Building(
            x, bw, minH + hash(seed + i + 50) * (maxH - minH), i,
            antenna = hash(seed + i + 90) > 0.6f,
            sign = hash(seed + i + 120) > 0.72f && bw > 34,
        )
        x += bw + 2 + hash(seed + i + 70) * 4
        i++
    }
    return out
}

private const val PROTO_WIDTH = 300f

private class Layer(
    val buildings: List<Building>,
    val parallax: Float,
    val day: Long,
    val dawn: Long,
    val night: Long,
    /** How bright lit windows are: far ones dimmer. */
    val win: Float,
    /** Window height. */
    val wh: Float,
)

private val LAYERS = listOf(
    Layer(skyline(10, 0.16f, 0.30f, 16f, 30f), 0.06f, 0xFFD9A8B8, 0xFF4A1A5E, 0xFF24103F, 0.35f, 2f),
    Layer(skyline(40, 0.20f, 0.38f, 22f, 40f), 0.12f, 0xFFC08DA5, 0xFF300E48, 0xFF170833, 0.6f, 3f),
    Layer(skyline(80, 0.24f, 0.46f, 30f, 56f), 0.2f, 0xFF9C6F8C, 0xFF1C0730, 0xFF0C0420, 1f, 3.5f),
)
