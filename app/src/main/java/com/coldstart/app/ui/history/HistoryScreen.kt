package com.coldstart.app.ui.history

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.coldstart.app.ui.components.springClick
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate
import java.time.YearMonth
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
import com.coldstart.app.ui.theme.LocalSkyTheme
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

/** One month of the calendar: its name, blanks before the 1st, and each day's mark (null = no alarm). */
data class MonthUi(
    val month: YearMonth,
    val title: String,
    val blanks: Int,
    val days: List<DayMark?>,
    val today: Int?,
    val hasNext: Boolean,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(repository: AlarmRepository, private val is24Hour: Boolean) : ViewModel() {
    private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
    private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val zone = ZoneId.systemDefault()

    val ui: StateFlow<HistoryUi?> = repository.recentWakes.map { wakes ->
        HistoryUi(rows = wakes.map(::row), summary = summary(wakes))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val shown = MutableStateFlow(YearMonth.now())

    val month: StateFlow<MonthUi?> = shown.flatMapLatest { m ->
        repository.wakesBetween(monthRange(m, zone)).map { wakes ->
            val marks = dayMarks(wakes, zone)
            val now = YearMonth.now()
            MonthUi(
                month = m,
                title = monthFormat.format(m),
                blanks = leadingBlanks(m),
                days = (1..m.lengthOfMonth()).map { marks[m.atDay(it)] },
                today = if (m == now) LocalDate.now().dayOfMonth else null,
                hasNext = m < now,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun previousMonth() {
        shown.value = shown.value.minusMonths(1)
    }

    fun nextMonth() {
        if (shown.value < YearMonth.now()) shown.value = shown.value.plusMonths(1)
    }

    /** When you were actually up: the clock time the ring ended, not how long the puzzles took. */
    private fun row(w: WakeLog): WakeRowUi {
        val fired = Instant.ofEpochMilli(w.firedAt).atZone(zone)
        val ended = w.endedAt?.let { Instant.ofEpochMilli(it).atZone(zone) }
        val at = ended?.let { formatTime(it.hour, it.minute, is24Hour) }
        val outcome = when (w.outcome) {
            WakeOutcome.SOLVED -> "Up at $at"
            WakeOutcome.NO_SCAN -> "Up at $at · without the scan"
            WakeOutcome.GAVE_UP -> "Gave up at $at"
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
    val month by viewModel.month.collectAsStateWithLifecycle()
    SkyBackground(LocalSkyTheme.current.night, calm = true) {
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
            month?.let { m ->
                item(key = "calendar") { MonthCard(m, viewModel::previousMonth, viewModel::nextMonth) }
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

/** The month as dots: amber for up, half amber for rang again, an empty ring for gave up. */
@Composable
private fun MonthCard(m: MonthUi, onPrevious: () -> Unit, onNext: () -> Unit) {
    val sky = LocalSky.current
    GlassCard(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(m.title, Modifier.weight(1f).padding(start = 4.dp))
                MonthArrow(SunIcons.Back, "Previous month", enabled = true, onPrevious)
                MonthArrow(SunIcons.Back, "Next month", enabled = m.hasNext, onNext, flip = true)
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                    Text(it, style = ColdText.chip.copy(fontSize = 11.sp), color = sky.mute, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            val cells: List<Int?> = List(m.blanks) { null } + (1..m.days.size)
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    for (i in 0 until 7) {
                        val day = week.getOrNull(i)
                        Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (day != null) DayDot(day, m.days[day - 1], future = m.today != null && day > m.today, today = day == m.today)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LegendDot(DayMark.UP, "Up")
                LegendDot(DayMark.RANG_AGAIN, "Rang again")
                LegendDot(DayMark.GAVE_UP, "Gave up")
            }
        }
    }
}

@Composable
private fun MonthArrow(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit, flip: Boolean = false) {
    val sky = LocalSky.current
    Box(
        Modifier
            .springClick(0.9f, enabled = enabled, onClick = onClick)
            .size(40.dp)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, contentDescription = null,
            tint = sky.ink.copy(alpha = if (enabled) 0.85f else 0.25f),
            modifier = Modifier.size(18.dp).graphicsLayer { scaleX = if (flip) -1f else 1f },
        )
    }
}

@Composable
private fun DayDot(day: Int, mark: DayMark?, future: Boolean, today: Boolean) {
    val sky = LocalSky.current
    val amber = Sun.ToggleLight
    Box(
        Modifier
            .fillMaxSize(0.82f)
            .clip(CircleShape)
            .drawBehind {
                when (mark) {
                    DayMark.UP -> drawCircle(amber)
                    DayMark.RANG_AGAIN -> drawArc(amber, startAngle = 90f, sweepAngle = 180f, useCenter = true)
                    else -> Unit
                }
            }
            .border(
                width = if (today) 2.dp else 1.dp,
                color = when {
                    today -> amber
                    mark == DayMark.UP -> Color.Transparent
                    mark != null -> sky.ink.copy(alpha = 0.45f)
                    else -> Color.Transparent
                },
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$day",
            style = ColdText.chip.copy(fontSize = 11.sp),
            color = when {
                mark == DayMark.UP -> Sun.OnAmber
                future -> sky.ink.copy(alpha = 0.3f)
                mark == null -> sky.ink.copy(alpha = 0.55f)
                else -> sky.ink
            },
        )
    }
}

@Composable
private fun LegendDot(mark: DayMark, text: String) {
    val sky = LocalSky.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(11.dp)
                .clip(CircleShape)
                .drawBehind {
                    when (mark) {
                        DayMark.UP -> drawCircle(Sun.ToggleLight)
                        DayMark.RANG_AGAIN -> drawArc(Sun.ToggleLight, 90f, 180f, useCenter = true)
                        DayMark.GAVE_UP -> Unit
                    }
                }
                .border(1.dp, if (mark == DayMark.UP) Color.Transparent else sky.ink.copy(alpha = 0.45f), CircleShape),
        )
        Text(text, style = ColdText.caption.copy(fontSize = 12.sp), color = sky.dim)
    }
}

/** Solved gets the sun; everything else stays plain. Rose is reserved for the give-up ring. */
private fun outcomeColour(outcome: WakeOutcome?, ink: Color): Color =
    if (outcome == WakeOutcome.SOLVED) Sun.Glow else ink
