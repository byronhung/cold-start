package com.coldstart.app.ui.list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coldstart.app.alarm.NextAlarm
import com.coldstart.app.alarm.Weekdays
import com.coldstart.app.puzzle.Preset
import com.coldstart.app.puzzle.WakeMethod
import com.coldstart.app.ui.components.Chip
import com.coldstart.app.ui.components.GlassCard
import com.coldstart.app.ui.components.IconSquareButton
import com.coldstart.app.ui.components.LogoMark
import com.coldstart.app.ui.components.SectionLabel
import com.coldstart.app.ui.components.SkyBackground
import com.coldstart.app.ui.components.SpringToggle
import com.coldstart.app.ui.components.SunFab
import com.coldstart.app.ui.components.SunIcons
import com.coldstart.app.ui.components.springClick
import com.coldstart.app.ui.theme.ColdStartTheme
import com.coldstart.app.ui.theme.ColdText
import com.coldstart.app.ui.theme.LocalSky
import com.coldstart.app.ui.theme.Motion
import com.coldstart.app.ui.theme.Skies
import com.coldstart.app.ui.theme.Space
import java.time.DayOfWeek
import java.time.LocalTime

@Composable
fun AlarmListScreen(
    viewModel: AlarmListViewModel,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    AlarmListContent(ui, onAdd, onEdit, viewModel::setEnabled, onHistory, onSettings)
}

@Composable
private fun AlarmListContent(
    ui: AlarmListUi?,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onToggle: (Long, Boolean) -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
) {
    SkyBackground(Skies.forHour(ui?.hour ?: LocalTime.now().hour)) {
        val sky = LocalSky.current
        LazyColumn(
            modifier = Modifier.safeDrawingPadding(),
            contentPadding = PaddingValues(start = Space.screen, end = Space.screen, top = 28.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LogoMark(sky.ink)
                    Text("Cold Start", style = ColdText.header, color = sky.ink, modifier = Modifier.weight(1f))
                    IconSquareButton(SunIcons.History, "History", onHistory)
                    IconSquareButton(SunIcons.Sliders, "Settings", onSettings)
                }
            }
            item { Hero(ui) }
            item { SetupCard() }
            itemsIndexed(ui?.rows.orEmpty(), key = { _, row -> row.id }) { i, row ->
                AlarmCard(row, index = i, onClick = { onEdit(row.id) }, onToggle = { onToggle(row.id, it) })
            }
            if (ui != null && ui.rows.isEmpty()) {
                item {
                    Text(
                        "No alarms yet. Tap + to add one.",
                        style = ColdText.body,
                        color = sky.dim,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
        SunFab(
            onClick = onAdd,
            label = "Add alarm",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .safeDrawingPadding()
                .padding(end = 24.dp, bottom = 30.dp),
        )
    }
}

@Composable
private fun Hero(ui: AlarmListUi?) {
    val sky = LocalSky.current
    Column(
        Modifier.padding(start = 12.dp, end = 12.dp, top = 28.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SectionLabel("Next alarm")
        when {
            ui == null -> Spacer(Modifier.padding(bottom = 104.dp))
            ui.next != null -> NextClock(ui.next)
            else -> {
                Text("All quiet", style = ColdText.display.copy(fontSize = 56.sp), color = sky.ink)
                Text("No alarms on. Sleep in.", style = ColdText.body, color = sky.dim)
            }
        }
    }
}

@Composable
private fun NextClock(next: NextAlarm) {
    val sky = LocalSky.current
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(next.digits, style = ColdText.heroClock, color = sky.ink)
        if (next.period != null) {
            Text(next.period, style = ColdText.period.copy(fontSize = 22.sp), color = sky.dim, modifier = Modifier.padding(bottom = 12.dp))
        }
    }
    Text(next.line, style = ColdText.body, color = sky.dim)
}

private val DAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")

/** Glass card: rises in on first show, staggered; fades when switched off. */
@Composable
private fun AlarmCard(row: AlarmRowUi, index: Int, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val sky = LocalSky.current
    val rise = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 90L)
        rise.animateTo(1f, Motion.settle())
    }
    val dim by animateFloatAsState(if (row.enabled) 1f else 0.55f, tween(350), label = "off")

    GlassCard(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = rise.value.coerceIn(0f, 1f) * dim
                translationY = (1f - rise.value) * 16.dp.toPx()
            },
    ) {
        Row(
            Modifier.padding(start = 20.dp, end = 18.dp, top = 18.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .springClick(0.985f, onClick = onClick),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(row.time, style = ColdText.alarmTime, color = sky.ink)
                    if (row.period != null) {
                        Text(row.period, style = ColdText.period, color = sky.dim, modifier = Modifier.padding(start = 6.dp, bottom = 3.dp))
                    }
                    if (row.label.isNotBlank()) {
                        Text(
                            row.label,
                            style = ColdText.caption,
                            color = sky.dim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 12.dp, bottom = 3.dp),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        buildAnnotatedString {
                            if (row.repeatDays == Weekdays.NONE) {
                                append("ONCE")
                            } else {
                                DayOfWeek.entries.forEachIndexed { i, day ->
                                    withStyle(SpanStyle(color = if (Weekdays.has(row.repeatDays, day)) sky.sunInk else sky.mute)) {
                                        append(DAY_LETTERS[i])
                                    }
                                    if (i < 6) append("  ")
                                }
                            }
                        },
                        style = ColdText.day,
                        color = sky.mute,
                    )
                    if (row.wakeChecks > 0) Chip(if (row.wakeChecks == 1) "1 check" else "${row.wakeChecks} checks", SunIcons.Check)
                    when (WakeMethod.of(row.wakeMethod)) {
                        WakeMethod.SCAN -> Chip("Scan only", SunIcons.Scan)
                        WakeMethod.PUZZLES_AND_SCAN -> Chip("Scan", SunIcons.Scan)
                        WakeMethod.PUZZLES -> Unit
                    }
                    if (WakeMethod.of(row.wakeMethod) != WakeMethod.SCAN) when (Preset.of(row.difficulty)) {
                        Preset.GENTLE -> Chip("Gentle")
                        Preset.HARD -> Chip("Hard")
                        Preset.NORMAL -> Unit
                    }
                }
            }
            SpringToggle(row.enabled, onToggle, label = "${row.time} ${row.period.orEmpty()} on")
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun AlarmListPreview() = ColdStartTheme {
    AlarmListContent(
        ui = AlarmListUi(
            rows = listOf(
                AlarmRowUi(1, "6:30", "AM", Weekdays.WEEKDAYS, "Weekdays", true, 1, 2),
                AlarmRowUi(2, "8:15", "AM", Weekdays.WEEKEND, "Weekend", true, 0, 1),
                AlarmRowUi(3, "5:45", "AM", 0b1010, "Gym", false, 1, 0),
            ),
            next = NextAlarm("6:30", "AM", "tomorrow · in 8 h 30 m"),
            hour = 22,
        ),
        onAdd = {}, onEdit = {}, onToggle = { _, _ -> }, onHistory = {}, onSettings = {},
    )
}
