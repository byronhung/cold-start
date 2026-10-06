package com.coldstart.app.puzzle

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coldstart.app.ui.theme.ColdText
import com.coldstart.app.ui.theme.Sun
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

private val BlockShape = RoundedCornerShape(14.dp)

/**
 * Slide out, the simple wiSlide: tap a block on the end you want it to go, and it slides that way
 * as far as it can. Get the amber block out through the gap on the right. Boards come from
 * [slideFor], which only returns boards it has solved, so there are no dead ends.
 */
@Composable
fun SlidePuzzle(level: Int, onSolved: () -> Unit) {
    val rng = remember { Random(System.nanoTime()) }
    // Built off the main thread: a hard board can take a moment to find, and the screen must not stall.
    var board by remember(level) { mutableStateOf<SlideBoard?>(null) }
    val context = LocalContext.current
    LaunchedEffect(level) {
        board = withContext(Dispatchers.Default) {
            val bank = runCatching {
                context.assets.open("slide_bank_$level.txt").bufferedReader().readLines().filter { it.isNotBlank() }
            }.getOrDefault(emptyList())
            slideFor(level, rng, bank)
        }
    }
    var finished by remember { mutableStateOf(false) }
    var moves by remember { mutableIntStateOf(0) }

    LaunchedEffect(finished) {
        if (finished) {
            // Let the key slide out of the gap before the round moves on.
            kotlinx.coroutines.delay(450)
            onSolved()
        }
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PuzzlePrompt("Tap a block toward where it should go. Get the amber block out.")
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val boardSide = minOf(maxWidth - 24.dp, 300.dp)
            val n = board?.size ?: 4
            val exitRow = board?.exitRow ?: 1
            val cell = boardSide / n
            Box(
                Modifier
                    .size(boardSide)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(20.dp)),
            ) {
                board?.blocks?.forEach { b ->
                    key(b.id) {
                        BlockView(b, cell, out = finished && b.key) { towardEnd ->
                            if (finished) return@BlockView false
                            val next = board?.slide(b.id, if (towardEnd) 1 else -1) ?: return@BlockView false
                            board = next
                            moves++
                            if (next.isSolved) finished = true
                            true
                        }
                    }
                }
            }
            // The exit: a glowing gap on the right edge of the key's row.
            Box(
                Modifier
                    .align(Alignment.Center)
                    .offset(x = boardSide / 2 + 2.dp, y = -boardSide / 2 + cell * exitRow + cell / 2)
                    .size(width = 8.dp, height = cell - 12.dp)
                    .shadow(12.dp, RoundedCornerShape(4.dp), ambientColor = Sun.AmberLight, spotColor = Sun.AmberLight)
                    .background(Sun.Glow, RoundedCornerShape(4.dp)),
            )
        }
        Text(
            if (finished) "Out!" else "$moves ${if (moves == 1) "move" else "moves"}",
            style = ColdText.caption,
            color = Sun.OnGlass.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * One block. [onSlide] gets whether the tap was on its far end (right or bottom) and returns
 * whether it moved; a block that can't move gives a small nudge instead.
 */
@Composable
private fun BlockView(b: Block, cell: Dp, out: Boolean, onSlide: (towardEnd: Boolean) -> Boolean) {
    val pad = 4.dp
    val x by animateDpAsState(cell * (if (out) b.x + 3 else b.x) + pad, spring(dampingRatio = 0.7f, stiffness = 400f), label = "x")
    val y by animateDpAsState(cell * b.y + pad, spring(dampingRatio = 0.7f, stiffness = 400f), label = "y")
    val nudge = remember { Animatable(0f) }
    var nudges by remember { mutableIntStateOf(0) }
    LaunchedEffect(nudges) {
        if (nudges == 0) return@LaunchedEffect
        for (t in listOf(-4f, 4f, -2f, 0f)) nudge.animateTo(t, tween(60))
    }
    val w = cell * b.w - pad * 2
    val h = cell * b.h - pad * 2
    Box(
        Modifier
            .offset(x = x, y = y)
            .size(w, h)
            .graphicsLayer {
                if (b.horizontal) translationX = nudge.value * density else translationY = nudge.value * density
            }
            .then(if (b.key) Modifier.shadow(10.dp, BlockShape, ambientColor = Sun.Amber, spotColor = Sun.Amber) else Modifier)
            .clip(BlockShape)
            .background(if (b.key) Sun.amberBrush else androidx.compose.ui.graphics.SolidColor(Sun.OnGlass.copy(alpha = 0.2f)))
            .border(1.dp, if (b.key) Color.Transparent else Color.White.copy(alpha = 0.22f), BlockShape)
            .pointerInput(b.id, b.x, b.y) {
                detectTapGestures { pos ->
                    val towardEnd = if (b.horizontal) pos.x > size.width / 2 else pos.y > size.height / 2
                    if (!onSlide(towardEnd)) nudges++
                }
            }
            .semantics {
                contentDescription = (if (b.key) "Amber block" else "Block") +
                    if (b.horizontal) ", slides left and right" else ", slides up and down"
            },
        contentAlignment = Alignment.Center,
    ) {
        // A grip line along the block's track: a hint at which way it moves.
        Box(
            Modifier
                .size(if (b.horizontal) 18.dp else 4.dp, if (b.horizontal) 4.dp else 18.dp)
                .background(if (b.key) Sun.OnAmber.copy(alpha = 0.5f) else Sun.OnGlass.copy(alpha = 0.45f), RoundedCornerShape(2.dp)),
        )
    }
}
