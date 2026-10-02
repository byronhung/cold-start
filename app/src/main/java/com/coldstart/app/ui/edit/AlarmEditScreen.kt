package com.coldstart.app.ui.edit

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.style.TextOverflow
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coldstart.app.alarm.Weekdays
import com.coldstart.app.alarm.formatTime
import com.coldstart.app.puzzle.Preset
import com.coldstart.app.puzzle.WakeMethod
import com.coldstart.app.ui.components.AmberButton
import com.coldstart.app.ui.components.DayPill
import com.coldstart.app.ui.components.GlassCard
import com.coldstart.app.ui.components.SectionLabel
import com.coldstart.app.ui.components.Segmented
import com.coldstart.app.ui.components.SkyBackground
import com.coldstart.app.ui.components.SpringToggle
import com.coldstart.app.ui.components.WheelColumn
import com.coldstart.app.ui.components.springClick
import com.coldstart.app.ui.theme.ColdShapes
import com.coldstart.app.ui.theme.ColdText
import com.coldstart.app.ui.theme.LocalSky
import com.coldstart.app.ui.theme.Skies
import com.coldstart.app.ui.theme.Space
import java.time.DayOfWeek
import java.time.LocalTime

@Composable
fun AlarmEditScreen(viewModel: AlarmEditViewModel, onScanForCode: () -> Unit, onDone: () -> Unit) {
    val wakeCode by viewModel.wakeCode.collectAsStateWithLifecycle()
    SkyBackground(Skies.forHour(LocalTime.now().hour)) {
        val draft = viewModel.draft ?: return@SkyBackground
        AlarmEditContent(
            draft = draft,
            hasWakeCode = wakeCode != null,
            onMethod = { if (viewModel.pickMethod(it)) onScanForCode() },
            onCancel = onDone,
            onSave = { viewModel.save(onDone) },
            onTime = viewModel::setTime,
            onToggleDay = viewModel::toggleDay,
            onChecks = viewModel::setWakeChecks,
            onDifficulty = viewModel::setDifficulty,
            onSound = viewModel::setSound,
            onLabel = viewModel::setLabel,
            onDelete = { viewModel.delete(onDone) },
        )
    }
}

private val DAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")
private val DAY_NAMES = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

@Composable
private fun AlarmEditContent(
    draft: EditDraft,
    hasWakeCode: Boolean,
    onMethod: (WakeMethod) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onTime: (Int, Int) -> Unit,
    onToggleDay: (Int) -> Unit,
    onChecks: (Int) -> Unit,
    onDifficulty: (Preset) -> Unit,
    onSound: (String?) -> Unit,
    onLabel: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val sky = LocalSky.current
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding(),
    ) {
        Row(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Cancel",
                style = ColdText.prompt,
                color = sky.dim,
                modifier = Modifier
                    .springClick(0.92f, onClick = onCancel)
                    .padding(vertical = 10.dp, horizontal = 4.dp),
            )
            Text(
                if (draft.isNew) "New alarm" else "Edit alarm",
                style = ColdText.header,
                color = sky.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            AmberButton("Save", onSave, height = 40.dp)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.screen, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TimeWheels(draft.hour, draft.minute, is24Hour, onTime)
            Text(
                "Rings at ${formatTime(draft.hour, draft.minute, is24Hour)} ${repeatPhrase(draft.repeatDays)}",
                style = ColdText.caption,
                color = sky.dim,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-4).dp),
            )

            Section {
                SectionLabel("Repeat")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    DayOfWeek.entries.forEachIndexed { i, day ->
                        DayPill(DAY_LETTERS[i], DAY_NAMES[i], Weekdays.has(draft.repeatDays, day)) { onToggleDay(i) }
                    }
                }
            }

            val method = WakeMethod.of(draft.wakeMethod)
            Section {
                SectionLabel("How to wake up")
                Segmented(listOf("Puzzles", "Scan", "Both"), WakeMethod.entries.indexOf(method), onSelect = { onMethod(WakeMethod.entries[it]) })
                Text(methodLine(method, hasWakeCode), style = ColdText.caption, color = sky.dim)
            }

            // Difficulty only means something when there are puzzles.
            if (method != WakeMethod.SCAN) {
                Section {
                    SectionLabel("Difficulty")
                    val preset = Preset.of(draft.difficulty)
                    Segmented(listOf("Gentle", "Normal", "Hard"), Preset.entries.indexOf(preset), onSelect = { onDifficulty(Preset.entries[it]) })
                    Text(difficultyLine(preset), style = ColdText.caption, color = sky.dim)
                }
            }

            Section {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                    SectionLabel("Wake checks", Modifier.weight(1f))
                    Text("instead of snooze", style = ColdText.caption.copy(fontSize = ColdText.label.fontSize), color = sky.mute)
                }
                Segmented(listOf("Off", "1", "2", "3"), draft.wakeChecks, onChecks)
                Text(checksLine(draft.wakeChecks), style = ColdText.caption, color = sky.dim)
            }

            SoundRow(draft.soundUri, onSound)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel("Label", Modifier.padding(start = 4.dp))
                BasicTextField(
                    value = draft.label,
                    onValueChange = onLabel,
                    singleLine = true,
                    textStyle = ColdText.body.copy(color = sky.ink),
                    cursorBrush = SolidColor(sky.sunInk),
                    decorationBox = { inner ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(ColdShapes.button)
                                .background(sky.card)
                                .border(1.dp, sky.cardEdge, ColdShapes.button)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (draft.label.isEmpty()) Text("Weekdays, Gym…", style = ColdText.body, color = sky.mute)
                            inner()
                        }
                    },
                )
            }

            if (!draft.isNew) {
                Text(
                    "Delete alarm",
                    style = ColdText.prompt,
                    color = sky.mute,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .springClick(0.92f, onClick = onDelete)
                        .padding(14.dp),
                )
            }
        }
    }
}

@Composable
private fun Section(content: @Composable () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
}

/** Hour, minute and AM/PM wheels over one highlighted band. 24-hour phones get two wheels. */
@Composable
private fun TimeWheels(hour: Int, minute: Int, is24Hour: Boolean, onTime: (Int, Int) -> Unit) {
    val sky = LocalSky.current
    var h by remember { mutableIntStateOf(if (is24Hour) hour else (hour + 11) % 12) }
    var m by remember { mutableIntStateOf(minute) }
    var pm by remember { mutableIntStateOf(if (hour >= 12) 1 else 0) }
    fun report() {
        val h24 = if (is24Hour) h else ((h + 1) % 12) + 12 * pm
        onTime(h24, m)
    }

    Box(Modifier.padding(horizontal = 24.dp)) {
        Box(
            Modifier
                .padding(top = 88.dp)
                .fillMaxWidth()
                .height(44.dp)
                .clip(ColdShapes.button)
                .background(sky.card)
                .border(1.dp, sky.cardEdge, ColdShapes.button),
        )
        Row(Modifier.fillMaxWidth()) {
            val hours = if (is24Hour) (0..23).map { "%02d".format(it) } else (1..12).map { it.toString() }
            WheelColumn(hours, h, { h = it; report() }, "Hour", Modifier.weight(1f))
            WheelColumn((0..59).map { "%02d".format(it) }, m, { m = it; report() }, "Minute", Modifier.weight(1f))
            if (!is24Hour) WheelColumn(listOf("AM", "PM"), pm, { pm = it; report() }, "AM or PM", Modifier.weight(1f))
        }
    }
}

private fun repeatPhrase(days: Int): String = when (days) {
    Weekdays.NONE -> "once"
    Weekdays.WEEKDAYS -> "on weekdays"
    Weekdays.WEEKEND -> "on weekends"
    0b1111111 -> "every day"
    else -> "on " + DayOfWeek.entries.filter { Weekdays.has(days, it) }
        .joinToString(", ") { DAY_NAMES[it.value - 1].take(3) }
}

private fun methodLine(method: WakeMethod, hasCode: Boolean): String = when (method) {
    WakeMethod.PUZZLES -> "Solve the puzzles to stop it."
    WakeMethod.SCAN ->
        if (hasCode) "No puzzles. Walk to your wake-up code and scan it." else "No puzzles: scan your wake-up code. You'll register it next."
    WakeMethod.PUZZLES_AND_SCAN ->
        if (hasCode) "The puzzles, then a last round: walk to your code and scan it." else "Puzzles, then a scan. You'll register your code next."
}

private fun difficultyLine(preset: Preset): String = when (preset) {
    Preset.GENTLE -> "3 rounds of the easiest puzzles. For mornings you wake up fine."
    Preset.NORMAL -> "5 rounds. Starts hard and eases off if you've been slow lately."
    Preset.HARD -> "7 rounds at the hardest level, every time."
}

private fun checksLine(n: Int): String = when (n) {
    0 -> "Off. Once you solve the puzzles, the alarm is done for the morning."
    1 -> "5 min after you solve, one “Still awake?” check. If your phone's in use it passes by itself. Miss it and the alarm rings again, from round 1."
    else -> "Starting 5 min after you solve: $n “Still awake?” checks, 5 min apart. Any you miss rings the alarm again, from round 1."
}

/**
 * The alarm's sound. Opens Android's own picker: every built-in tone, plus files on the phone.
 * Picking "Default" stores null, so the alarm follows the phone's default alarm sound.
 */
@Composable
private fun SoundRow(soundUri: String?, onSound: (String?) -> Unit) {
    val sky = LocalSky.current
    val context = LocalContext.current
    val title = remember(soundUri) {
        if (soundUri == null) {
            "Phone's default alarm sound"
        } else {
            runCatching { RingtoneManager.getRingtone(context, Uri.parse(soundUri))?.getTitle(context) }
                .getOrNull() ?: "Custom sound"
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val picked: Uri? = if (Build.VERSION.SDK_INT >= 33) {
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        val isDefault = picked == null || picked == Settings.System.DEFAULT_ALARM_ALERT_URI
        onSound(if (isDefault) null else picked.toString())
    }

    GlassCard(
        Modifier
            .fillMaxWidth()
            .springClick(0.97f) {
                picker.launch(
                    Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Alarm sound")
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_ALARM_ALERT_URI)
                        .putExtra(
                            RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                            soundUri?.let { Uri.parse(it) } ?: Settings.System.DEFAULT_ALARM_ALERT_URI,
                        ),
                )
            },
    ) {
        Row(
            Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionLabel("Sound")
                Text(title, style = ColdText.bodyStrong, color = sky.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("Change", style = ColdText.caption, color = sky.sunInk)
        }
    }
}
