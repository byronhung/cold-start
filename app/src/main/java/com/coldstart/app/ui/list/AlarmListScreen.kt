package com.coldstart.app.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coldstart.app.alarm.Weekdays
import com.coldstart.app.ui.components.RepeatLine
import com.coldstart.app.ui.components.coldSwitchColors
import com.coldstart.app.ui.theme.ColdColors
import com.coldstart.app.ui.theme.ColdShapes
import com.coldstart.app.ui.theme.ColdStartTheme
import com.coldstart.app.ui.theme.ColdText
import com.coldstart.app.ui.theme.Space

@Composable
fun AlarmListScreen(
    viewModel: AlarmListViewModel,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    AlarmListContent(ui, onAdd, onEdit, viewModel::setEnabled)
}

@Composable
private fun AlarmListContent(
    ui: AlarmListUi?,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onToggle: (Long, Boolean) -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(ColdColors.Ground)
            .safeDrawingPadding(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = Space.screen,
                end = Space.screen,
                top = Space.xl,
                bottom = 104.dp, // room for the add button
            ),
        ) {
            item {
                Column {
                    Text("Alarms", style = ColdText.title, color = ColdColors.Ink)
                    Spacer(Modifier.height(Space.xs))
                    Text(
                        text = when {
                            ui == null -> ""
                            ui.nextSummary != null -> ui.nextSummary
                            else -> "No alarms on"
                        },
                        style = ColdText.caption,
                        color = ColdColors.InkMute,
                    )
                    Spacer(Modifier.height(Space.lg))
                }
            }

            if (ui != null && ui.rows.isEmpty()) {
                item {
                    Text(
                        "No alarms yet. Tap + to add one.",
                        style = ColdText.body,
                        color = ColdColors.InkDim,
                        modifier = Modifier.padding(top = Space.xl),
                    )
                }
            }

            items(ui?.rows.orEmpty(), key = { it.id }) { row ->
                HorizontalDivider(color = ColdColors.Line)
                AlarmRow(row, onClick = { onEdit(row.id) }, onToggle = { onToggle(row.id, it) })
            }

            if (!ui?.rows.isNullOrEmpty()) {
                item { HorizontalDivider(color = ColdColors.Line) }
            }
        }

        FloatingActionButton(
            onClick = onAdd,
            containerColor = ColdColors.Accent,
            contentColor = ColdColors.Ground,
            shape = ColdShapes.medium,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Space.screen),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add alarm")
        }
    }
}

@Composable
private fun AlarmRow(row: AlarmRowUi, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                row.time,
                style = ColdText.alarmTime,
                color = if (row.enabled) ColdColors.Ink else ColdColors.InkMute,
            )
            RepeatLine(row.repeatDays, row.label)
        }
        Switch(checked = row.enabled, onCheckedChange = onToggle, colors = coldSwitchColors())
    }
}

@Preview(widthDp = 390, heightDp = 700)
@Composable
private fun AlarmListPreview() = ColdStartTheme {
    AlarmListContent(
        ui = AlarmListUi(
            rows = listOf(
                AlarmRowUi(1, "06:30", Weekdays.WEEKDAYS, "", true),
                AlarmRowUi(2, "08:15", Weekdays.WEEKEND, "", true),
                AlarmRowUi(3, "05:45", Weekdays.NONE, "Gym", false),
            ),
            nextSummary = "Next: tomorrow at 06:30 · in 7 h 49 m",
        ),
        onAdd = {}, onEdit = {}, onToggle = { _, _ -> },
    )
}
