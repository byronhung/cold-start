package com.coldstart.app.ring

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coldstart.app.alarm.clockDigits
import com.coldstart.app.alarm.formatDuration
import com.coldstart.app.alarm.period
import com.coldstart.app.data.RoundType
import com.coldstart.app.data.WakeOutcome
import com.coldstart.app.puzzle.Levels
import com.coldstart.app.puzzle.OddOneOutPuzzle
import com.coldstart.app.puzzle.PatternPuzzle
import com.coldstart.app.puzzle.QrPuzzle
import com.coldstart.app.puzzle.StroopPuzzle
import com.coldstart.app.ui.theme.ColdColors
import com.coldstart.app.ui.theme.ColdShapes
import com.coldstart.app.ui.theme.ColdText
import com.coldstart.app.ui.theme.Space
import kotlinx.coroutines.delay
import java.time.LocalTime

private sealed interface Ending {
    data class Solved(val totalMs: Long) : Ending
    data object GaveUp : Ending
}

@Composable
fun RingRoute(controller: RingController, is24Hour: Boolean, onClose: () -> Unit) {
    val state by controller.state.collectAsStateWithLifecycle()
    // Set the moment the ring ends here, so the closing screen shows instead of an instant exit.
    var ending by remember { mutableStateOf<Ending?>(null) }

    val finished = ending
    when {
        finished != null -> EndScreen(finished, onClose)
        state is RingState.Ringing -> RingScreen(
            session = (state as RingState.Ringing).session,
            is24Hour = is24Hour,
            onSolved = { results, totalMs ->
                ending = Ending.Solved(totalMs)
                controller.finish(WakeOutcome.SOLVED, results)
            },
            onGiveUp = { results ->
                ending = Ending.GaveUp
                controller.finish(WakeOutcome.GAVE_UP, results)
            },
        )
        state == RingState.Starting -> Blank()
        else -> {
            // Idle. The alarm receiver can open this screen a moment before the service reports
            // Starting, so wait briefly before deciding nothing is ringing.
            Blank()
            LaunchedEffect(Unit) {
                delay(3_000)
                onClose()
            }
        }
    }
}

@Composable
private fun Blank() {
    Box(
        Modifier
            .fillMaxSize()
            .background(ColdColors.Ground),
    )
}

/**
 * The round harness: the same frame for every puzzle. Clock and progress at the top, the puzzle in
 * the middle, the give-up bar at the bottom. Each puzzle only draws the middle and reports
 * onMiss / onSolved; timing and results are kept here.
 */
@Composable
private fun RingScreen(
    session: RingSession,
    is24Hour: Boolean,
    onSolved: (List<RoundResultDraft>, Long) -> Unit,
    onGiveUp: (List<RoundResultDraft>) -> Unit,
) {
    val rounds = session.rounds
    var index by remember { mutableIntStateOf(0) }
    val results = remember { mutableStateListOf<RoundResultDraft>() }
    val firstStart = remember { SystemClock.elapsedRealtime() }
    var roundStart by remember { mutableLongStateOf(firstStart) }
    var misses by remember { mutableIntStateOf(0) }

    val now by produceState(LocalTime.now()) {
        while (true) {
            delay(1_000 - System.currentTimeMillis() % 1_000)
            value = LocalTime.now()
        }
    }

    fun roundSolved() {
        val type = rounds[index]
        val t = SystemClock.elapsedRealtime()
        results += RoundResultDraft(type, levelOf(session, type), t - roundStart, misses)
        misses = 0
        roundStart = t
        if (index == rounds.lastIndex) onSolved(results.toList(), t - firstStart) else index++
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColdColors.Ground)
            .safeDrawingPadding()
            .padding(horizontal = Space.screen, vertical = Space.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(clockDigits(now.hour, now.minute, is24Hour), style = ColdText.clock, color = ColdColors.Ink)
            if (!is24Hour) {
                Text(
                    period(now.hour),
                    style = ColdText.label.copy(fontSize = 16.sp),
                    color = ColdColors.Ink,
                    modifier = Modifier.padding(start = 6.dp, bottom = 10.dp),
                )
            }
        }
        Text(
            session.label.ifBlank { "Alarm" },
            style = ColdText.caption,
            color = ColdColors.InkMute,
        )
        Spacer(Modifier.height(Space.lg))
        Pips(done = index, total = rounds.size)
        Spacer(Modifier.height(Space.sm))
        Text("ROUND ${index + 1} OF ${rounds.size}", style = ColdText.label, color = ColdColors.InkMute)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = Space.lg),
            contentAlignment = Alignment.Center,
        ) {
            // key: each round gets a fresh puzzle with fresh state.
            key(index) {
                val type = rounds[index]
                val level = levelOf(session, type)
                val onMiss: () -> Unit = { misses++ }
                when (type) {
                    RoundType.STROOP -> StroopPuzzle(level, onMiss, ::roundSolved)
                    RoundType.PATTERN_FLASH -> PatternPuzzle(level, onMiss, ::roundSolved)
                    RoundType.ODD_ONE_OUT -> OddOneOutPuzzle(level, onMiss, ::roundSolved)
                    RoundType.QR_SCAN -> QrPuzzle(session.qrCode.orEmpty(), onMiss, ::roundSolved)
                }
            }
        }

        GiveUpBar(onGiveUp = { onGiveUp(results.toList()) })
    }
}

private fun levelOf(session: RingSession, type: RoundType): Int =
    if (type == RoundType.QR_SCAN) 0 else session.levels[type] ?: Levels.DEFAULT

@Composable
private fun Pips(done: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            val colour = when {
                i < done -> ColdColors.Accent
                i == done -> ColdColors.Ink
                else -> ColdColors.Line
            }
            Box(
                Modifier
                    .size(width = 32.dp, height = 5.dp)
                    .background(colour, ColdShapes.pill),
            )
        }
    }
}

/** The one-line summary after the sound stops. Tap anywhere, or it closes itself. */
@Composable
private fun EndScreen(ending: Ending, onClose: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(6_000)
        onClose()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColdColors.Ground)
            .clickable(onClick = onClose)
            .safeDrawingPadding()
            .padding(Space.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Space.sm, Alignment.CenterVertically),
    ) {
        when (ending) {
            is Ending.Solved -> {
                Text("Good morning", style = ColdText.title, color = ColdColors.Ink)
                Text("Solved in ${formatDuration(ending.totalMs)}", style = ColdText.prompt, color = ColdColors.Accent)
            }
            Ending.GaveUp -> {
                Text("Alarm stopped", style = ColdText.title, color = ColdColors.Ink)
                Text("Logged as a give-up.", style = ColdText.prompt, color = ColdColors.InkDim)
            }
        }
        Text("Tap to close", style = ColdText.caption, color = ColdColors.InkMute, modifier = Modifier.padding(top = Space.xl))
    }
}
