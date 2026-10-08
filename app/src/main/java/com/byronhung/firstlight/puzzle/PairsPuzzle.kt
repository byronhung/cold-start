package com.byronhung.firstlight.puzzle

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.byronhung.firstlight.ui.components.LogoMark
import com.byronhung.firstlight.ui.components.springClick
import com.byronhung.firstlight.ui.theme.AppText
import com.byronhung.firstlight.ui.theme.Sun
import kotlinx.coroutines.delay
import kotlin.random.Random

private val CardShape = RoundedCornerShape(16.dp)

private val SHAPE_NAMES = listOf("red circle", "blue square", "green triangle", "yellow diamond", "amber ring", "pink plus")
private val SHAPE_COLOURS = listOf(Sun.PuzzleRed, Sun.PuzzleBlue, Sun.PuzzleGreen, Sun.PuzzleYellow, Sun.Amber, Color(0xFFFF8FC8))

/**
 * Memory pairs: six pairs face down, find [pairsToFind] of them. Cards flip with a turn; a
 * mismatch turns back after a moment. Finding only some of the pairs keeps the search real (luck
 * can't do it) and the round about as long as the other puzzles.
 */
@Composable
fun PairsPuzzle(level: Int, onSolved: () -> Unit) {
    val rng = remember { Random(System.nanoTime()) }
    val deck = remember { dealPairs(rng) }
    val target = remember(level) { pairsToFind(level) }
    val up = remember { mutableStateListOf<Int>() }
    val found = remember { mutableStateListOf<Int>() }
    var busy by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    // A mismatch stays visible long enough to remember, then turns back.
    LaunchedEffect(busy) {
        if (!busy) return@LaunchedEffect
        delay(750)
        up.clear()
        busy = false
    }

    fun flip(i: Int) {
        if (busy || finished || i in up || i in found) return
        up += i
        if (up.size < 2) return
        val (a, b) = up[0] to up[1]
        if (deck[a] == deck[b]) {
            found += listOf(a, b)
            up.clear()
            if (found.size / 2 >= target) {
                finished = true
                onSolved()
            }
        } else {
            busy = true
        }
    }

    val left = (target - found.size / 2).coerceAtLeast(0)
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PuzzlePrompt("Find any $target matching pairs")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            deck.indices.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { i ->
                        Card(
                            shape = deck[i],
                            faceUp = i in up || i in found,
                            matched = i in found,
                            onTap = { flip(i) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        Text(
            if (left == 0) "Done" else "$left ${if (left == 1) "pair" else "pairs"} to go",
            style = AppText.caption,
            color = Sun.OnGlass.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun Card(shape: Int, faceUp: Boolean, matched: Boolean, onTap: () -> Unit, modifier: Modifier) {
    val turn by animateFloatAsState(if (faceUp) 180f else 0f, spring(dampingRatio = 0.7f, stiffness = 300f), label = "flip")
    val showingFace = turn > 90f
    Box(
        modifier
            .aspectRatio(1f)
            .springClick(0.92f, onClick = onTap)
            .graphicsLayer {
                rotationY = turn
                cameraDistance = 12f * density
            }
            .clip(CardShape)
            .background(
                when {
                    matched -> Color(0xFFFFE1B0).copy(alpha = 0.18f)
                    showingFace -> Sun.OnGlass.copy(alpha = 0.1f)
                    else -> Color.White.copy(alpha = 0.08f)
                },
            )
            .border(1.dp, if (matched) Sun.Glow.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.16f), CardShape)
            .semantics { contentDescription = if (faceUp) SHAPE_NAMES[shape] else "Face-down card" },
        contentAlignment = Alignment.Center,
    ) {
        if (showingFace) {
            // Drawn mirrored back, so the shape reads the right way round after the flip.
            Canvas(
                Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .graphicsLayer { rotationY = 180f },
            ) { drawShape(shape, SHAPE_COLOURS[shape]) }
        } else {
            LogoMark(Sun.OnGlass.copy(alpha = 0.35f), size = 22.dp)
        }
    }
}

private fun DrawScope.drawShape(shape: Int, colour: Color) {
    val w = size.width
    val h = size.height
    when (shape) {
        0 -> drawCircle(colour, radius = w * 0.42f)
        1 -> drawRect(colour, Offset(w * 0.12f, h * 0.12f), Size(w * 0.76f, h * 0.76f))
        2 -> drawPath(Path().apply { moveTo(w / 2, h * 0.08f); lineTo(w * 0.94f, h * 0.88f); lineTo(w * 0.06f, h * 0.88f); close() }, colour)
        3 -> drawPath(Path().apply { moveTo(w / 2, 0f); lineTo(w, h / 2); lineTo(w / 2, h); lineTo(0f, h / 2); close() }, colour)
        4 -> drawCircle(colour, radius = w * 0.33f, style = Stroke(w * 0.17f))
        else -> {
            drawRect(colour, Offset(w * 0.36f, h * 0.08f), Size(w * 0.28f, h * 0.84f))
            drawRect(colour, Offset(w * 0.08f, h * 0.36f), Size(w * 0.84f, h * 0.28f))
        }
    }
}
