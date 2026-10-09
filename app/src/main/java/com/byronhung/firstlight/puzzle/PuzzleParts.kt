package com.byronhung.firstlight.puzzle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.byronhung.firstlight.ui.theme.Sun
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.byronhung.firstlight.ui.theme.AppText
import kotlinx.coroutines.delay

/** A square n×n grid that fits whatever space it's given. [cell] gets the index, row by row. */
@Composable
fun SquareGrid(
    size: Int,
    modifier: Modifier = Modifier,
    gap: Int = 6,
    cell: @Composable (index: Int) -> Unit,
) {
    Column(
        modifier = modifier.aspectRatio(1f),
        verticalArrangement = Arrangement.spacedBy(gap.dp),
    ) {
        repeat(size) { row ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap.dp),
            ) {
                repeat(size) { col ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) { cell(row * size + col) }
                }
            }
        }
    }
}

/**
 * "Not that one." for a moment after a wrong tap, then gone. Space is always reserved, so the
 * layout never jumps. [missCount] changing is what triggers it.
 */
@Composable
fun MissNote(missCount: Int, text: String = "Not that one.") {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(missCount) {
        if (missCount == 0) return@LaunchedEffect
        visible = true
        delay(1_200)
        visible = false
    }
    Box(Modifier.height(20.dp)) {
        Text(
            if (visible) text else "",
            style = AppText.caption,
            color = Sun.OnGlass.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

fun inkColour(ink: InkColour): Color = when (ink) {
    InkColour.RED -> Sun.PuzzleRed
    InkColour.BLUE -> Sun.PuzzleBlue
    InkColour.PINK -> Sun.PuzzlePink
    InkColour.YELLOW -> Sun.PuzzleYellow
}

/** The prompt above every puzzle. */
@Composable
fun PuzzlePrompt(text: String) {
    Text(text, style = AppText.prompt, color = Sun.OnGlass.copy(alpha = 0.8f), textAlign = TextAlign.Center)
}

/** Shakes sideways whenever [trigger] changes (a wrong tap), the prototype's 0.38 s shake. */
fun Modifier.shakeOn(trigger: Int): Modifier = composed {
    val x = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        for (target in listOf(-7f, 7f, -7f, 7f, 0f)) x.animateTo(target, tween(38))
    }
    graphicsLayer { translationX = x.value * density }
}
