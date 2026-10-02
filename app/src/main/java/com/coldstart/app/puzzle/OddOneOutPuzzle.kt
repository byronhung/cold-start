package com.coldstart.app.puzzle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coldstart.app.ui.components.springClick
import com.coldstart.app.ui.theme.ColdText
import com.coldstart.app.ui.theme.Sun
import kotlin.random.Random

/**
 * V2 hard: colour, shape and size. One shape is a combination nothing else has. A wrong tap
 * deals a new grid, so tapping every shape in turn doesn't get you through.
 */
@Composable
fun OddOneOutPuzzle(level: Int, onMiss: () -> Unit, onSolved: () -> Unit) {
    val rng = remember { Random(System.nanoTime()) }
    var spec by remember(level) { mutableStateOf(oddOneOutFor(level, rng)) }
    var misses by remember { mutableIntStateOf(0) }
    var finished by remember { mutableStateOf(false) }

    fun tap(index: Int) {
        if (finished) return
        if (index == spec.targetIndex) {
            finished = true
            onSolved()
        } else {
            misses++
            onMiss()
            spec = oddOneOutFor(level, rng)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PuzzlePrompt("Find the one that doesn't match")
        SquareGrid(size = spec.gridSize, gap = 4, modifier = Modifier.shakeOn(misses)) { i ->
            val shape = spec.items[i]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .springClick(0.82f) { tap(i) }
                    .semantics {
                        contentDescription = "${shape.size.name} ${shape.colour.name} ${shape.form.name}".lowercase()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .fillMaxSize(if (shape.size == ShapeSize.BIG) 0.82f else 0.46f)
                        .background(
                            shapeColour(shape.colour),
                            if (shape.form == ShapeForm.CIRCLE) CircleShape else RoundedCornerShape(percent = 18),
                        ),
                )
            }
        }
        Text("Colour, shape and size: only one is unique", style = ColdText.caption, color = Sun.OnGlass.copy(alpha = 0.6f))
        MissNote(misses, text = "Not that one. New grid.")
    }
}

/** Red and blue: they stay distinct under red-green colour blindness. */
private fun shapeColour(colour: ShapeColour): Color = when (colour) {
    ShapeColour.RED -> Sun.PuzzleRed
    ShapeColour.BLUE -> Sun.PuzzleBlue
}
