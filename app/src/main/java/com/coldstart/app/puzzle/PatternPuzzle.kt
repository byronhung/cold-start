package com.coldstart.app.puzzle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import com.coldstart.app.ui.theme.ColdColors
import com.coldstart.app.ui.theme.ColdShapes
import com.coldstart.app.ui.theme.ColdText
import com.coldstart.app.ui.theme.Space
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Cells light for two seconds, then go dark: tap them back. A wrong tap clears your picks and
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
        verticalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        Text(
            if (showing) "Remember these" else "Tap the ones that lit up",
            style = ColdText.prompt,
            color = ColdColors.InkDim,
        )
        SquareGrid(size = spec.gridSize, gap = 10, modifier = Modifier.padding(horizontal = Space.lg)) { i ->
            val on = (showing && i in spec.lit) || i in picked
            Box(
                Modifier
                    .fillMaxSize()
                    .background(if (on) ColdColors.Accent else ColdColors.Surface, ColdShapes.small)
                    .border(1.dp, if (on) ColdColors.Accent else ColdColors.Line, ColdShapes.small)
                    .clickable(enabled = !showing) { tap(i) },
            )
        }
        MissNote(misses, text = "Not that one. Watch again.")
    }
}

private const val SHOW_MS = 2_000L
