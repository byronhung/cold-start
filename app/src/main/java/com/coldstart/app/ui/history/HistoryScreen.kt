package com.coldstart.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.coldstart.app.ui.theme.ColdColors
import com.coldstart.app.ui.theme.ColdText
import com.coldstart.app.ui.theme.Space
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class WakeRowUi(val id: Long, val whenText: String, val outcomeText: String, val outcome: WakeOutcome?)

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
            WakeOutcome.GAVE_UP -> "Gave up after $took"
            WakeOutcome.TIMED_OUT -> "Nobody answered for an hour"
            null -> "Interrupted"
        }
        return WakeRowUi(w.id, "${dayFormat.format(fired)} · ${formatTime(fired.hour, fired.minute, is24Hour)}", outcome, w.outcome)
    }

    private fun summary(wakes: List<WakeLog>): String {
        if (wakes.isEmpty()) return "Nothing yet. Your first morning shows up here."
        val solved = wakes.count { it.outcome == WakeOutcome.SOLVED }
        val gaveUp = wakes.count { it.outcome == WakeOutcome.GAVE_UP }
        return "Last ${wakes.size}: $solved solved · $gaveUp gave up"
    }
}

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColdColors.Ground)
            .safeDrawingPadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Space.screen - Space.md, vertical = Space.lg),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ColdColors.Ink)
            }
            Column {
                Text("History", style = ColdText.title, color = ColdColors.Ink)
                Text(ui?.summary.orEmpty(), style = ColdText.caption, color = ColdColors.InkMute)
            }
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = Space.screen)) {
            items(ui?.rows.orEmpty(), key = { it.id }) { row ->
                HorizontalDivider(color = ColdColors.Line)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Space.md),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(row.whenText.uppercase(), style = ColdText.label, color = ColdColors.InkMute)
                    Text(row.outcomeText, style = ColdText.body, color = outcomeColour(row.outcome))
                }
            }
        }
    }
}

/** Solved is the only outcome that earns the accent. Nothing here is red: red is the give-up bar. */
private fun outcomeColour(outcome: WakeOutcome?): Color =
    if (outcome == WakeOutcome.SOLVED) ColdColors.Accent else ColdColors.InkDim

