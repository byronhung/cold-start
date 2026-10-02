package com.coldstart.app.ring

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coldstart.app.alarm.WakeCheck
import com.coldstart.app.alarm.clockDigits
import com.coldstart.app.alarm.formatDuration
import com.coldstart.app.alarm.formatTime
import com.coldstart.app.alarm.period
import com.coldstart.app.data.RoundType
import com.coldstart.app.data.WakeOutcome
import com.coldstart.app.puzzle.Levels
import com.coldstart.app.puzzle.OddOneOutPuzzle
import com.coldstart.app.puzzle.PatternPuzzle
import com.coldstart.app.puzzle.QrPuzzle
import com.coldstart.app.puzzle.StroopPuzzle
import com.coldstart.app.ui.components.HoldToGiveUp
import com.coldstart.app.ui.components.Pips
import com.coldstart.app.ui.components.SkyBackground
import com.coldstart.app.ui.components.springClick
import com.coldstart.app.ui.theme.ColdShapes
import com.coldstart.app.ui.theme.ColdText
import com.coldstart.app.ui.theme.Motion
import com.coldstart.app.ui.theme.Skies
import com.coldstart.app.ui.theme.Sun
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime

const val GIVE_UP_HOLD_MS = 30_000L

private sealed interface Ending {
    data class Solved(val totalMs: Long, val checks: Int) : Ending
    data object GaveUp : Ending
}

@Composable
fun RingRoute(controller: RingController, is24Hour: Boolean, onClose: () -> Unit) {
    val state by controller.state.collectAsStateWithLifecycle()
    // Set the moment the ring ends here, so the closing screen shows instead of an instant exit.
    var ending by remember { mutableStateOf<Ending?>(null) }

    SkyBackground(Skies.Dawn, drift = true) {
        val finished = ending
        when {
            finished != null -> EndScreen(finished, is24Hour, onClose)
            state is RingState.Ringing -> {
                val session = (state as RingState.Ringing).session
                RingScreen(
                    session = session,
                    is24Hour = is24Hour,
                    onSolved = { results, totalMs ->
                        ending = Ending.Solved(totalMs, session.wakeChecks)
                        controller.finish(WakeOutcome.SOLVED, results)
                    },
                    onGiveUp = { results ->
                        ending = Ending.GaveUp
                        controller.finish(WakeOutcome.GAVE_UP, results)
                    },
                )
            }
            state == RingState.Starting -> Unit
            else -> LaunchedEffect(Unit) {
                // Idle. The alarm receiver can open this screen a moment before the service reports
                // Starting, so wait briefly before deciding nothing is ringing.
                delay(3_000)
                onClose()
            }
        }
    }
}

/**
 * The round harness: the same frame for every puzzle. Clock and round markers at the top, the
 * puzzle card in the middle (each new round slides in), the give-up ring at the bottom. Puzzles
 * only report onMiss / onSolved; timing and results are kept here.
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
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(start = 24.dp, end = 24.dp, top = 30.dp, bottom = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(clockDigits(now.hour, now.minute, is24Hour), style = ColdText.ringClock, color = Sun.OnGlass)
            if (!is24Hour) {
                Text(period(now.hour), style = ColdText.period.copy(fontSize = 20.sp), color = Sun.OnGlass.copy(alpha = 0.75f), modifier = Modifier.padding(bottom = 10.dp))
            }
        }
        Text(session.label.ifBlank { "Alarm" }, style = ColdText.caption.copy(fontSize = 15.sp), color = Sun.OnGlass.copy(alpha = 0.75f))
        Spacer(Modifier.height(20.dp))
        // A scan-only alarm is one step, not "round 1 of 1".
        val scanOnly = rounds == listOf(RoundType.QR_SCAN)
        if (!scanOnly) {
            Pips(done = index, total = rounds.size)
            Spacer(Modifier.height(10.dp))
        }
        Text(
            if (scanOnly) "SCAN YOUR CODE TO STOP IT" else "ROUND ${index + 1} OF ${rounds.size}",
            style = ColdText.label.copy(letterSpacing = 0.18.em),
            color = Sun.OnGlass.copy(alpha = 0.7f),
        )

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    (slideInHorizontally(Motion.settle()) { it / 6 } + fadeIn(tween(300)) + scaleIn(Motion.settle(), initialScale = 0.96f))
                        .togetherWith(fadeOut(tween(150)))
                },
                label = "round",
            ) { i ->
                PuzzleCard {
                    val type = rounds[i]
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
        }

        HoldToGiveUp(GIVE_UP_HOLD_MS, onGiveUp = { onGiveUp(results.toList()) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp))
    }
}

/** Dark glass, so the puzzle colours read on a bright dawn. */
@Composable
private fun PuzzleCard(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                shape = ColdShapes.puzzle
                clip = true
            }
            .background(Sun.PuzzleGlass)
            .drawBehind {
                drawRoundRect(Color.White.copy(alpha = 0.14f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(30.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            }
            .padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 18.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

private fun levelOf(session: RingSession, type: RoundType): Int =
    if (type == RoundType.QR_SCAN) 0 else session.levels[type] ?: Levels.DEFAULT

/** "Good morning" in a sunburst, or the give-up note. Tap anywhere, or it closes itself. */
@Composable
private fun EndScreen(ending: Ending, is24Hour: Boolean, onClose: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(8_000)
        onClose()
    }
    val halo = remember { Animatable(0f) }
    val rays = remember { Animatable(0f) }
    val text = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { halo.animateTo(1f, Motion.bouncy()) }
        launch { rays.animateTo(1f, tween(1_200, easing = Motion.softOut)) }
        delay(350)
        text.animateTo(1f, tween(700, easing = Motion.softOut))
    }

    Box(
        Modifier
            .fillMaxSize()
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose),
    ) {
        if (ending is Ending.Solved) {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val c = Offset(size.width / 2, size.height * 0.4f)
                        // Twelve rays growing out from the sun.
                        for (k in 0 until 12) {
                            val a = Math.toRadians(k * 30.0 - 90.0)
                            val inner = 50.dp.toPx()
                            val outer = inner + 120.dp.toPx() * rays.value
                            drawLine(
                                Brush.linearGradient(
                                    listOf(Color.Transparent, Sun.Glow),
                                    start = Offset(c.x + (inner * Math.cos(a)).toFloat(), c.y + (inner * Math.sin(a)).toFloat()),
                                    end = Offset(c.x + (outer * Math.cos(a)).toFloat(), c.y + (outer * Math.sin(a)).toFloat()),
                                ),
                                start = Offset(c.x + (inner * Math.cos(a)).toFloat(), c.y + (inner * Math.sin(a)).toFloat()),
                                end = Offset(c.x + (outer * Math.cos(a)).toFloat(), c.y + (outer * Math.sin(a)).toFloat()),
                                strokeWidth = 4.dp.toPx(),
                                cap = StrokeCap.Round,
                                alpha = rays.value,
                            )
                        }
                        drawCircle(
                            Brush.radialGradient(
                                0f to Color(0xFFFFE6BC), 0.5f to Color(0x80FFAA6E), 0.7f to Color.Transparent,
                                center = c, radius = 110.dp.toPx() * halo.value.coerceAtLeast(0.01f),
                            ),
                            radius = 110.dp.toPx() * halo.value.coerceAtLeast(0f),
                            center = c,
                        )
                    },
            )
        }
        Column(
            Modifier
                .align(if (ending is Ending.Solved) Alignment.BottomCenter else Alignment.Center)
                .safeDrawingPadding()
                .padding(start = 28.dp, end = 28.dp, bottom = 70.dp)
                .graphicsLayer {
                    alpha = text.value
                    translationY = (1f - text.value) * 24.dp.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when (ending) {
                is Ending.Solved -> {
                    Text("Good morning", style = ColdText.display.copy(fontSize = 54.sp), color = Sun.OnGlass)
                    Text("Solved in ${formatDuration(ending.totalMs)}", style = ColdText.bodyStrong.copy(fontSize = 18.sp), color = Sun.Glow)
                    if (ending.checks > 0) {
                        val at = LocalTime.now().plusSeconds(WakeCheck.delayMs / 1000)
                        Text(
                            if (ending.checks == 1) "Wake check at ${formatTime(at.hour, at.minute, is24Hour)}."
                            else "First wake check at ${formatTime(at.hour, at.minute, is24Hour)}. ${ending.checks} checks, five minutes apart.",
                            style = ColdText.body.copy(fontSize = 15.sp),
                            color = Sun.OnGlass.copy(alpha = 0.82f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 290.dp),
                        )
                    }
                }
                Ending.GaveUp -> {
                    Text("Alarm stopped", style = ColdText.display.copy(fontSize = 48.sp), color = Sun.OnGlass)
                    Text("Logged as a give-up in History.", style = ColdText.body, color = Sun.OnGlass.copy(alpha = 0.82f))
                }
            }
            Box(
                Modifier
                    .padding(top = 18.dp)
                    .springClick(0.92f, onClick = onClose)
                    .height(56.dp)
                    .graphicsLayer {
                        shape = ColdShapes.button
                        clip = true
                    }
                    .background(Color.White.copy(alpha = 0.92f))
                    .padding(horizontal = 34.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Close", style = ColdText.button, color = Sun.OnAmber) }
        }
    }
}
