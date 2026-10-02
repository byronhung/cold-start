package com.coldstart.app.puzzle

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coldstart.app.ui.components.springClick
import com.coldstart.app.ui.theme.ColdShapes
import com.coldstart.app.ui.theme.ColdText
import com.coldstart.app.ui.theme.Sun
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Cells glow amber for two seconds, then go dark: tap them back. A wrong tap clears your picks and
 * shows the same pattern again, so tapping every cell can't brute-force it.
 */
@Composable
fun PatternPuzzle(level: Int, onMiss: () -> Unit, onSolved: () -> Unit) {
    val rng = remember { Random(System.nanoTime()) }
    val spec = remember(level) { patternFor(level, rng) }
    val picked = remember { mutableStateListOf<Int>() }
    var showing by remember { mutableStateOf(true) }
    var shows by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(shows) {
        showing = true
        delay(SHOW_MS)
        showing = false
    }

    fun tap(index: Int) {
        if (showing || finished) return
        if (index in spec.lit) {
            if (index !in picked) picked += index
            if (picked.size == spec.lit.size) {
                finished = true
                onSolved()
            }
        } else {
            misses++
            onMiss()
            picked.clear()
            shows++
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PuzzlePrompt(if (showing) "Remember these" else "Tap the ones that lit up")
        SquareGrid(
            size = spec.gridSize,
            gap = 10,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .shakeOn(misses),
        ) { i ->
            val on = (showing && i in spec.lit) || i in picked
            val glow by animateFloatAsState(if (on) 1f else 0f, tween(250), label = "cell")
            Box(
                Modifier
                    .fillMaxSize()
                    .springClick(0.9f, enabled = !showing) { tap(i) }
                    .shadow((14 * glow).dp, ColdShapes.small, ambientColor = Sun.AmberLight, spotColor = Sun.AmberLight)
                    .clip(ColdShapes.small)
                    .background(Color.White.copy(alpha = 0.08f))
                    .drawBehind { drawRect(Sun.amberBrush, alpha = glow) }
                    .semantics { contentDescription = "Cell ${i + 1}" },
            )
        }
        Text(
            if (showing) "Watch…" else "${picked.size} of ${spec.lit.size}",
            style = ColdText.caption.copy(fontSize = ColdText.label.fontSize * 1.1f),
            color = Sun.OnGlass.copy(alpha = 0.6f),
        )
        MissNote(misses, text = "Not that one. Watch again.")
    }
}

private const val SHOW_MS = 2_000L
