package com.coldstart.app.puzzle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coldstart.app.ui.theme.ColdColors
import com.coldstart.app.ui.theme.ColdShapes
import com.coldstart.app.ui.theme.ColdText
import com.coldstart.app.ui.theme.Space
import kotlin.random.Random

/**
 * The word says one colour, the ink is another: tap the ink. A round is 2–4 words depending on
 * level. A wrong tap deals a fresh word, so mashing the four buttons doesn't get you through.
 */
@Composable
fun StroopPuzzle(level: Int, onMiss: () -> Unit, onSolved: () -> Unit) {
    val rng = remember { Random(System.nanoTime()) }
    val total = remember(level) { stroopWordCount(level) }
    val swatches = remember { InkColour.entries.shuffled(rng) }
    var word by remember { mutableStateOf(stroopWord(rng)) }
    var cleared by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    fun tap(colour: InkColour) {
        if (finished) return
        if (colour == word.ink) {
            if (cleared + 1 == total) {
                finished = true
                onSolved()
            } else {
                cleared++
                word = stroopWord(rng)
            }
        } else {
            misses++
            onMiss()
            word = stroopWord(rng)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        Text("Tap the ink colour", style = ColdText.prompt, color = ColdColors.InkDim)
        Text(word.word.name, style = ColdText.stroopWord, color = inkColour(word.ink))
        Text("${cleared + 1} OF $total", style = ColdText.label, color = ColdColors.InkMute)
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm), modifier = Modifier.fillMaxWidth()) {
            swatches.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(Space.sm), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { colour ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(68.dp)
                                .background(ColdColors.Surface, ColdShapes.small)
                                .border(1.dp, ColdColors.Line, ColdShapes.small)
                                .clickable { tap(colour) }
                                .semantics { contentDescription = colour.name.lowercase() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .background(inkColour(colour), CircleShape),
                            )
                        }
                    }
                }
            }
        }
        MissNote(misses)
    }
}
