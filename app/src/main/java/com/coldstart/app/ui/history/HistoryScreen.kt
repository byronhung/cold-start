package com.coldstart.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.coldstart.app.alarm.formatDuration
import com.coldstart.app.alarm.formatTime
import com.coldstart.app.data.AlarmRepository
import com.coldstart.app.data.WakeLog
import com.coldstart.app.data.WakeOutcome
import com.coldstart.app.ui.components.GlassCard
import com.coldstart.app.ui.components.IconSquareButton
import com.coldstart.app.ui.components.SectionLabel
import com.coldstart.app.ui.components.SkyBackground
import com.coldstart.app.ui.components.SunIcons
import com.coldstart.app.ui.theme.ColdShapes
import com.coldstart.app.ui.theme.ColdText
import com.coldstart.app.ui.theme.LocalSky
import com.coldstart.app.ui.theme.Skies
import com.coldstart.app.ui.theme.Space
import com.coldstart.app.ui.theme.Sun
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class WakeRowUi(val id: Long, val whenText: String, val outcomeText: String, val outcome: WakeOutcome?, val checksText: String?)

data class HistoryUi(val rows: List<WakeRowUi>, val summary: String)

class HistoryViewModel(repository: AlarmRepository, private val is24Hour: Boolean) : ViewModel() {
    private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)

    val ui: StateFlow<HistoryUi?> = repository.recentWakes.map { wakes ->
        HistoryUi(rows = wakes.map(::row), summary = summary(wakes))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private fun row(w: WakeLog): WakeRowUi {
        val fired = Instant.ofEpochMilli(w.firedAt).atZone(ZoneId.systemDefault())
        val took = w.endedAt?.let { formatDuration(it - w.firedAt) }
        val outcome = when (w.outcome) {
            WakeOutcome.SOLVED -> "Solved · up in $took"
            WakeOutcome.NO_SCAN -> "Solved without the scan · up in $took"
            WakeOutcome.GAVE_UP -> "Gave up after $took"
            WakeOutcome.TIMED_OUT -> "Nobody answered for an hour"
            null -> "Interrupted"
        }
        val checks = when {
            w.checksMissed > 0 -> "Fell back asleep · rang again" + if (w.checksMissed > 1) " ×${w.checksMissed}" else ""
            w.checksPassed == 1 -> "1 wake check passed"
            w.checksPassed > 1 -> "${w.checksPassed} wake checks passed"
            else -> null
        }
        return WakeRowUi(w.id, "${dayFormat.format(fired)} · ${formatTime(fired.hour, fired.minute, is24Hour)}", outcome, w.outcome, checks)
    }

    private fun summary(wakes: List<WakeLog>): String {
        if (wakes.isEmpty()) return "Nothing yet. Your first morning shows up here."
        val solved = wakes.count { it.outcome == WakeOutcome.SOLVED }
        val noScan = wakes.count { it.outcome == WakeOutcome.NO_SCAN }
        val gaveUp = wakes.count { it.outcome == WakeOutcome.GAVE_UP }
        val noScanText = if (noScan > 0) " · $noScan no scan" else ""
        return "Last ${wakes.size}: $solved solved$noScanText · $gaveUp gave up"
    }
}

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    SkyBackground(Skies.Night) {
        val sky = LocalSky.current
        LazyColumn(
            modifier = Modifier.safeDrawingPadding(),
            contentPadding = PaddingValues(start = Space.screen, end = Space.screen, top = 24.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    Modifier.padding(start = 4.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    IconSquareButton(SunIcons.Back, "Back", onBack)
                    Column {
                        Text("History", style = ColdText.title, color = sky.ink)
                        Text(ui?.summary.orEmpty(), style = ColdText.caption, color = sky.dim)
                    }
                }
            }
            items(ui?.rows.orEmpty(), key = { it.id }) { row ->
                GlassCard(Modifier.fillMaxWidth(), shape = ColdShapes.button) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SectionLabel(row.whenText)
                        Text(row.outcomeText, style = ColdText.bodyStrong, color = outcomeColour(row.outcome, sky.ink))
                        if (row.checksText != null) Text(row.checksText, style = ColdText.caption, color = sky.dim)
                    }
                }
            }
        }
    }
}

/** Solved gets the sun; everything else stays plain. Rose is reserved for the give-up ring. */
private fun outcomeColour(outcome: WakeOutcome?, ink: Color): Color =
    if (outcome == WakeOutcome.SOLVED) Sun.Glow else ink
