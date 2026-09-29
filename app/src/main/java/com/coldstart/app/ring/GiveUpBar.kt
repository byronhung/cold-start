package com.coldstart.app.ring

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.coldstart.app.ui.theme.ColdColors
import com.coldstart.app.ui.theme.ColdShapes
import com.coldstart.app.ui.theme.ColdText

const val GIVE_UP_HOLD_MS = 30_000L

/**
 * The escape hatch: hold for 30 seconds. Letting go empties it. Red is used here and nowhere else
 * outside the puzzles, because this is the one place the app costs you something.
 */
@Composable
fun GiveUpBar(onGiveUp: () -> Unit, modifier: Modifier = Modifier) {
    val latestOnGiveUp by rememberUpdatedState(onGiveUp)
    var pressed by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(pressed) {
        if (!pressed) {
            progress = 0f
            return@LaunchedEffect
        }
        val start = withFrameNanos { it }
        while (true) {
            val elapsedMs = (withFrameNanos { it } - start) / 1_000_000
            progress = (elapsedMs.toFloat() / GIVE_UP_HOLD_MS).coerceAtMost(1f)
            if (progress >= 1f) {
                latestOnGiveUp()
                break
            }
        }
    }

    val heldSeconds = (progress * GIVE_UP_HOLD_MS / 1000).toInt()
    val totalSeconds = (GIVE_UP_HOLD_MS / 1000).toInt()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(ColdShapes.pill)
            .border(1.dp, ColdColors.Line, ColdShapes.pill)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            }
            .semantics { contentDescription = "Hold for $totalSeconds seconds to give up" },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(ColdColors.Danger.copy(alpha = 0.25f)),
        )
        Text(
            if (pressed) "Hold to give up · $heldSeconds of $totalSeconds s" else "Hold to give up · $totalSeconds s",
            style = ColdText.caption,
            color = ColdColors.InkMute,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
