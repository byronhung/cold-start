package com.coldstart.app.ring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coldstart.app.alarm.formatTime
import com.coldstart.app.data.WakeOutcome
import com.coldstart.app.ui.theme.ColdColors
import com.coldstart.app.ui.theme.ColdShapes
import com.coldstart.app.ui.theme.ColdText
import com.coldstart.app.ui.theme.Space

@Composable
fun RingRoute(controller: RingController, is24Hour: Boolean, onClose: () -> Unit) {
    val state by controller.state.collectAsStateWithLifecycle()
    when (val s = state) {
        is RingState.Ringing -> TemporaryStopScreen(s.session, is24Hour) {
            controller.finish(WakeOutcome.SOLVED, emptyList())
        }
        RingState.Starting -> Box(
            Modifier
                .fillMaxSize()
                .background(ColdColors.Ground),
        )
        RingState.Idle -> LaunchedEffect(Unit) { onClose() }
    }
}

/** Chunk 03 only: proves the ring works end to end. Chunk 05 replaces it with the puzzles. */
@Composable
private fun TemporaryStopScreen(session: RingSession, is24Hour: Boolean, onStop: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColdColors.Ground)
            .safeDrawingPadding()
            .padding(Space.screen),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(formatTime(session.hour, session.minute, is24Hour), style = ColdText.clock, color = ColdColors.Ink)
        Text(session.rounds.joinToString(" · "), style = ColdText.label, color = ColdColors.InkMute)
        Button(
            onClick = onStop,
            colors = ButtonDefaults.buttonColors(containerColor = ColdColors.Accent, contentColor = ColdColors.Ground),
            shape = ColdShapes.small,
            modifier = Modifier
                .padding(top = Space.xxl)
                .fillMaxWidth()
                .height(56.dp),
        ) { Text("Stop", style = ColdText.prompt) }
    }
}
